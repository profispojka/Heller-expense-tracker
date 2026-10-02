package cz.heller.core.money

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

/**
 * Formátování peněz. Aplikace je jednoměnová — měnu si uživatel zvolí na začátku (kosmetické,
 * částky jsou vždy v minor units ×100). Číselný formát je ve výchozím stavu český (mezera tisíce,
 * čárka desetiny); v Nastavení lze přepnout na jiný [AmountFormat]. Žádný float — výpočty přes BigDecimal.
 */
object Money {

    private val LOCALE: Locale = Locale.forLanguageTag("cs-CZ")
    private const val MINUS = "−" // typografické minus
    private const val NBSP = "\u00A0" // symbol měny se neodtrhne od čísla

    /** Symbol zvolené měny (Kč/€/$/£/CHF…). Nastavuje se při startu z nastavení (viz [applyCurrency]). */
    @Volatile
    var currencySymbol: String = AppCurrency.CZK.symbol

    /** Nastaví zvolenou měnu podle kódu (viz [AppCurrency]). */
    fun applyCurrency(code: String?) {
        currencySymbol = AppCurrency.fromCode(code).symbol
    }

    /**
     * Zvolený styl zápisu částek. Snapshot state, aby se po změně v Nastavení překreslily
     * všechny composable, které během kompozice volají [format].
     */
    var amountFormat: AmountFormat by mutableStateOf(AmountFormat.CZECH)
        private set

    /** Nastaví styl zápisu částek podle kódu (viz [AmountFormat]). */
    fun applyAmountFormat(code: String?) {
        amountFormat = AmountFormat.fromCode(code)
    }

    /** Hodnota v hlavní jednotce jako BigDecimal. */
    fun toMajor(amountMinor: Long): BigDecimal = BigDecimal(amountMinor).movePointLeft(2)

    /**
     * Naformátuje částku se symbolem zvolené měny. Při [withSign] přidá + / − a nezáporné/záporné
     * rozliší znaménkem (na E-Ink nese význam znaménko a tučnost, ne barva).
     */
    fun format(amountMinor: Long, withSign: Boolean = false): String =
        format(amountMinor, withSign, amountFormat, currencySymbol)

    /** Varianta s explicitním stylem a symbolem (náhled v Nastavení, testy). */
    fun format(amountMinor: Long, withSign: Boolean, style: AmountFormat, symbol: String): String {
        val number = formatNumber(toMajor(kotlin.math.abs(amountMinor)), style)
        val base = when {
            !style.symbolBefore -> number + NBSP + symbol
            // "CHF 9,133.58", ale "$9,133.58" / "C$9,133.58".
            symbol.lastOrNull()?.isLetter() == true -> symbol + NBSP + number
            else -> symbol + number
        }
        return when {
            withSign && amountMinor > 0 -> "+ $base"
            amountMinor < 0 -> "$MINUS $base"
            else -> base
        }
    }

    private fun formatNumber(value: BigDecimal, style: AmountFormat): String {
        val nf = if (style == AmountFormat.CZECH) {
            NumberFormat.getNumberInstance(LOCALE)
        } else {
            val symbols = DecimalFormatSymbols(Locale.ROOT).apply {
                groupingSeparator = style.groupingSeparator
                decimalSeparator = style.decimalSeparator
            }
            DecimalFormat("#,##0.00", symbols)
        }
        nf.minimumFractionDigits = 2
        nf.maximumFractionDigits = 2
        return nf.format(value)
    }

    /**
     * Prostý editovatelný řetězec částky pro textové pole ("5000", "5000.5"; v evropském stylu
     * "5000,5"). Bez oddělovačů tisíců, ať jde snadno upravit.
     */
    fun toPlainAmount(amountMinor: Long): String {
        val plain = toMajor(amountMinor).stripTrailingZeros().toPlainString()
        return if (amountFormat == AmountFormat.EUROPEAN) plain.replace('.', ',') else plain
    }

    /**
     * Naparsuje text na minor units. null = neplatné, prázdné = 0. Přijímá "1234,50", "1234.50"
     * i zápisy s oddělovači tisíců ("9,133.58", "9.133,58", "9 133,58") — viz [normalizeAmount].
     */
    fun parseToMinor(text: String): Long? = parseToMinor(text, amountFormat)

    fun parseToMinor(text: String, style: AmountFormat): Long? {
        val cleaned = normalizeAmount(text.filterNot { it.isWhitespace() }, style)
        if (cleaned.isEmpty()) return 0
        return try {
            BigDecimal(cleaned).movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
        } catch (e: NumberFormatException) {
            null
        }
    }

    /**
     * Převede čárky/tečky na tvar pro BigDecimal ("9133.58"):
     * - obě značky → desetinná je ta poslední, druhá jsou tisíce,
     * - jedna značka vícekrát → tisíce,
     * - jedna značka jednou → tisíce jen tehdy, je-li to oddělovač tisíců zvoleného stylu
     *   a následují přesně 3 číslice ("9,133" v US stylu); jinak desetinná (dosavadní chování).
     */
    private fun normalizeAmount(s: String, style: AmountFormat): String {
        val lastComma = s.lastIndexOf(',')
        val lastDot = s.lastIndexOf('.')
        if (lastComma >= 0 && lastDot >= 0) {
            val decimal = if (lastComma > lastDot) ',' else '.'
            val grouping = if (decimal == ',') '.' else ','
            return s.replace(grouping.toString(), "").replace(decimal, '.')
        }
        val mark = when {
            lastComma >= 0 -> ','
            lastDot >= 0 -> '.'
            else -> return s
        }
        val count = s.count { it == mark }
        val isGrouping = count > 1 ||
            (mark == style.groupingSeparator && s.length - s.indexOf(mark) - 1 == 3)
        return if (isGrouping) s.replace(mark.toString(), "") else s.replace(mark, '.')
    }
}
