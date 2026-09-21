package cz.heller.core.categorize

/**
 * Vstup kategorizace — vše, co o pohybu víme, nezávisle na Roomu i na Fiu.
 * [counterAccount] je číslo protiúčtu (bez kódu banky nebo s ním, normalizuje se), [txType]
 * je typ pohybu z banky (Fio column8, např. „Platba kartou", „Výběr z bankomatu").
 */
data class TxFeatures(
    val payee: String?,
    val note: String?,
    val isIncome: Boolean,
    val amountMinor: Long = 0,
    val txType: String? = null,
    val counterAccount: String? = null,
    val variableSymbol: String? = null,
) {
    val payeeTokens: List<String> by lazy { MerchantText.tokens(payee) }
    val noteTokens: List<String> by lazy { MerchantText.tokens(note) }
    val txTypeNorm: String by lazy { MerchantText.normalize(txType) }

    /** Číslo protiúčtu jen číslice (bez kódu banky za „/"); "" když chybí. */
    val counterAccountDigits: String by lazy {
        counterAccount?.substringBefore('/')?.filter { it.isDigit() } ?: ""
    }

    /** Klíč obchodníka pro paměť identity (viz [MerchantText.key]). */
    val merchantKey: String by lazy { MerchantText.key(payee) }

    /**
     * Příznaky pro naivní Bayes: slova z názvu plátce (p:) a poznámky (n:), typ pohybu (t:),
     * řád částky (a:) a zda jde o karetní platbu bez protiúčtu (k:).
     */
    fun bayesFeatures(): List<String> {
        val out = ArrayList<String>()
        for (t in payeeTokens) if (MerchantText.isWord(t) && t !in MerchantText.STOP) out += "p:$t"
        for (t in noteTokens) if (MerchantText.isWord(t) && t !in MerchantText.STOP) out += "n:$t"
        if (txTypeNorm.isNotEmpty()) out += "t:$txTypeNorm"
        out += "a:" + amountBucket(amountMinor)
        out += if (counterAccountDigits.isEmpty()) "k:card" else "k:transfer"
        return out.distinct()
    }

    companion object {
        /** Řád částky: 0 = do 50, 1 = do 200, 2 = do 500, 3 = do 2 000, 4 = do 10 000, 5 = víc (v hlavní měně). */
        fun amountBucket(minor: Long): Int = when {
            minor < 5_000 -> 0
            minor < 20_000 -> 1
            minor < 50_000 -> 2
            minor < 200_000 -> 3
            minor < 1_000_000 -> 4
            else -> 5
        }
    }
}
