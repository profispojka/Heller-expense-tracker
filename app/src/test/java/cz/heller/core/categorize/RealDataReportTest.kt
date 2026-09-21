package cz.heller.core.categorize

import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Report pokrytí na **reálných datech bez štítků** (`src/test/resources/categorize/local/real.tsv`,
 * gitignorovaná složka; vytvoří `tools/lexicon/fio-csv-to-eval.mjs`). Nic neassertuje — vypíše,
 * kolik pohybů by engine zařadil ze studeného startu (bez historie uživatele), z jaké vrstvy,
 * a nejčastější nezařazené obchodníky. Řádky s vyplněným `expected` navíc vyhodnotí přesnost.
 * Když soubor chybí, test se přeskočí.
 */
class RealDataReportTest {

    @Test
    fun `coverage report on real data`() {
        TestPoi.index // nainstaluje index provozoven, pokud je k dispozici
        val stream = javaClass.classLoader?.getResourceAsStream("categorize/local/real.tsv")
        assumeTrue("real.tsv chybí — přeskočeno", stream != null)
        val rows = stream!!.bufferedReader().readLines().mapNotNull { line ->
            if (line.isBlank() || line.startsWith("#")) return@mapNotNull null
            val c = line.split('\t')
            TxFeatures(
                payee = c[0].ifBlank { null }, note = c[1].ifBlank { null }, txType = c[2].ifBlank { null },
                counterAccount = c[3].ifBlank { null }, variableSymbol = c[4].ifBlank { null },
                amountMinor = c[5].toLong(), isIncome = c[6] == "1",
            ) to c.getOrNull(7)?.takeIf { it.isNotBlank() && it != "?" }?.let { cz.heller.data.db.CategoryAliases.toCurrent(it) }
        }

        val model = CategoryModel.EMPTY
        val bySource = HashMap<String, Int>()
        val byCategory = HashMap<String, Int>()
        val uncertain = HashMap<String, Int>()
        val uncertainNoText = HashMap<String, Int>()
        val samples = HashMap<String, MutableList<String>>()
        val categorizedRows = ArrayList<String>()
        var labeled = 0; var labeledOk = 0; var labeledWrong = 0
        val wrongLines = ArrayList<String>()

        for ((tx, expected) in rows) {
            val res = Categorizer.categorize(tx, "", model)
            val label = tx.payee ?: "(bez plátce)"
            when (res) {
                is Categorizer.Result.Category -> {
                    bySource[res.source.name] = (bySource[res.source.name] ?: 0) + 1
                    byCategory[res.id] = (byCategory[res.id] ?: 0) + 1
                    samples.getOrPut("${res.id} (${res.source})") { ArrayList() }.let { if (label !in it) it += label }
                    val noteShort = tx.note?.substringBefore(", dne")?.take(60) ?: ""
                    categorizedRows += "  ${if (tx.isIncome) "+" else "-"}${tx.amountMinor / 100} | $label | $noteShort → ${res.id} (${res.source})"
                    if (expected != null) { labeled++; if (expected == res.id) labeledOk++ else { labeledWrong++; wrongLines += "  $label → ${res.id}, čekal $expected" } }
                }
                is Categorizer.Result.Uncertain -> {
                    val key = MerchantText.key(tx.payee).ifBlank { label }
                    val hasText = tx.payeeTokens.any { MerchantText.isWord(it) } || tx.noteTokens.any { MerchantText.isWord(it) }
                    (if (hasText) uncertain else uncertainNoText).let { it[key] = (it[key] ?: 0) + 1 }
                    if (expected != null) labeled++
                }
                Categorizer.Result.Transfer -> bySource["TRANSFER"] = (bySource["TRANSFER"] ?: 0) + 1
            }
        }

        val total = rows.size
        val categorized = bySource.values.sum()
        println("=== Reálná data: $total pohybů, studený start (bez historie) ===")
        println("zařazeno: $categorized (${"%.1f".format(100.0 * categorized / total)} %)")
        bySource.entries.sortedByDescending { it.value }.forEach { println("  ${it.key}: ${it.value}") }
        val unc = uncertain.values.sum(); val uncNo = uncertainNoText.values.sum()
        println("nezařazeno s textem: $unc (${"%.1f".format(100.0 * unc / total)} %), unikátních obchodníků ${uncertain.size}")
        println("nezařazeno bez textu (převody bez poznámky): $uncNo (${"%.1f".format(100.0 * uncNo / total)} %)")
        if (labeled > 0) println("se štítkem: $labeled, správně $labeledOk, špatně $labeledWrong"); wrongLines.forEach(::println)

        println("--- kategorie (počet) ---")
        byCategory.entries.sortedByDescending { it.value }.forEach { println("  ${it.key}: ${it.value}") }
        println("--- nejčastější nezařazení obchodníci s textem ---")
        uncertain.entries.sortedByDescending { it.value }.take(80).forEach { println("  ${it.value}× ${it.key}") }
        println("--- vzorek zařazení podle kategorie a vrstvy (max 12 obchodníků) ---")
        samples.entries.sortedBy { it.key }.forEach { (k, v) -> println("  $k: ${v.take(12).joinToString(" | ")}${if (v.size > 12) " … (+${v.size - 12})" else ""}") }
        // --- Simulace provozu na ostítkovaných řádcích: chronologicky, po každém pohybu se štítek
        // uživatele stane potvrzeným vzorkem (paměť + Bayes). Nezařazené řádky bez štítku se přeskočí.
        val labeledRows = rows.filter { it.second != null }
        if (labeledRows.isNotEmpty()) {
            val incomeCats = labeledRows.map { it.second!! }.filter { it.startsWith("income_") }.toSet()
            val seen = HashSet<String>()
            val confirmed = ArrayList<Pair<TxFeatures, String>>()
            var total = 0; var correct = 0; var wrong = 0; var none = 0
            var firstTotal = 0; var firstCorrect = 0
            val perSource = HashMap<String, IntArray>() // [ok, wrong]
            val misses = ArrayList<String>()
            for ((tx, expected) in labeledRows) {
                val m = CategoryModel.Builder(incomeCategories = incomeCats).also { b -> confirmed.forEach { (t, c) -> b.add(t, c) } }.build()
                val keys = CategoryModel.identityKeys(tx)
                val first = keys.none { it in seen }
                seen += keys
                val res = Categorizer.categorize(tx, "", m)
                val got = (res as? Categorizer.Result.Category)?.id
                total++; if (first) firstTotal++
                val label = "${tx.payee} | ${tx.note?.substringBefore(", dne")?.take(50) ?: ""}"
                when {
                    got == expected!! -> { correct++; if (first) firstCorrect++; perSource.getOrPut((res as Categorizer.Result.Category).source.name) { IntArray(2) }[0]++ }
                    got == null -> { none++; misses += "  nezařazeno: $label → čekal $expected; tipy ${(res as Categorizer.Result.Uncertain).suggestions}" }
                    else -> { wrong++; perSource.getOrPut((res as Categorizer.Result.Category).source.name) { IntArray(2) }[1]++; misses += "  ŠPATNĚ: $label → $got, čekal $expected (${res.source})" }
                }
                confirmed += tx to expected
            }
            println("=== Simulace provozu na $total ostítkovaných pohybech: $correct správně (${"%.1f".format(100.0 * correct / total)} %), " +
                "$wrong špatně, $none nezařazeno; prvně viděné: $firstCorrect/$firstTotal (${"%.1f".format(100.0 * firstCorrect / firstTotal)} %) ===")
            perSource.entries.sortedBy { it.key }.forEach { println("  ${it.key}: ${it.value[0]} ok, ${it.value[1]} špatně") }
            misses.forEach(::println)
        }

        // Náhodný (deterministický) vzorek řádků pro ruční kontrolu přesnosti — jeden řádek na
        // obchodníka, ať častí obchodníci nezakryjí zbytek.
        println("--- náhodný vzorek 150 zařazených řádků, unikátní obchodníci (seed 42) ---")
        categorizedRows.distinctBy { it.substringAfter("| ").substringBefore(" |").let { p -> MerchantText.key(p) } }
            .shuffled(java.util.Random(42)).take(150).forEach(::println)
    }
}
