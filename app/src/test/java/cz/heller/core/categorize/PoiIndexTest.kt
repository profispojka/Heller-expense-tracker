package cz.heller.core.categorize

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class PoiIndexTest {

    @Test
    fun `fnv1a64 matches the generator`() {
        // Referenční hodnoty FNV-1a 64: prázdný řetězec = offset basis, "a" = 0xaf63dc4c8601ec8c.
        assertEquals(-0x340d631b7bdddcdbL, PoiIndex.fnv1a64(""))
        assertEquals(-0x509c23b379fe1374L, PoiIndex.fnv1a64("a"))
    }

    @Test
    fun `index loads and finds a known local business`() {
        val idx = TestPoi.index
        assumeTrue("poi_index.bin chybí", idx != null)
        assertTrue(idx!!.size > 100_000)
        // Ostravský podnik, který není v žádném slovníku značek.
        assertEquals("food_dining", idx.match(MerchantText.tokens("U CERNEHO STROMU"))?.categoryId)
        // Kód pobočky a právní forma se ignorují; jednopísmenné „u" také.
        assertEquals("food_dining", idx.match(MerchantText.tokens("DUE FRATELLI 12 S.R.O."))?.categoryId)
        assertNull(idx.match(MerchantText.tokens("XY 1234")))
        // Krátký jednoslovný zbytek („Černého" = parkoviště v Praze) se nesmí použít.
        assertNull(idx.match(MerchantText.tokens("CERNEHO")))
    }

    @Test
    fun `poi beats a single generic word but not a brand or manual rule`() {
        val idx = TestPoi.index
        assumeTrue("poi_index.bin chybí", idx != null)
        val tx = TxFeatures("FRATELLI OSTRAVA", "Nákup: FRATELLI OSTRAVA, OSTRAVA, CZ", false, 20_000, txType = "Karetní transakce")
        val r = Categorizer.categorize(tx, "", CategoryModel.EMPTY, poi = idx)
        assertEquals(Categorizer.Result.Category("food_dining", Categorizer.Source.POI), r)
        // Značka z NSI má přednost před provozovnou.
        val lidl = TxFeatures("LIDL CZ 0123", null, false, 20_000)
        assertEquals(Categorizer.Source.LEXICON, (Categorizer.categorize(lidl, "", CategoryModel.EMPTY, poi = idx) as Categorizer.Result.Category).source)
    }
}
