package cz.heller.core.categorize

/**
 * Co se kategorizace naučila z **potvrzených** záznamů uživatele. Staví se z tabulky záznamů
 * (žádné samostatné úložiště pravidel — záloha DB tak zálohuje i učení, a přeučení = změna
 * kategorie záznamu).
 *
 * - **Paměť identity:** protiúčet (+ variabilní symbol) a klíč obchodníka → kategorie.
 *   Většinové hlasování, při shodě vyhrává novější záznam (přidávej chronologicky).
 * - **Naivní Bayes:** zobecnění přes slova, typ pohybu a řád částky.
 * - **Legacy pravidla:** naučené klíče ze starého DataStore (jen pro čtení).
 * - **Nejčastější kategorie** podle směru — poslední záchrana pro návrhy.
 */
class CategoryModel private constructor(
    private val identity: Map<String, String>,
    /** Klíče „jen protiúčet", na které chodí platby více kategorií (liší se VS) — nesmí přebít lexikon. */
    private val ambiguous: Set<String>,
    private val legacy: Lexicon?,
    val bayes: NaiveBayes?,
    private val validCategories: Set<String>?,
    private val frequent: Map<Boolean, List<String>>,
    /** Kategorie s příjmovým typem (pro omezení Bayese na správný směr). */
    private val incomeCategories: Set<String>,
) {
    fun isValid(categoryId: String): Boolean = validCategories == null || categoryId in validCategories

    /** Směr kategorie: příjmová, nebo výdajová. Neznámou (uživatelskou bez typu) bere jako obojí. */
    fun matchesDirection(categoryId: String, isIncome: Boolean): Boolean =
        if (incomeCategories.isEmpty()) true else (categoryId in incomeCategories) == isIncome

    /**
     * Kategorie z paměti identity (protiúčet+VS → protiúčet → obchodník). Samotný protiúčet
     * se nepoužije, když má pohyb VS a na ten účet už chodily platby různých kategorií
     * (např. město: poplatek za odpad i pokuta) — pak rozhodne lexikon podle poznámky.
     */
    fun identityFor(tx: TxFeatures): String? {
        val hasVs = !tx.variableSymbol.isNullOrBlank()
        for (k in identityKeys(tx)) {
            if (hasVs && k in ambiguous) continue
            val c = identity[k] ?: continue
            if (isValid(c)) return c
        }
        return null
    }

    /** Kategorie ze starých naučených pravidel (klíč obchodníka jako fráze v názvu nebo poznámce). */
    fun legacyFor(tx: TxFeatures): String? {
        val lx = legacy ?: return null
        val m = lx.match(tx.payeeTokens, income = false) ?: lx.match(tx.noteTokens, income = false)
        return m?.categoryId?.takeIf { isValid(it) }
    }

    fun frequentFor(isIncome: Boolean): List<String> = frequent[isIncome] ?: emptyList()

    class Builder(
        private val validCategories: Set<String>? = null,
        legacyRules: Map<String, String> = emptyMap(),
        private val incomeCategories: Set<String> = emptySet(),
    ) {
        private val votes = HashMap<String, HashMap<String, Int>>()
        private val latest = HashMap<String, String>()
        private val bayes = NaiveBayes.Builder()
        private val freq = HashMap<Boolean, HashMap<String, Int>>()
        private var samples = 0
        private val legacy: Lexicon? = if (legacyRules.isEmpty()) null else Lexicon.build(
            expense = listOf(legacyRules.entries.map { it.key to it.value } to Lexicon.PRIORITY_MANUAL),
            income = emptyList(),
        )

        /** Přidá potvrzený vzorek; volej v chronologickém pořadí (starší první). */
        fun add(tx: TxFeatures, categoryId: String): Builder {
            samples++
            for (k in identityKeys(tx)) {
                val v = votes.getOrPut(k) { HashMap() }
                v[categoryId] = (v[categoryId] ?: 0) + 1
                latest[k] = categoryId
            }
            bayes.add(tx.bayesFeatures(), categoryId)
            val f = freq.getOrPut(tx.isIncome) { HashMap() }
            f[categoryId] = (f[categoryId] ?: 0) + 1
            return this
        }

        fun build(): CategoryModel {
            val identity = HashMap<String, String>(votes.size)
            val ambiguous = HashSet<String>()
            for ((k, v) in votes) {
                val best = v.values.max()
                val winners = v.filterValues { it == best }.keys
                identity[k] = if (latest[k] in winners) latest[k]!! else winners.first()
                if (k.startsWith("a:") && '|' !in k && v.size > 1) ambiguous += k
            }
            val frequent = freq.mapValues { (_, m) -> m.entries.sortedByDescending { it.value }.map { it.key } }
            return CategoryModel(
                identity = identity,
                ambiguous = ambiguous,
                legacy = legacy,
                bayes = if (samples > 0) bayes.build() else null,
                validCategories = validCategories,
                frequent = frequent,
                incomeCategories = incomeCategories,
            )
        }
    }

    companion object {
        val EMPTY: CategoryModel = Builder().build()

        /** Klíče identity pohybu v pořadí od nejspecifičtějšího. */
        fun identityKeys(tx: TxFeatures): List<String> {
            val dir = if (tx.isIncome) "i" else "o"
            val out = ArrayList<String>(3)
            val acc = tx.counterAccountDigits
            if (acc.isNotEmpty()) {
                val vs = tx.variableSymbol?.trim()?.trimStart('0').orEmpty()
                if (vs.isNotEmpty()) out += "a:$dir:$acc|$vs"
                out += "a:$dir:$acc"
            }
            val mk = tx.merchantKey
            if (mk.isNotEmpty()) out += "m:$dir:$mk"
            return out
        }
    }
}
