package cz.heller.core.recurring

import cz.heller.core.categorize.Categorizer
import cz.heller.core.categorize.CategoryModel
import cz.heller.core.categorize.MerchantText
import cz.heller.core.categorize.TxFeatures
import cz.heller.data.db.RecordEntity
import cz.heller.data.db.RecordType
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Detekce **opakovaných plateb** (trvalé příkazy / inkasa / karetní předplatné) z historie pohybů.
 * Fio token API seznam trvalých příkazů nevystavuje, takže je odvozujeme: stejná částka, stejný
 * obchodník/protistrana a **měsíční kadence** (~1× za měsíc). Časté útraty za stejnou částku (kafe,
 * MHD) se vyřadí podmínkou „počet výskytů ≈ počet měsíců".
 */
object RecurringDetector {

    private val zone: ZoneId = ZoneId.systemDefault()

    /** Minimální částka, aby se za předplatné nepovažovaly drobné (MHD, kafe). */
    private const val MIN_MINOR = 5_000L // 50 Kč

    data class Candidate(
        val key: String,
        val name: String,
        val amountMinor: Long,
        val categoryId: String?,
        val occurrences: Int,
        /** Datum dalšího očekávaného výskytu (epoch day) = poslední výskyt + 1 měsíc. */
        val nextStartEpochDay: Long,
    )

    fun detect(records: List<RecordEntity>, model: CategoryModel): List<Candidate> {
        val expenses = records.filter { it.type == RecordType.EXPENSE && it.amountMinor >= MIN_MINOR }
        val groups = expenses.groupBy {
            (MerchantText.key(it.payee).ifBlank { MerchantText.normalize(it.payee) }) to it.amountMinor
        }

        val out = ArrayList<Candidate>()
        for ((gk, list) in groups) {
            val (key, amount) = gk
            if (key.isBlank()) continue

            val dates = list.map { Instant.ofEpochMilli(it.dateTime).atZone(zone).toLocalDate() }.sorted()
            val months = dates.map { YearMonth.from(it) }.toSet()
            // ≥2 různé měsíce (z 90denního okna se měsíční příkaz často projeví jen 2×).
            if (months.size < 2) continue
            if (list.size > months.size + 1) continue // častá útrata, ne pravidelná platba

            val gaps = dates.zipWithNext { a, b -> ChronoUnit.DAYS.between(a, b) }
            if (gaps.isEmpty()) continue
            val medianGap = gaps.sorted()[gaps.size / 2]
            if (medianGap < 22 || medianGap > 40) continue // ne ~měsíční kadence

            val recent = list.maxByOrNull { it.dateTime }!!
            // Už zařazený záznam má přednost; jinak zkus kategorizátor.
            val features = TxFeatures(
                payee = recent.payee,
                note = recent.note,
                isIncome = false,
                amountMinor = recent.amountMinor,
                txType = recent.txType,
                counterAccount = recent.counterAccount,
                variableSymbol = recent.variableSymbol,
            )
            val categoryId = recent.categoryId ?: when (val r = Categorizer.categorize(features, "", model)) {
                is Categorizer.Result.Category -> r.id
                else -> null
            }
            out += Candidate(
                key = key,
                name = recent.payee ?: key,
                amountMinor = amount,
                categoryId = categoryId,
                occurrences = list.size,
                nextStartEpochDay = dates.last().plusMonths(1).toEpochDay(),
            )
        }
        return out.sortedByDescending { it.amountMinor }
    }
}
