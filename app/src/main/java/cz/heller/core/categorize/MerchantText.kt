package cz.heller.core.categorize

import java.text.Normalizer

/**
 * Normalizace textu obchodníka / poznámky pro porovnávání a učení.
 * Obecné (nezávislé na bance): malá písmena, bez diakritiky, apostrofy zahozeny („McDonald's" →
 * „mcdonalds"), vše ostatní mimo [a-z0-9] je oddělovač tokenů.
 */
object MerchantText {

    private val diacritics = Regex("\\p{M}+")
    private val apostrophes = Regex("[’'`´]")
    private val nonAlnum = Regex("[^a-z0-9]+")

    /** „ALBERT VÁM DĚKUJE" → „albert vam dekuje". */
    fun normalize(s: String?): String {
        if (s.isNullOrBlank()) return ""
        val stripped = Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(diacritics, "")
            .replace(apostrophes, "")
        return stripped.lowercase().replace(nonAlnum, " ").trim()
    }

    /** Tokeny normalizovaného textu (bez prázdných). */
    fun tokens(s: String?): List<String> {
        val n = normalize(s)
        return if (n.isEmpty()) emptyList() else n.split(' ')
    }

    /**
     * Klíč identity obchodníka: normalizováno a bez „kódových" tokenů (číselné kódy, krátké
     * zkratky, stop slova), aby „ZABKA Z8921 K.1" i „ZABKA ZC451 K.1" daly stejný klíč „zabka".
     * Když po filtru nic nezbude (např. „CD CZ"), vrátí celý normalizovaný text.
     */
    fun key(s: String?): String {
        val norm = normalize(s)
        if (norm.isEmpty()) return ""
        val tokens = norm.split(' ').filter { t -> isWord(t) && t !in STOP }
        return tokens.joinToString(" ").ifBlank { norm }
    }

    /** Slovní token: aspoň 3 znaky a žádná číslice (kódy poboček, částky, data se vyřadí). */
    fun isWord(t: String): Boolean = t.length >= 3 && t.none { it.isDigit() }

    /**
     * Tokeny, které nenesou informaci o obchodníkovi: právní formy, platební šum, spojky a
     * platební brány / terminály („NYX *AUTOTOMAN", „SUMUP *ESKAFE", „GOPAY *IDOKLAD").
     */
    val STOP: Set<String> = setOf(
        "sro", "spol", "as", "gmbh", "ltd", "inc", "llc", "kasa", "pos", "kiosk", "www", "com",
        "platba", "kartou", "nakup", "vyber", "kredit", "the", "and", "und", "dne", "castka",
        "czk", "eur", "pln", "usd", "cz", "sk", "pl", "de", "at",
        "nyx", "sumup", "gopay", "paypal", "zettle", "adyen", "payu", "comgate", "stripe", "bkg", "sq", "sp",
    )
}
