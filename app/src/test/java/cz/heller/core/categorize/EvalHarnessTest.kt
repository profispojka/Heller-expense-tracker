package cz.heller.core.categorize

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Vyhodnocení kategorizace na sadě pohybů (`src/test/resources/categorize/eval.tsv`).
 *
 * Simuluje reálný běh: pohyby jdou chronologicky, po každém se „uživatel" podívá a zařazení
 * potvrdí (očekávaná kategorie jde do modelu jako potvrzený vzorek). Měří se:
 * - **celková přesnost** (kolik pohybů dostalo správnou kategorii automaticky),
 * - **přesnost na prvně viděných** obchodnících / protiúčtech — to je ta bolest, kterou
 *   paměť identity nevyřeší a musí ji zvládnout slovník nebo Bayes,
 * - **chybovost**: pohyby zařazené špatně (horší než nezařazené — uživatel si jich nevšimne).
 *
 * Formát TSV (tab): payee, note, txType, counterAccount, vs, amountMinor, income (0/1), expected.
 * Řádky začínající `#` se přeskočí. Sem patří anonymizovaný export reálných dat — čím věrnější,
 * tím užitečnější čísla. Prahy dole jsou záměrně pod aktuálním výsledkem; při ladění slovníku
 * je zvedej, ať regrese nepropadne.
 */
class EvalHarnessTest {

    data class Row(val tx: TxFeatures, val expected: String, val line: Int)

    private fun load(): List<Row> {
        val stream = checkNotNull(javaClass.classLoader?.getResourceAsStream("categorize/eval.tsv")) { "eval.tsv chybí" }
        return stream.bufferedReader().readLines().mapIndexedNotNull { i, line ->
            if (line.isBlank() || line.startsWith("#")) return@mapIndexedNotNull null
            val c = line.split('\t')
            require(c.size == 8) { "řádek ${i + 1}: očekávám 8 sloupců, mám ${c.size}" }
            Row(
                TxFeatures(
                    payee = c[0].ifBlank { null },
                    note = c[1].ifBlank { null },
                    txType = c[2].ifBlank { null },
                    counterAccount = c[3].ifBlank { null },
                    variableSymbol = c[4].ifBlank { null },
                    amountMinor = c[5].toLong(),
                    isIncome = c[6] == "1",
                ),
                expected = c[7],
                line = i + 1,
            )
        }
    }

    @Test
    fun `evaluate categorization on the sample dataset`() {
        TestPoi.index // nainstaluje index provozoven, pokud je k dispozici
        val rows = load()
        val incomeCats = rows.map { it.expected }.filter { it.startsWith("income_") }.toSet()
        val seen = HashSet<String>()
        val confirmed = ArrayList<Pair<TxFeatures, String>>()

        var total = 0; var correct = 0; var wrong = 0; var none = 0
        var firstTotal = 0; var firstCorrect = 0
        val misses = ArrayList<String>()

        for (row in rows) {
            val model = CategoryModel.Builder(incomeCategories = incomeCats).also { b ->
                confirmed.forEach { (tx, c) -> b.add(tx, c) }
            }.build()
            val keys = CategoryModel.identityKeys(row.tx)
            val first = keys.none { it in seen }
            seen += keys

            val res = Categorizer.categorize(row.tx, "", model)
            val got = (res as? Categorizer.Result.Category)?.id
            total++
            if (first) firstTotal++
            when {
                got == row.expected -> { correct++; if (first) firstCorrect++ }
                got == null -> { none++; misses += "  ř.${row.line} nezařazeno: ${row.tx.payee} → čekal ${row.expected}; tipy ${(res as Categorizer.Result.Uncertain).suggestions}" }
                else -> { wrong++; misses += "  ř.${row.line} ŠPATNĚ: ${row.tx.payee} → $got, čekal ${row.expected} (${(res as Categorizer.Result.Category).source})" }
            }
            confirmed += row.tx to row.expected
        }

        val acc = correct.toDouble() / total
        val firstAcc = firstCorrect.toDouble() / firstTotal
        println("=== Kategorizace: $correct/$total správně (${"%.1f".format(acc * 100)} %), " +
            "$wrong špatně, $none nezařazeno; prvně viděné: $firstCorrect/$firstTotal (${"%.1f".format(firstAcc * 100)} %) ===")
        misses.forEach(::println)

        assertTrue("celková přesnost $acc < 0.9", acc >= 0.9)
        assertTrue("přesnost na prvně viděných $firstAcc < 0.85", firstAcc >= 0.85)
        assertTrue("špatně zařazených $wrong > 2", wrong <= 2)
    }
}
