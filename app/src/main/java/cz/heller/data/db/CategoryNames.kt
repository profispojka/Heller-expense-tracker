package cz.heller.data.db

import android.content.Context
import androidx.annotation.StringRes
import cz.heller.R

/**
 * Lokalizace názvů přednastavených kategorií podle stabilního `id` (slugu).
 * Vlastní (uživatelské) kategorie mají náhodné UUID id, takže v mapě nejsou
 * a zobrazí se jejich uložený název beze změny.
 */
object CategoryNames {

    private val res: Map<String, Int> = mapOf(
        "food" to R.string.cat_food,
        "food_groceries" to R.string.cat_food_groceries,
        "food_dining" to R.string.cat_food_dining,
        "transport" to R.string.cat_transport,
        "transport_public" to R.string.cat_transport_public,
        "transport_taxi" to R.string.cat_transport_taxi,
        "transport_fuel" to R.string.cat_transport_fuel,
        "transport_car_insurance" to R.string.cat_transport_car_insurance,
        "transport_car_other" to R.string.cat_transport_car_other,
        "housing" to R.string.cat_housing,
        "housing_rent_mortgage" to R.string.cat_housing_rent_mortgage,
        "housing_utilities" to R.string.cat_housing_utilities,
        "housing_home" to R.string.cat_housing_home,
        "shopping" to R.string.cat_shopping,
        "shopping_electronics" to R.string.cat_shopping_electronics,
        "shopping_clothes" to R.string.cat_shopping_clothes,
        "shopping_drugstore" to R.string.cat_shopping_drugstore,
        "shopping_other" to R.string.cat_shopping_other,
        "health" to R.string.cat_health,
        "health_care" to R.string.cat_health_care,
        "health_sport" to R.string.cat_health_sport,
        "leisure" to R.string.cat_leisure,
        "leisure_culture" to R.string.cat_leisure_culture,
        "leisure_holidays" to R.string.cat_leisure_holidays,
        "leisure_education" to R.string.cat_leisure_education,
        "comm" to R.string.cat_comm,
        "comm_phone_internet" to R.string.cat_comm_phone_internet,
        "comm_apps" to R.string.cat_comm_apps,
        "work" to R.string.cat_work,
        "work_tools" to R.string.cat_work_tools,
        "work_other" to R.string.cat_work_other,
        "financial" to R.string.cat_financial,
        "financial_insurance" to R.string.cat_financial_insurance,
        "financial_taxes" to R.string.cat_financial_taxes,
        "financial_loans" to R.string.cat_financial_loans,
        "financial_fees" to R.string.cat_financial_fees,
        "investments" to R.string.cat_investments,
        "charity" to R.string.cat_charity,
        "cash_withdrawal" to R.string.cat_cash_withdrawal,
        "others" to R.string.cat_others,
        "income" to R.string.cat_income,
        "income_wage" to R.string.cat_income_wage,
        "income_refunds" to R.string.cat_income_refunds,
        "income_interest" to R.string.cat_income_interest,
        "income_other" to R.string.cat_income_other,
    )

    @StringRes
    fun resFor(id: String): Int? = res[id]

    /** Lokalizovaný název pro přednastavenou kategorii, jinak uložený [fallback]. */
    fun display(context: Context, id: String, fallback: String): String =
        res[id]?.let { context.getString(it) } ?: fallback

    /** Vrátí kopii kategorie s lokalizovaným názvem (pro přednastavené). */
    fun localized(context: Context, category: CategoryEntity): CategoryEntity =
        res[category.id]?.let { category.copy(name = context.getString(it)) } ?: category
}
