package cz.heller.data.repo

import android.content.Context
import cz.heller.R
import cz.heller.core.categorize.Categorizer
import cz.heller.core.categorize.MerchantText
import cz.heller.core.categorize.TxFeatures
import cz.heller.core.fio.FioApiClient
import cz.heller.core.fio.FioFetchResult
import cz.heller.core.fio.FioParser
import cz.heller.core.fio.FioTx
import cz.heller.data.db.AccountDao
import cz.heller.data.db.RecordDao
import cz.heller.data.db.RecordEntity
import cz.heller.data.db.RecordSource
import cz.heller.data.db.RecordType
import cz.heller.data.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/** Výsledek synchronizace s Fio. */
sealed interface FioSyncResult {
    data class Success(val added: Int, val total: Int, val categorized: Int) : FioSyncResult
    data object RateLimited : FioSyncResult
    data class Error(val message: String) : FioSyncResult
}

/**
 * Import pohybů z Fio do účtu Heller. Deduplikace přes unikátní `fioTransactionId`.
 * Při importu rovnou **kategorizuje** (naučená pravidla + seed) a vlastní převody mezi
 * účty (protistrana = majitel účtu) označí jako TRANSFER (mimo příjmy/výdaje ve statistikách).
 */
@Singleton
class FioRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: FioApiClient,
    private val recordDao: RecordDao,
    private val accountDao: AccountDao,
    private val categorization: CategorizationRepository,
    private val planned: PlannedPaymentRepository,
    private val settings: SettingsRepository,
) {
    private val zone: ZoneId = ZoneId.systemDefault()

    /**
     * Stáhne a naimportuje pohyby za posledních [daysBack] dní do účtu [accountId].
     * Fio bez silné autorizace poskytne jen data ne starší 90 dní — proto výchozí okno 90 dní.
     */
    suspend fun sync(token: String, accountId: String, daysBack: Long = 90): FioSyncResult {
        if (token.isBlank()) return FioSyncResult.Error(context.getString(R.string.fio_error_missing_token))
        val to = LocalDate.now()
        val from = to.minusDays(daysBack)
        return when (val r = client.fetchPeriods(token, from, to)) {
            is FioFetchResult.Success -> importJson(r.json, accountId).also {
                if (it is FioSyncResult.Success) {
                    // Dožeň i starší nezařazené (po vylepšení seed pravidel) a napáruj plánované platby.
                    categorization.recategorizeUncategorized()
                    planned.reconcileAll()
                }
            }
            FioFetchResult.RateLimited -> FioSyncResult.RateLimited
            is FioFetchResult.HttpError -> when (r.code) {
                404 -> FioSyncResult.Error(context.getString(R.string.fio_error_invalid_token))
                422 -> FioSyncResult.Error(context.getString(R.string.fio_error_422))
                else -> FioSyncResult.Error(context.getString(R.string.fio_error_http, r.code))
            }
            is FioFetchResult.NetworkError -> FioSyncResult.Error(context.getString(R.string.fio_error_network, r.message))
        }
    }

    /** Naparsuje JSON, zkategorizuje a vloží nové pohyby; vrátí přidané / celkem / zařazené. */
    suspend fun importJson(json: String, accountId: String): FioSyncResult {
        val txs = runCatching { FioParser.parse(json) }
            .getOrElse { return FioSyncResult.Error(context.getString(R.string.fio_error_parse)) }

        val model = categorization.model()
        val ownerNorm = ownerOf(txs)

        // Číslo tohoto účtu z Fia (pro budoucí detekci převodů mezi vlastními účty) ulož.
        FioParser.accountNumber(json)?.let { settings.setFioAccountNumber(accountId, it) }
        // Čísla OSTATNÍCH připojených Fio účtů — platba na ně = převod mezi vlastními účty (ignoruje se ve statistikách).
        val otherAccounts = settings.fioConnections.first()
            .filter { it.accountId != accountId }
            .mapNotNull { it.fioAccountNumber }
            .map { normAcc(it) }
            .filter { it.isNotEmpty() }
        val isBusiness = accountDao.getById(accountId)?.isBusiness == true

        var added = 0
        var categorized = 0
        for (t in txs) {
            val minor = BigDecimal(t.amount).movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
            val ts = System.currentTimeMillis()

            // U karetních plateb je protiúčet prázdný, v „názvu protiúčtu" je název karty
            // („Fio Visa Classic payWave CZK") a obchodník je v textu („Nákup: …").
            val rawDesc = t.message ?: t.userIdentification ?: t.comment
            val isCard = t.type?.contains("karetn", ignoreCase = true) == true || looksLikeCardName(t.counterName)
            val payee = if (isCard) {
                merchantFrom(rawDesc) ?: t.counterName
            } else {
                t.counterName ?: merchantFrom(rawDesc) ?: t.counterAccount
            }
            val note = if (rawDesc != null && rawDesc != payee) rawDesc else null

            var recType = if (minor < 0) RecordType.EXPENSE else RecordType.INCOME
            var categoryId: String? = null
            var transferOut: Boolean? = null
            val features = TxFeatures(
                payee = payee,
                note = note,
                isIncome = minor >= 0,
                amountMinor = abs(minor),
                txType = t.type,
                counterAccount = t.counterAccount,
                variableSymbol = t.variableSymbol,
            )
            when (val res = Categorizer.categorize(features, ownerNorm, model)) {
                Categorizer.Result.Transfer -> { recType = RecordType.TRANSFER; transferOut = minor < 0 }
                is Categorizer.Result.Category -> categoryId = res.id
                is Categorizer.Result.Uncertain -> {}
            }

            // Převod mezi vlastními účty (protiúčet = jiný připojený Fio účet) → TRANSFER, mimo statistiky.
            val counter = normAcc(t.counterAccount)
            if (recType != RecordType.TRANSFER && counter.isNotEmpty() && otherAccounts.any { sameAccount(counter, it) }) {
                recType = RecordType.TRANSFER
                transferOut = minor < 0
                categoryId = null
            }

            // Podnikatelský účet: každý příchozí pohyb (co není převod) ber jako příjem „Plat, mzda, fakturace".
            if (isBusiness && recType == RecordType.INCOME) {
                categoryId = "income_wage"
            }

            val rec = RecordEntity(
                id = UUID.randomUUID().toString(),
                type = recType,
                accountId = accountId,
                categoryId = categoryId,
                amountMinor = abs(minor),
                dateTime = t.date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli(),
                payee = payee,
                note = note,
                transferOut = transferOut,
                source = RecordSource.FIO,
                fioTransactionId = t.id,
                counterAccount = t.counterAccount,
                variableSymbol = t.variableSymbol,
                txType = t.type,
                // Automaticky přiřazená kategorie čeká na potvrzení uživatelem (v UI označená „auto").
                categoryAuto = categoryId != null,
                createdAt = ts,
                updatedAt = ts,
            )
            if (recordDao.insertIgnore(rec) != -1L) {
                added++
                if (categoryId != null || recType == RecordType.TRANSFER) categorized++
            } else {
                // Už importovaný pohyb (před v9 bez bankovních signálů) — doplň je, ať se z něj model učí.
                recordDao.backfillFioSignals(accountId, t.id, t.counterAccount, t.variableSymbol, t.type)
            }
        }

        // Sjednoť zobrazený zůstatek se skutečným zůstatkem z banky (Fio `closingBalance`):
        // dopočítej počáteční zůstatek tak, aby `initial + Σ(záznamy) = closingBalance`. Tím sedí
        // i přesto, že importujeme jen 90 dní a počáteční zůstatek byl jen odhad.
        FioParser.closingBalanceMinor(json)?.let { closingMinor ->
            val signed = recordDao.signedSumForAccount(accountId)
            accountDao.setInitialBalance(accountId, closingMinor - signed, System.currentTimeMillis())
        }

        return FioSyncResult.Success(added, txs.size, categorized)
    }

    /** Číslo účtu jen číslice (bez kódu banky za „/"). „2901229792/2010" → „2901229792". */
    private fun normAcc(s: String?): String = s?.substringBefore('/')?.filter { it.isDigit() } ?: ""

    /** Shoda čísel účtů (toleruje předčíslí — porovná shodu nebo koncovku). */
    private fun sameAccount(a: String, b: String): Boolean {
        if (a.isEmpty() || b.isEmpty()) return false
        if (a == b) return true
        return minOf(a.length, b.length) >= 6 && (a.endsWith(b) || b.endsWith(a))
    }

    /** Majitel účtu = nejčastější „Provedl" (normalizovaně) — pro detekci vlastních převodů. */
    private fun ownerOf(txs: List<FioTx>): String =
        txs.mapNotNull { it.performedBy }
            .map { MerchantText.normalize(it) }
            .filter { it.isNotEmpty() }
            .groupingBy { it }.eachCount()
            .maxByOrNull { it.value }?.key ?: ""

    /**
     * Z popisu karetní platby vytáhne obchodníka: „Nákup: ALBERT…, Havířov…" → „ALBERT…".
     * Přeskočí zdvořilostní první segment („Dekujeme, foodora.cz, …" → „foodora.cz").
     */
    private fun merchantFrom(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        var s = raw.trim()
        for (prefix in listOf("Nákup:", "Platba kartou:", "Výběr:", "Kredit:", "Nákup")) {
            if (s.startsWith(prefix)) { s = s.removePrefix(prefix).trim(); break }
        }
        val segments = s.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val first = segments.firstOrNull() ?: return null
        if (MerchantText.normalize(first) in MERCHANT_NOISE) segments.getOrNull(1)?.let { return it }
        return first
    }

    /** Název karty místo protistrany (Fio ho dává u karetních pohybů do „názvu protiúčtu"). */
    private fun looksLikeCardName(name: String?): Boolean {
        val n = MerchantText.normalize(name)
        return n.isNotEmpty() && CARD_WORDS.any { it in n.split(' ') }
    }

    private companion object {
        val MERCHANT_NOISE = setOf("dekujeme", "dakujeme", "dziekujemy", "danke", "thank you", "thanks")
        val CARD_WORDS = setOf("visa", "mastercard", "maestro", "paywave", "debit", "credit")
    }
}
