package cz.heller.core.categorize

import kotlin.math.exp
import kotlin.math.ln

/**
 * Multinomiální naivní Bayes nad příznaky z [TxFeatures.bayesFeatures], natrénovaný na
 * uživatelem potvrzených záznamech (stejný princip jako importní matcher v GnuCash). Umí
 * zobecnit přes sdílená slova („restaurace", „lekarna", „praha 5") a typ pohybu / řád částky
 * tam, kde slovník ani paměť identity nic nevědí.
 *
 * Příznaky, které se vyskytují ve více než [MAX_DOC_FREQ] dokumentů, se ignorují (šum
 * jako „platba kartou" nebo město uživatele).
 */
class NaiveBayes private constructor(
    private val classDocs: Map<String, Int>,
    private val featureCounts: Map<String, Map<String, Int>>,
    private val classFeatureTotal: Map<String, Int>,
    private val informative: Set<String>,
    private val docs: Int,
) {
    data class Scored(val categoryId: String, val posterior: Double)

    val classes: Set<String> get() = classDocs.keys
    fun samples(categoryId: String): Int = classDocs[categoryId] ?: 0

    /**
     * Jsou v příznacích textová slova, která model zná? Bez nich by rozhodovaly jen priory.
     * Chce aspoň dvě **různá** známá slova (název i poznámka dohromady), aby samotné město
     * v názvu obchodníka („FRATELLI OSTRAVA") nestačilo.
     */
    fun hasTextEvidence(features: Collection<String>): Boolean {
        val words = HashSet<String>()
        for (f in features) {
            if (f !in informative) continue
            if (f.startsWith("p:") || f.startsWith("n:")) words += f.substring(2)
        }
        return words.size >= 2
    }

    /** Třídy seřazené podle posteriorní pravděpodobnosti (součet = 1). */
    fun classify(features: Collection<String>, allowed: (String) -> Boolean = { true }): List<Scored> {
        val known = features.filter { it in informative }
        val vocab = informative.size.coerceAtLeast(1)
        val logs = ArrayList<Pair<String, Double>>()
        for ((c, n) in classDocs) {
            if (!allowed(c)) continue
            var lp = ln(n.toDouble() / docs)
            val counts = featureCounts[c] ?: emptyMap()
            val total = classFeatureTotal[c] ?: 0
            val denom = total + ALPHA * vocab
            for (f in known) lp += ln(((counts[f] ?: 0) + ALPHA) / denom)
            logs += c to lp
        }
        if (logs.isEmpty()) return emptyList()
        val max = logs.maxOf { it.second }
        val exps = logs.map { it.first to exp(it.second - max) }
        val sum = exps.sumOf { it.second }
        return exps.map { Scored(it.first, it.second / sum) }.sortedByDescending { it.posterior }
    }

    class Builder {
        private val classDocs = HashMap<String, Int>()
        private val featureCounts = HashMap<String, HashMap<String, Int>>()
        private val docFreq = HashMap<String, Int>()
        private var docs = 0

        fun add(features: Collection<String>, categoryId: String): Builder {
            val set = features.toSet()
            docs++
            classDocs[categoryId] = (classDocs[categoryId] ?: 0) + 1
            val fc = featureCounts.getOrPut(categoryId) { HashMap() }
            for (f in set) {
                fc[f] = (fc[f] ?: 0) + 1
                docFreq[f] = (docFreq[f] ?: 0) + 1
            }
            return this
        }

        fun build(): NaiveBayes {
            val informative = docFreq.filter { (_, df) -> docs < 5 || df <= docs * MAX_DOC_FREQ }.keys
            val totals = featureCounts.mapValues { (_, fc) -> fc.filterKeys { it in informative }.values.sum() }
            return NaiveBayes(classDocs, featureCounts, totals, informative, docs)
        }
    }

    companion object {
        const val ALPHA = 0.5
        const val MAX_DOC_FREQ = 0.4
    }
}
