package cz.heller.core.categorize

import cz.heller.core.categorize.Categorizer.Result
import cz.heller.core.categorize.Categorizer.Source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategorizerTest {

    private fun card(payee: String, city: String = "Ostrava", amount: Long = 30_000, income: Boolean = false) = TxFeatures(
        payee = payee,
        note = "${if (income) "Kredit" else "Nákup"}: $payee, $city, dne 1.6.2026, částka ${amount / 100}.00 CZK",
        isIncome = income,
        amountMinor = amount,
        txType = "Platba kartou",
    )

    private fun transfer(payee: String, note: String?, acc: String, vs: String?, amount: Long, income: Boolean = false) = TxFeatures(
        payee = payee,
        note = note,
        isIncome = income,
        amountMinor = amount,
        txType = if (income) "Bezhotovostní příjem" else "Bezhotovostní platba",
        counterAccount = acc,
        variableSymbol = vs,
    )

    private fun cat(r: Result): String? = (r as? Result.Category)?.id

    @Test
    fun `own transfer by owner name`() {
        val r = Categorizer.categorize(transfer("Jiří Novák", null, "123/2010", null, 100_000), "jiri novak", CategoryModel.EMPTY)
        assertEquals(Result.Transfer, r)
    }

    @Test
    fun `structural rules from bank transaction type`() {
        val atm = TxFeatures("CSOB ATM", "Výběr: CSOB ATM 12, Ostrava", false, 200_000, txType = "Výběr z bankomatu")
        assertEquals(Result.Category("cash_withdrawal", Source.RULE), Categorizer.categorize(atm, "", CategoryModel.EMPTY))
        val fee = TxFeatures("Fio banka", "Poplatek za vedení účtu", false, 500, txType = "Poplatek")
        assertEquals(Result.Category("financial_fees", Source.RULE), Categorizer.categorize(fee, "", CategoryModel.EMPTY))
        val refund = card("ALZA.CZ", income = true)
        assertEquals(Result.Category("income_refunds", Source.RULE), Categorizer.categorize(refund, "", CategoryModel.EMPTY))
    }

    @Test
    fun `memory beats lexicon and works by counter account and variable symbol`() {
        val model = CategoryModel.Builder()
            .add(transfer("Jan Novák", "Nájem červen", "2223334445/0100", "202606", 1_200_000), "housing_rent_mortgage")
            .add(card("ALBERT 0641"), "shopping_other") // uživatel si Albert vede jinak než slovník
            .build()

        // stejný protiúčet, jiný VS, bez poznámky → paměť podle účtu
        assertEquals(
            Result.Category("housing_rent_mortgage", Source.MEMORY),
            Categorizer.categorize(transfer("Jan Novák", null, "2223334445/0100", "202607", 1_200_000), "", model),
        )
        // obchodník s jiným kódem pobočky → paměť podle klíče obchodníka, přebije slovník
        assertEquals(
            Result.Category("shopping_other", Source.MEMORY),
            Categorizer.categorize(card("ALBERT 0912"), "", model),
        )
    }

    @Test
    fun `longer note phrase beats short payee match`() {
        val tx = transfer("Generali Česká pojišťovna", "Povinné ručení", "5555666688/0800", "77", 280_000)
        assertEquals("transport_car_insurance", cat(Categorizer.categorize(tx, "", CategoryModel.EMPTY)))
    }

    @Test
    fun `account-only memory yields to lexicon when the account carries mixed categories`() {
        val model = CategoryModel.Builder()
            .add(transfer("Město", "Poplatek za odpad", "1649297309/0800", "1", 120_000), "financial_fees")
            .add(transfer("Město", "Pokuta", "1649297309/0800", "2", 50_000), "financial_fees")
            .build()
        // nový VS, poznámka říká pokuta → lexikon, ne většinová paměť účtu
        val tx = transfer("Statutární město Ostrava", "Pokuta za parkování", "1649297309/0800", "3", 50_000)
        assertEquals("financial_fees", cat(Categorizer.categorize(tx, "", model)))
        // známý VS → paměť
        val known = transfer("Město", null, "1649297309/0800", "1", 120_000)
        assertEquals("financial_fees", cat(Categorizer.categorize(known, "", model)))
    }

    @Test
    fun `income from a known merchant without income rule is a refund`() {
        val tx = TxFeatures("ROSSMANN 123", "Vrácení zboží", true, 20_000, txType = "Bezhotovostní příjem")
        assertEquals("income_refunds", cat(Categorizer.categorize(tx, "", CategoryModel.EMPTY)))
    }

    @Test
    fun `lexicon categorizes unknown merchants by generic words`() {
        assertEquals(Result.Category("food_dining", Source.LEXICON), Categorizer.categorize(card("PIZZERIE U KOCOURA"), "", CategoryModel.EMPTY))
        assertEquals(Result.Category("food_groceries", Source.LEXICON), Categorizer.categorize(card("LIDL CZ 0123"), "", CategoryModel.EMPTY))
    }

    @Test
    fun `bayes generalizes from confirmed history only with text evidence`() {
        val b = CategoryModel.Builder(incomeCategories = setOf("income_wage"))
        repeat(3) { b.add(card("U TRI VEVEREK $it", "Beskydy"), "leisure_holidays") }
        repeat(3) { b.add(card("CHATA PRASIVA $it", "Beskydy"), "leisure_holidays") }
        b.add(card("ALBERT 0641", "Havirov"), "food_groceries")
        b.add(card("LIDL CZ 0123", "Havirov"), "food_groceries")
        val model = b.build()

        // jiný klíč obchodníka, ale sdílená slova „tri veverek" + „beskydy" → Bayes zařadí
        val r = Categorizer.categorize(card("TRI VEVEREK BESKYDY 7", "Beskydy"), "", model)
        assertEquals(Source.BAYES, (r as Result.Category).source)
        assertEquals("leisure_holidays", r.id)

        // úplně neznámý název bez známých slov → nejistý, ale s tipy z historie
        val u = Categorizer.categorize(card("XY TRADE S.R.O.", "Ostrava"), "", model)
        assertTrue(u is Result.Uncertain)
        assertTrue((u as Result.Uncertain).suggestions.isNotEmpty())
        assertEquals("leisure_holidays", u.suggestions.first())
    }

    @Test
    fun `invalid categories are skipped`() {
        val model = CategoryModel.Builder(validCategories = setOf("food_dining")).build()
        // LIDL → food_groceries neexistuje → nejistý
        assertTrue(Categorizer.categorize(card("LIDL CZ 0123"), "", model) is Result.Uncertain)
        assertEquals("food_dining", cat(Categorizer.categorize(card("PIZZERIE U KOCOURA"), "", model)))
    }

    @Test
    fun `suggestions are ordered and distinct`() {
        val model = CategoryModel.Builder()
            .add(card("ALBERT 0641"), "food_groceries")
            .add(card("ALBERT 0641"), "food_groceries")
            .add(card("KFC OSTRAVA"), "food_dining")
            .build()
        val s = Categorizer.suggestions(card("PIZZERIE U KOCOURA"), model)
        assertEquals("food_dining", s.first())
        assertEquals(s.size, s.distinct().size)
    }
}
