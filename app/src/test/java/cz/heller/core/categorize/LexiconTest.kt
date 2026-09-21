package cz.heller.core.categorize

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LexiconTest {

    private val lx = Lexicon.default

    private fun expense(text: String): String? = lx.match(MerchantText.tokens(text), income = false)?.categoryId

    @Test
    fun `normalize drops diacritics and apostrophes`() {
        assertEquals("albert vam dekuje", MerchantText.normalize("ALBERT VÁM DĚKUJE"))
        assertEquals("mcdonalds", MerchantText.normalize("McDonald's"))
        assertEquals("apple com bill", MerchantText.normalize("APPLE.COM/BILL"))
    }

    @Test
    fun `merchant key strips branch codes and stop words`() {
        assertEquals("zabka", MerchantText.key("ZABKA Z8921 K.1"))
        assertEquals("zabka", MerchantText.key("ZABKA ZC451 K.1"))
        assertEquals("albert", MerchantText.key("ALBERT 0641"))
        assertEquals("cd cz", MerchantText.key("CD CZ"))
    }

    @Test
    fun `matches whole tokens only - no substring false positives`() {
        assertNull(expense("SPARTA PRAHA FANSHOP"))
        assertNull(expense("CZECH REPUBLIC"))
        assertNull(expense("MELODY"))
        assertNull(expense("NADVORI"))
        assertEquals("food_groceries", expense("SPAR 1234"))
        assertEquals("food_dining", expense("PUB U KOCOURA"))
    }

    @Test
    fun `brands from NSI and manual rules`() {
        assertEquals("food_groceries", expense("LIDL CZ 0123"))
        assertEquals("food_groceries", expense("KAUFLAND CR 4100"))
        assertEquals("food_dining", expense("MCDONALDS 123"))
        assertEquals("food_dining", expense("BURGER KING 12"))
        assertEquals("transport_fuel", expense("SHELL 3128"))
        assertEquals("housing_home", expense("IKEA OSTRAVA"))
        assertEquals("shopping_clothes", expense("H&M 123"))
        assertEquals("comm_apps", expense("NETFLIX.COM"))
        assertEquals("work_tools", expense("GITHUB, INC."))
    }

    @Test
    fun `generic stems generalize to unknown merchants`() {
        assertEquals("food_dining", expense("PIZZERIE U KOCOURA"))
        assertEquals("food_dining", expense("Restaurace Pod Hradem"))
        assertEquals("food_dining", expense("Kavarna Na Rohu"))
        assertEquals("health_care", expense("LEKARNA NA POLIKLINICE"))
        assertEquals("health_care", expense("MUDr. Jana Sedlackova"))
        assertEquals("leisure_holidays", expense("HOTEL PANORAMA"))
        assertEquals("transport_car_other", expense("MPLA PARKOVANI"))
        assertEquals("transport_car_other", expense("AUTOSERVIS NOVAK"))
        assertEquals("health_sport", expense("KADERNICTVI LENKA"))
        assertEquals("shopping_other", expense("VETERINA MEDVED"))
    }

    @Test
    fun `longer phrase wins over shorter, manual over brand, exact over prefix`() {
        assertEquals("transport_car_insurance", expense("Generali - povinne ruceni"))
        assertEquals("financial_insurance", expense("Generali pojistovna"))
        assertEquals("financial_taxes", expense("Zdravotni pojistovna VZP"))
        // manuální „tesco" → potraviny i kdyby značka říkala něco jiného
        assertEquals("food_groceries", expense("TESCO STORES 123"))
        // přesné „skolka" (děti) vyhraje nad prefixem „skol*" (vzdělávání)
        assertEquals("shopping_other", expense("SKOLKA BERUSKA"))
        assertEquals("leisure_education", expense("SKOLA TANCE"))
    }

    @Test
    fun `income lexicon`() {
        assertEquals("income_wage", lx.match(MerchantText.tokens("Mzda 05/2026"), income = true)?.categoryId)
        assertEquals("income_refunds", lx.match(MerchantText.tokens("Vratka za zbozi"), income = true)?.categoryId)
    }
}
