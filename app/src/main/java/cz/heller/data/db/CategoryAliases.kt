package cz.heller.data.db

/**
 * Převod sluggů přednastavených kategorií z verzí před 1.4 (82 kategorií podle Wallet) na
 * zjednodušený strom (13 skupin, 35 kategorií). Používá ho migrace DB v9 → v10, legacy naučená
 * pravidla z DataStoru a vyhodnocovací testy se staršími štítky. Slug, který v mapě není,
 * zůstává (aktuální sluggy a uživatelské UUID).
 */
object CategoryAliases {

    val LEGACY: Map<String, String> = mapOf(
        // Jídlo
        "food_restaurant" to "food_dining",
        "food_bar" to "food_dining",
        // Nákupy
        "shopping_jewelry" to "shopping_other",
        "shopping_health_beauty" to "shopping_drugstore",
        "shopping_kids" to "shopping_other",
        "shopping_home_garden" to "housing_home",
        "shopping_pets" to "shopping_other",
        "shopping_gifts" to "charity",
        "shopping_office_tools" to "shopping_other",
        "shopping_free_time" to "shopping_other",
        // Bydlení
        "housing_rent" to "housing_rent_mortgage",
        "housing_mortgage" to "housing_rent_mortgage",
        "housing_services" to "housing_utilities",
        "housing_maintenance" to "housing_home",
        "housing_property_insurance" to "financial_insurance",
        // Doprava a vozidlo
        "transport_long_distance" to "transport_public",
        "transport_business_trips" to "work_other",
        "vehicle" to "transport",
        "vehicle_fuel" to "transport_fuel",
        "vehicle_parking" to "transport_car_other",
        "vehicle_maintenance" to "transport_car_other",
        "vehicle_rental" to "transport_car_other",
        "vehicle_vehicle_insurance" to "transport_car_insurance",
        "vehicle_leasing" to "transport_car_other",
        // Život, zábava
        "life" to "leisure",
        "life_healthcare" to "health_care",
        "life_wellness" to "health_sport",
        "life_sport" to "health_sport",
        "life_culture" to "leisure_culture",
        "life_life_events" to "others",
        "life_hobbies" to "leisure_culture",
        "life_education" to "leisure_education",
        "life_books" to "leisure_culture",
        "life_tv" to "comm_apps",
        "life_holidays" to "leisure_holidays",
        "life_charity" to "charity",
        // Komunikace
        "comm_phone" to "comm_phone_internet",
        "comm_internet" to "comm_phone_internet",
        "comm_software" to "comm_apps",
        "comm_postal" to "shopping_other",
        // Finance
        "financial_fines" to "financial_fees",
        "financial_advisory" to "financial_fees",
        "financial_alimony" to "financial_fees",
        // Investice
        "investments_realty" to "investments",
        "investments_movables" to "investments",
        "investments_financial" to "investments",
        "investments_savings" to "investments",
        "investments_collections" to "investments",
        // Příjem
        "income_sale" to "income_other",
        "income_rental" to "income_other",
        "income_grants" to "income_other",
        "income_lending" to "income_other",
        "income_coupons" to "income_other",
        "income_lottery" to "income_other",
        "income_alimony" to "income_other",
        "income_gifts" to "income_other",
        // Ostatní, práce
        "others_missing" to "others",
        // Staré „Práce / Podnikání" byly z většiny vývojářské nástroje → Software a hosting.
        "work" to "work_tools",
    )

    fun toCurrent(id: String): String = LEGACY[id] ?: id
}
