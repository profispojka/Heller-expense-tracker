package cz.heller.core.money

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {

    private val nbsp = ' '

    @Test
    fun formatsInEachStyle() {
        assertEquals("9${nbsp}133,58$nbsp$", Money.format(913_358, false, AmountFormat.CZECH, "$"))
        assertEquals("$9,133.58", Money.format(913_358, false, AmountFormat.US, "$"))
        assertEquals("9.133,58$nbsp€", Money.format(913_358, false, AmountFormat.EUROPEAN, "€"))
    }

    @Test
    fun usStyleSpacesLetterSymbolsAndKeepsSigns() {
        assertEquals("CHF${nbsp}1,000.00", Money.format(100_000, false, AmountFormat.US, "CHF"))
        assertEquals("+ $12.50", Money.format(1_250, true, AmountFormat.US, "$"))
        assertEquals("− $1,234,567.00", Money.format(-123_456_700, false, AmountFormat.US, "$"))
    }

    @Test
    fun parsesPlainAndGroupedInput() {
        assertEquals(913_358L, Money.parseToMinor("9,133.58", AmountFormat.US))
        assertEquals(913_300L, Money.parseToMinor("9,133", AmountFormat.US))
        assertEquals(913_358L, Money.parseToMinor("9.133,58", AmountFormat.EUROPEAN))
        assertEquals(913_300L, Money.parseToMinor("9.133", AmountFormat.EUROPEAN))
        assertEquals(913_358L, Money.parseToMinor("9 133,58", AmountFormat.CZECH))
        // Dosavadní chování: jediná čárka/tečka je desetinná.
        assertEquals(123_450L, Money.parseToMinor("1234,5", AmountFormat.CZECH))
        assertEquals(123_450L, Money.parseToMinor("1234.5", AmountFormat.CZECH))
        assertEquals(12_50L, Money.parseToMinor("12,5", AmountFormat.US))
        assertEquals(1_234_567_00L, Money.parseToMinor("1,234,567", AmountFormat.US))
        assertEquals(0L, Money.parseToMinor("", AmountFormat.US))
        assertEquals(null, Money.parseToMinor("abc", AmountFormat.US))
    }
}
