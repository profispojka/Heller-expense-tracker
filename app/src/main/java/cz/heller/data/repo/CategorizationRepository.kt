package cz.heller.data.repo

import android.content.Context
import android.util.Log
import cz.heller.core.categorize.Categorizer
import cz.heller.core.categorize.CategoryModel
import cz.heller.core.categorize.MerchantText
import cz.heller.core.categorize.PoiIndex
import cz.heller.core.categorize.TxFeatures
import dagger.hilt.android.qualifiers.ApplicationContext
import cz.heller.data.db.CategoryAliases
import cz.heller.data.db.CategoryDao
import cz.heller.data.db.RecordDao
import cz.heller.data.db.RecordEntity
import cz.heller.data.db.RecordType
import cz.heller.data.settings.CategoryRulesStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Model kategorizace a dávková aplikace. Model ([CategoryModel]) se staví z **potvrzených**
 * záznamů v DB (paměť identity + naivní Bayes) a drží se v paměti; po každé změně kategorie
 * uživatelem se zneplatní ([invalidate]) a při dalším použití znovu postaví.
 */
@Singleton
class CategorizationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rulesStore: CategoryRulesStore,
    private val recordDao: RecordDao,
    private val categoryDao: CategoryDao,
) {
    private val lock = Mutex()
    @Volatile private var cached: CategoryModel? = null

    /** Model postavený z aktuální historie (cache, dokud někdo nezavolá [invalidate]). */
    suspend fun model(): CategoryModel {
        ensurePoiIndex()
        cached?.let { return it }
        return lock.withLock {
            cached ?: build().also { cached = it }
        }
    }

    /** Index provozoven z assets se načte jednou, při prvním použití kategorizace (~1 s na pozadí). */
    private fun ensurePoiIndex() {
        if (PoiIndex.installed != null) return
        synchronized(PoiIndex.Companion) {
            if (PoiIndex.installed != null) return
            PoiIndex.installed = runCatching { context.assets.open(POI_ASSET).use { PoiIndex.load(it) } }
                .onFailure { Log.w("Categorization", "POI index unavailable", it) }
                .getOrNull()
        }
    }

    /** Zneplatní cache — volat po každé změně kategorie uživatelem nebo kategorií samotných. */
    fun invalidate() {
        cached = null
    }

    private suspend fun build(): CategoryModel {
        val builder = CategoryModel.Builder(
            validCategories = categoryDao.getAllIds().toSet(),
            // Stará pravidla odkazují na sluggy z verzí před 1.4 → převeď na aktuální strom.
            legacyRules = rulesStore.snapshot().mapValues { (_, v) -> CategoryAliases.toCurrent(v) },
            incomeCategories = categoryDao.getIncomeIds().toSet(),
        )
        for (r in recordDao.getConfirmedCategorized()) {
            builder.add(r.toFeatures(), r.categoryId ?: continue)
        }
        return builder.build()
    }

    /** Kategorizace jednoho záznamu (bez detekce vlastního převodu). */
    suspend fun categorize(record: RecordEntity): Categorizer.Result =
        Categorizer.categorize(record.toFeatures(), ownerNorm = "", model = model())

    /** Seřazené tipy kategorií pro záznam (pro chipy v UI). */
    suspend fun suggestionsFor(record: RecordEntity, max: Int = 3): List<String> =
        Categorizer.suggestions(record.toFeatures(), model(), max)

    /** Kolik dalších nezařazených záznamů má stejného obchodníka jako [payee]. */
    suspend fun countUncategorizedForMerchant(payee: String?): Int {
        val key = MerchantText.key(payee)
        if (key.isBlank()) return 0
        return recordDao.getUncategorized().count { MerchantText.key(it.payee) == key }
    }

    /** Zařadí všechny nezařazené záznamy od stejného obchodníka do [categoryId] (jako potvrzené); vrátí počet. */
    suspend fun applyToMerchant(payee: String?, categoryId: String): Int {
        val key = MerchantText.key(payee)
        if (key.isBlank()) return 0
        var n = 0
        val ts = System.currentTimeMillis()
        for (r in recordDao.getUncategorized()) {
            if (MerchantText.key(r.payee) == key) {
                recordDao.setCategory(r.id, categoryId, auto = false, ts = ts)
                n++
            }
        }
        if (n > 0) invalidate()
        return n
    }

    /**
     * Projede nezařazené příjmy/výdaje a zařadí, co jde (jako automatické, tj. nepotvrzené);
     * vrátí počet zařazených. Volá se po Fio syncu, po změně kategorie uživatelem a při otevření.
     */
    suspend fun recategorizeUncategorized(): Int {
        val model = model()
        var n = 0
        val ts = System.currentTimeMillis()
        for (r in recordDao.getUncategorized()) {
            val res = Categorizer.categorize(r.toFeatures(), ownerNorm = "", model = model)
            if (res is Categorizer.Result.Category) {
                recordDao.setCategory(r.id, res.id, auto = true, ts = ts)
                n++
            }
        }
        return n
    }

    companion object {
        private const val POI_ASSET = "poi_index.bin"

        fun RecordEntity.toFeatures(): TxFeatures = TxFeatures(
            payee = payee,
            note = note,
            isIncome = type == RecordType.INCOME,
            amountMinor = amountMinor,
            txType = txType,
            counterAccount = counterAccount,
            variableSymbol = variableSymbol,
        )
    }
}
