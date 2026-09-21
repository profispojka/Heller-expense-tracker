package cz.heller.core.categorize

/**
 * Jádro kategorizace. Čisté funkce, nezávislé na Roomu i na Fiu.
 *
 * Vrstvy v pořadí:
 * 1. **Vlastní převod** — protistrana = majitel účtu → [Result.Transfer].
 * 2. **Strukturální pravidla** — typ pohybu z banky (bankomat, poplatek) a refundace („Kredit").
 * 3. **Paměť identity** — protiúčet + VS nebo obchodník, jak ho uživatel sám zařadil.
 * 4. **Legacy naučená pravidla** ze starého úložiště.
 * 5. **Lexikon** — ruční pravidla, značky (NSI), obecná slova; nejdřív název plátce, pak poznámka.
 *    Příjem od známého obchodníka bez příjmového pravidla = refundace.
 * 5b. **Provozovny (POI)** — index názvů z Overture Maps pro CZ/SK/PL ([PoiIndex]). Použije se,
 *    když lexikon nic nenašel, nebo když našel jen obecné slovo a provozovna sedí na víc tokenů.
 * 6. **Naivní Bayes** — jen když má textovou oporu a je si jistý ([BAYES_MIN_POSTERIOR]).
 * 7. Jinak [Result.Uncertain] s návrhy pro uživatele.
 */
object Categorizer {

    enum class Source { RULE, MEMORY, LEXICON, POI, BAYES }

    sealed interface Result {
        /** Vlastní převod mezi účty (jméno protistrany = majitel účtu) → typ TRANSFER, mimo statistiky. */
        data object Transfer : Result
        data class Category(val id: String, val source: Source) : Result
        /** Nic jistého; [suggestions] jsou seřazené tipy (může být prázdné). */
        data class Uncertain(val suggestions: List<String>) : Result
    }

    const val BAYES_MIN_POSTERIOR = 0.8
    const val BAYES_MIN_SAMPLES = 2

    fun categorize(
        tx: TxFeatures,
        ownerNorm: String,
        model: CategoryModel,
        lexicon: Lexicon = Lexicon.default,
        poi: PoiIndex? = PoiIndex.installed,
    ): Result {
        // 1) Vlastní převod — protistrana = majitel účtu.
        if (ownerNorm.isNotEmpty() && MerchantText.normalize(tx.payee) == ownerNorm) return Result.Transfer

        // 2) Strukturální pravidla.
        structural(tx)?.let { if (model.isValid(it)) return Result.Category(it, Source.RULE) }

        // 3) Paměť identity (uživatelovo vlastní zařazení má přednost před vším ostatním).
        model.identityFor(tx)?.let { return Result.Category(it, Source.MEMORY) }

        // 4) Legacy naučená pravidla.
        model.legacyFor(tx)?.let { return Result.Category(it, Source.MEMORY) }

        // 5) Lexikon + provozovny.
        val lex = lexiconMatch(tx, lexicon)
        // Provozovny jen u karetních plateb (bez protiúčtu) — u převodů je plátce jméno osoby.
        val poiMatch = if (poi != null && !tx.isIncome && tx.counterAccountDigits.isEmpty()) {
            poi.match(tx.payeeTokens)?.takeIf { model.isValid(it.categoryId) }
        } else null
        if (poiMatch != null && poiBeatsLexicon(poiMatch, lex)) return Result.Category(poiMatch.categoryId, Source.POI)
        lexiconFor(tx, model, lexicon)?.let { return Result.Category(it, Source.LEXICON) }
        if (poiMatch != null) return Result.Category(poiMatch.categoryId, Source.POI)

        // 6) Naivní Bayes.
        val bayes = model.bayes
        if (bayes != null) {
            val features = tx.bayesFeatures()
            if (bayes.hasTextEvidence(features)) {
                val scored = bayes.classify(features) { model.isValid(it) && model.matchesDirection(it, tx.isIncome) }
                val top = scored.firstOrNull()
                if (top != null && top.posterior >= BAYES_MIN_POSTERIOR && bayes.samples(top.categoryId) >= BAYES_MIN_SAMPLES) {
                    return Result.Category(top.categoryId, Source.BAYES)
                }
            }
        }

        return Result.Uncertain(suggestions(tx, model, lexicon = lexicon))
    }

    /**
     * Seřazené tipy pro uživatele (bez duplicit): jistý výsledek, pak Bayes podle
     * pravděpodobnosti, pak nejčastější kategorie daného směru.
     */
    fun suggestions(
        tx: TxFeatures,
        model: CategoryModel,
        max: Int = 3,
        lexicon: Lexicon = Lexicon.default,
        poi: PoiIndex? = PoiIndex.installed,
    ): List<String> {
        val out = LinkedHashSet<String>()
        structural(tx)?.let { out += it }
        model.identityFor(tx)?.let { out += it }
        model.legacyFor(tx)?.let { out += it }
        lexiconFor(tx, model, lexicon)?.let { out += it }
        if (poi != null && !tx.isIncome && tx.counterAccountDigits.isEmpty()) poi.match(tx.payeeTokens)?.let { out += it.categoryId }
        val bayes = model.bayes
        if (bayes != null) {
            val features = tx.bayesFeatures()
            if (bayes.hasTextEvidence(features)) {
                bayes.classify(features) { model.matchesDirection(it, tx.isIncome) }
                    .filter { it.posterior >= SUGGEST_MIN_POSTERIOR }
                    .forEach { out += it.categoryId }
            }
        }
        model.frequentFor(tx.isIncome).forEach { out += it }
        return out.filter { model.isValid(it) }.take(max)
    }

    private const val SUGGEST_MIN_POSTERIOR = 0.05
    private val INSTITUTIONAL_PREFIXES = listOf("financial_", "housing_", "investments")

    private fun structural(tx: TxFeatures): String? {
        val type = tx.txTypeNorm
        if (type.contains("bankomat")) return "cash_withdrawal"
        // Vše, co banka vede jako poplatek (i „Poplatek - pojištění hypotéky"), je poplatek — tak to
        // uživatelé vedou ve svých záznamech.
        if (type.contains("poplatek")) return "financial_fees"
        if (type.contains("dobiti")) return "comm_phone_internet"
        if (!tx.isIncome && tx.noteTokens.firstOrNull() == "vyber" && tx.counterAccountDigits.isEmpty()) return "cash_withdrawal"
        // Příjmová refundace karetní platby („Kredit: …").
        if (tx.isIncome && ("kredit" in tx.payeeTokens || "kredit" in tx.noteTokens)) return "income_refunds"
        return null
    }

    /** Surová shoda lexikonu (bez validace kategorií) — kvůli porovnání s provozovnou. */
    private fun lexiconMatch(tx: TxFeatures, lexicon: Lexicon): Lexicon.Match? =
        if (tx.isIncome) bestMatch(tx, lexicon, income = true) else bestMatch(tx, lexicon, income = false)

    /**
     * Provozovna přebije lexikon, jen když lexikon nic nemá, nebo našel jediné obecné slovo
     * uvnitř názvu (ne na začátku — „Koupaliště Riviera" je koupaliště, i když ho mapa vede
     * jako atrakci) a provozovna sedí aspoň na dva tokeny.
     */
    private fun poiBeatsLexicon(poi: PoiIndex.Match, lex: Lexicon.Match?): Boolean {
        if (lex == null) return true
        return lex.rule.priority == Lexicon.PRIORITY_GENERIC && lex.rule.size == 1 && lex.position > 0 && poi.tokens >= 2
    }

    private fun lexiconFor(tx: TxFeatures, model: CategoryModel, lexicon: Lexicon): String? {
        if (tx.isIncome) {
            val m = bestMatch(tx, lexicon, income = true)
            if (m != null && model.isValid(m.categoryId)) return m.categoryId
            // Příjem od známého obchodníka (vrácené zboží apod.) = refundace. Od instituce
            // (pojišťovna, zdravotní pojišťovna, banka) zůstává její kategorie — přeplatek pojistného
            // je „pojištění se znaménkem plus", ne refundace nákupu.
            val e = lexicon.match(tx.payeeTokens, income = false)
            if (e != null && e.rule.priority >= Lexicon.PRIORITY_BRAND) {
                val institutional = INSTITUTIONAL_PREFIXES.any { e.categoryId.startsWith(it) }
                if (institutional && model.isValid(e.categoryId)) return e.categoryId
                if (model.isValid("income_refunds")) return "income_refunds"
            }
            return null
        }
        return bestMatch(tx, lexicon, income = false)?.categoryId?.takeIf { model.isValid(it) }
    }

    /**
     * Shoda v názvu plátce má přednost, ale delší (specifičtější) fráze v poznámce ji přebije:
     * „Generali" + poznámka „povinné ručení" → pojištění vozidla, ne obecné pojištění.
     */
    private fun bestMatch(tx: TxFeatures, lexicon: Lexicon, income: Boolean): Lexicon.Match? {
        val p = lexicon.match(tx.payeeTokens, income)
        val n = lexicon.match(tx.noteTokens, income)
        if (p == null) return n
        if (n == null) return p
        return if (n.rule.size > p.rule.size) n else p
    }
}
