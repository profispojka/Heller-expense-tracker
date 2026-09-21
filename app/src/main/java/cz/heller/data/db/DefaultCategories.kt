package cz.heller.data.db

/**
 * Přednastavená sada kategorií (seed): 13 skupin, 31 výdajových + 4 příjmové kategorie.
 * Struktura dle [docs/07-default-categories.md]. Zakládá se při prvním vytvoření DB a při
 * migraci v9 → v10 (starší strom podle Wallet se převádí přes [CategoryAliases]).
 *
 * Stabilní `id` (slug) `{skupina}_{podkategorie}` — slovník kategorizace na ně odkazuje.
 * `icon` jsou klíče monochrom Material ikon (viz `CategoryIcons`). Skupina „income" je jediná
 * příjmová (`CategoryType.INCOME`), zbytek je výdajový. Skupina bez podkategorií je zároveň
 * kategorie (investments, charity, cash_withdrawal, others).
 */
object DefaultCategories {

    fun all(): List<CategoryEntity> {
        val list = mutableListOf<CategoryEntity>()
        var order = 0

        // Skupina + podkategorie. `subs` = Triple(slug, název, ikona).
        fun group(
            id: String,
            name: String,
            icon: String,
            type: CategoryType = CategoryType.EXPENSE,
            subs: List<Triple<String, String, String>> = emptyList(),
        ) {
            list += CategoryEntity(
                id = id,
                name = name,
                type = type,
                parentId = null,
                icon = icon,
                sortOrder = order++,
                isDefault = true,
            )
            subs.forEach { (subId, subName, subIcon) ->
                list += CategoryEntity(
                    id = "${id}_$subId",
                    name = subName,
                    type = type,
                    parentId = id,
                    icon = subIcon,
                    sortOrder = order++,
                    isDefault = true,
                )
            }
        }

        // --- Výdajové skupiny ---
        group("food", "Jídlo a pití", "restaurant", subs = listOf(
            Triple("groceries", "Potraviny", "shopping_cart"),
            Triple("dining", "Restaurace a kavárny", "local_cafe"),
        ))
        group("transport", "Doprava", "directions_bus", subs = listOf(
            Triple("public", "Veřejná doprava", "directions_bus"),
            Triple("taxi", "Taxi", "local_taxi"),
            Triple("fuel", "Palivo", "local_gas_station"),
            Triple("car_insurance", "Pojištění auta", "verified_user"),
            Triple("car_other", "Auto ostatní", "car_repair"),
        ))
        group("housing", "Bydlení", "home", subs = listOf(
            Triple("rent_mortgage", "Hypotéka a nájem", "account_balance"),
            Triple("utilities", "Energie a služby", "bolt"),
            Triple("home", "Domácnost a údržba", "build"),
        ))
        group("shopping", "Nákupy", "shopping_bag", subs = listOf(
            Triple("electronics", "Elektronika", "devices"),
            Triple("clothes", "Oblečení a obuv", "checkroom"),
            Triple("drugstore", "Drogerie a kosmetika", "soap"),
            Triple("other", "Ostatní nákupy", "shopping_bag"),
        ))
        group("health", "Zdraví a sport", "health_and_safety", subs = listOf(
            Triple("care", "Zdraví a lékárna", "medical_services"),
            Triple("sport", "Sport a wellness", "fitness_center"),
        ))
        group("leisure", "Volný čas", "celebration", subs = listOf(
            Triple("culture", "Kultura a zábava", "theater_comedy"),
            Triple("holidays", "Dovolená a cestování", "beach_access"),
            Triple("education", "Vzdělávání", "school"),
        ))
        group("comm", "Předplatné a komunikace", "smartphone", subs = listOf(
            Triple("phone_internet", "Telefon a internet", "wifi"),
            Triple("apps", "Aplikace a předplatné", "sports_esports"),
        ))
        group("work", "Podnikání", "business_center", subs = listOf(
            Triple("tools", "Software a hosting", "devices"),
            Triple("other", "Ostatní podnikání", "handshake"),
        ))
        group("financial", "Finance", "account_balance", subs = listOf(
            Triple("insurance", "Pojištění", "policy"),
            Triple("taxes", "Daně a odvody", "receipt_long"),
            Triple("loans", "Půjčky a splátky", "request_quote"),
            Triple("fees", "Poplatky a pokuty", "credit_card"),
        ))
        group("investments", "Investice a spoření", "trending_up")
        group("charity", "Dary a charita", "volunteer_activism")
        group("cash_withdrawal", "Výběr hotovosti", "local_atm")
        group("others", "Ostatní", "more_horiz")

        // --- Příjmová skupina ---
        group("income", "Příjem", "payments", type = CategoryType.INCOME, subs = listOf(
            Triple("wage", "Mzda a fakturace", "payments"),
            Triple("refunds", "Refundace a vratky", "undo"),
            Triple("interest", "Úroky a dividendy", "trending_up"),
            Triple("other", "Ostatní příjem", "sell"),
        ))

        return list
    }

    /** Kategorie přidávané do existující DB při otevření (žádné — vše řeší seed a migrace). */
    fun extras(): List<CategoryEntity> = emptyList()

    /** ID skupin (kategorií bez rodiče) pro přemapování rozpočtů při migraci. */
    fun groupIds(): Set<String> = all().filter { it.parentId == null }.map { it.id }.toSet()
}
