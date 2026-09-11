package cz.heller.core.money

/** Podporované měny (jen zobrazení — částky se ukládají v minor units ×100 pro všechny). */
enum class AppCurrency(val code: String, val symbol: String) {
    CZK("CZK", "Kč"),
    EUR("EUR", "€"),
    USD("USD", "$"),
    GBP("GBP", "£"),
    CHF("CHF", "CHF"),
    PLN("PLN", "zł"),
    SEK("SEK", "kr"),
    NOK("NOK", "kr"),
    DKK("DKK", "kr"),
    HUF("HUF", "Ft"),
    RON("RON", "lei"),
    UAH("UAH", "₴"),
    CAD("CAD", "C$"),
    AUD("AUD", "A$"),
    JPY("JPY", "¥"),
    INR("INR", "₹");

    companion object {
        fun fromCode(code: String?): AppCurrency = entries.firstOrNull { it.code == code } ?: CZK
    }
}
