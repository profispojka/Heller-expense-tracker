package cz.heller.core.money

/**
 * Styl zápisu částek (oddělovač tisíců, desetinná značka, pozice symbolu). Jen zobrazení —
 * uložené hodnoty se nemění. Výchozí je [CZECH] (dosavadní chování aplikace).
 */
enum class AmountFormat(
    val code: String,
    val groupingSeparator: Char,
    val decimalSeparator: Char,
    val symbolBefore: Boolean,
) {
    /** 9 133,58 $ — mezera (NBSP) tisíce, čárka desetiny, symbol za číslem. */
    CZECH("cs", ' ', ',', symbolBefore = false),

    /** $9,133.58 — čárka tisíce, tečka desetiny, symbol před číslem. */
    US("us", ',', '.', symbolBefore = true),

    /** 9.133,58 $ — tečka tisíce, čárka desetiny, symbol za číslem (DE/AT/PL…). */
    EUROPEAN("eu", '.', ',', symbolBefore = false);

    companion object {
        fun fromCode(code: String?): AmountFormat = entries.firstOrNull { it.code == code } ?: CZECH
    }
}
