package cz.heller.core.categorize

/**
 * Slovník „fráze → kategorie" s porovnáváním po **celých tokenech**. Klíč je normalizovaná
 * fráze (viz [MerchantText.normalize]); token zakončený `*` je prefix („restaur*" chytí
 * restaurace / restaurant / restauracja), token začínající `*` je přípona („*cafe" chytí
 * crosscafe / eskafe). „spar" tak už nechytí „sparta" ani „pub" „republic".
 *
 * Při více shodách vyhrává delší fráze, pak vyšší priorita zdroje (ruční > značky > obecná
 * slova), pak přesnější (méně prefixů), pak dřívější pozice v textu. Obecné slovo na začátku
 * názvu („RESTAURACJA CONCORDIA") přebije značku uvnitř názvu, ne však ruční pravidlo.
 */
class Lexicon private constructor(
    private val expenseExact: Map<String, List<Rule>>,
    private val expenseFuzzy: List<Rule>,
    private val incomeExact: Map<String, List<Rule>>,
    private val incomeFuzzy: List<Rule>,
) {
    enum class Mode { EXACT, PREFIX, SUFFIX }

    class Rule(
        val tokens: List<String>,
        val modes: List<Mode>,
        val categoryId: String,
        val priority: Int,
        val order: Int,
    ) {
        val size: Int get() = tokens.size
        /** Počet přesných tokenů — přesnější pravidlo vyhrává nad prefixem/příponou. */
        val exactness: Int = modes.count { it == Mode.EXACT }
        val fuzzyFirst: Boolean get() = modes[0] != Mode.EXACT

        fun matchesAt(text: List<String>, start: Int): Boolean {
            if (start + tokens.size > text.size) return false
            for (i in tokens.indices) if (!tokenMatches(text[start + i], tokens[i], modes[i])) return false
            return true
        }

        private fun tokenMatches(t: String, r: String, mode: Mode): Boolean = when (mode) {
            Mode.EXACT -> t == r
            Mode.PREFIX -> t.startsWith(r)
            Mode.SUFFIX -> t.length > r.length && t.endsWith(r)
        }
    }

    data class Match(val categoryId: String, val rule: Rule, val position: Int)

    /** Nejlepší shoda v tokenech; [income] přepne na příjmový slovník. */
    fun match(text: List<String>, income: Boolean): Match? {
        if (text.isEmpty()) return null
        val exact = if (income) incomeExact else expenseExact
        val fuzzy = if (income) incomeFuzzy else expenseFuzzy
        var best: Match? = null
        for (i in text.indices) {
            exact[text[i]]?.forEach { r -> if (r.matchesAt(text, i)) best = pick(best, Match(r.categoryId, r, i)) }
            for (r in fuzzy) if (r.matchesAt(text, i)) best = pick(best, Match(r.categoryId, r, i))
        }
        return best
    }

    private fun pick(best: Match?, m: Match): Match {
        if (best == null) return m
        val r = m.rule; val b = best.rule
        if (r.size != b.size) return if (r.size > b.size) m else best
        val rp = effectivePriority(m); val bp = effectivePriority(best)
        if (rp != bp) return if (rp > bp) m else best
        if (r.exactness != b.exactness) return if (r.exactness > b.exactness) m else best
        return best // stejná kvalita: text se prochází zleva, dřívější pozice zůstává
    }

    /** Obecné slovo na začátku názvu je silnější než značka uvnitř („HOTEL X", „RESTAURACJA Y"). */
    private fun effectivePriority(m: Match): Double =
        if (m.rule.priority == PRIORITY_GENERIC && m.position == 0) PRIORITY_BRAND + 0.5 else m.rule.priority.toDouble()

    companion object {
        const val PRIORITY_MANUAL = 3
        const val PRIORITY_BRAND = 2
        const val PRIORITY_GENERIC = 1

        /** Výchozí slovník: ruční pravidla + značky (NSI) + obecná slova. */
        val default: Lexicon by lazy {
            build(
                expense = listOf(
                    SeedRules.expense to PRIORITY_MANUAL,
                    BrandLexicon.rules to PRIORITY_BRAND,
                    GenericLexicon.expense to PRIORITY_GENERIC,
                ),
                income = listOf(
                    SeedRules.income to PRIORITY_MANUAL,
                    GenericLexicon.income to PRIORITY_GENERIC,
                ),
            )
        }

        fun build(
            expense: List<Pair<List<Pair<String, String>>, Int>>,
            income: List<Pair<List<Pair<String, String>>, Int>>,
        ): Lexicon {
            val (ee, ef) = index(expense)
            val (ie, iff) = index(income)
            return Lexicon(ee, ef, ie, iff)
        }

        private fun index(sources: List<Pair<List<Pair<String, String>>, Int>>): Pair<Map<String, List<Rule>>, List<Rule>> {
            val exact = HashMap<String, MutableList<Rule>>()
            val fuzzy = ArrayList<Rule>()
            var order = 0
            for ((rules, priority) in sources) {
                for ((key, cat) in rules) {
                    val rule = parse(key, cat, priority, order++) ?: continue
                    if (rule.fuzzyFirst) fuzzy += rule else exact.getOrPut(rule.tokens[0]) { ArrayList() } += rule
                }
            }
            return exact to fuzzy
        }

        private fun parse(key: String, categoryId: String, priority: Int, order: Int): Rule? {
            val raw = key.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
            if (raw.isEmpty()) return null
            val tokens = ArrayList<String>(raw.size)
            val modes = ArrayList<Mode>(raw.size)
            for (t in raw) {
                val mode = when {
                    t.endsWith("*") -> Mode.PREFIX
                    t.startsWith("*") -> Mode.SUFFIX
                    else -> Mode.EXACT
                }
                val clean = t.trim('*')
                if (clean.isEmpty()) return null
                tokens += clean
                modes += mode
            }
            return Rule(tokens, modes, categoryId, priority, order)
        }
    }
}
