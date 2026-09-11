package cz.heller.data.db

/**
 * Přednastavená sada kategorií (seed). Zakládá se při prvním vytvoření DB.
 * Struktura dle [docs/07-default-categories.md] (vzor Wallet) — 11 skupin + podkategorie.
 * Stabilní `id` (slug) — kvůli budoucím pravidlům automatické kategorizace Fio.
 * `icon` jsou klíče monochrom Material ikon (viz `CategoryIcons`). Skupina „income"
 * je jediná příjmová (`CategoryType.INCOME`), zbytek je výdajový.
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
            subs: List<Triple<String, String, String>>,
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
            Triple("restaurant", "Restaurace, fast-food", "fastfood"),
            Triple("bar", "Bar, kavárna", "local_cafe"),
        ))
        group("shopping", "Nákupy", "shopping_bag", subs = listOf(
            Triple("clothes", "Oděvy a obuv", "checkroom"),
            Triple("jewelry", "Klenoty, módní doplňky", "diamond"),
            Triple("health_beauty", "Zdraví, krása", "health_and_safety"),
            Triple("kids", "Děti", "child_care"),
            Triple("home_garden", "Domácnost, zahrada", "yard"),
            Triple("pets", "Mazlíčci, zvířata", "pets"),
            Triple("electronics", "Elektronika, příslušenství", "devices"),
            Triple("gifts", "Dárky, radosti", "card_giftcard"),
            Triple("office_tools", "Kancelář, nářadí", "handyman"),
            Triple("free_time", "Volný čas", "weekend"),
            Triple("drugstore", "Drogerie", "soap"),
        ))
        group("housing", "Bydlení", "home", subs = listOf(
            Triple("rent", "Nájemné", "vpn_key"),
            Triple("mortgage", "Hypotéka", "account_balance"),
            Triple("utilities", "Energie, komunál", "bolt"),
            Triple("services", "Služby", "room_service"),
            Triple("maintenance", "Údržba, opravy", "build"),
            Triple("property_insurance", "Pojištění majetku", "shield"),
        ))
        group("transport", "Doprava", "directions_bus", subs = listOf(
            Triple("public", "Veřejná doprava", "directions_bus"),
            Triple("taxi", "Taxi", "local_taxi"),
            Triple("long_distance", "Dálková doprava", "train"),
            Triple("business_trips", "Služební cesty", "business_center"),
        ))
        group("vehicle", "Vozidlo", "directions_car", subs = listOf(
            Triple("fuel", "Palivo", "local_gas_station"),
            Triple("parking", "Parkování", "local_parking"),
            Triple("maintenance", "Údržba vozidel", "car_repair"),
            Triple("rental", "Půjčování", "car_rental"),
            Triple("vehicle_insurance", "Pojištění vozidla", "verified_user"),
            Triple("leasing", "Leasing", "account_balance"),
        ))
        group("life", "Život, zábava", "celebration", subs = listOf(
            Triple("healthcare", "Zdravotní péče, lékař", "medical_services"),
            Triple("wellness", "Wellness, krása", "spa"),
            Triple("sport", "Aktivní sport, fitness", "fitness_center"),
            Triple("culture", "Kultura, sportovní akce", "theater_comedy"),
            Triple("life_events", "Životní události", "celebration"),
            Triple("hobbies", "Koníčky", "palette"),
            Triple("education", "Vzdělávání, osobní rozvoj", "school"),
            Triple("books", "Knihy, audio, předplatné", "menu_book"),
            Triple("tv", "Televize, streaming", "tv"),
            Triple("holidays", "Dovolená, výlety, hotely", "beach_access"),
            Triple("charity", "Charita, dary", "volunteer_activism"),
        ))
        group("comm", "Komunikace, PC", "smartphone", subs = listOf(
            Triple("phone", "Telefon, mobil", "smartphone"),
            Triple("internet", "Internet", "wifi"),
            Triple("software", "Software, aplikace, hry", "sports_esports"),
            Triple("postal", "Poštovní služby", "local_post_office"),
        ))
        group("financial", "Finanční výdaje", "account_balance", subs = listOf(
            Triple("taxes", "Daně", "receipt_long"),
            Triple("insurance", "Pojištění", "policy"),
            Triple("loans", "Půjčky, splátky", "request_quote"),
            Triple("fines", "Pokuty", "gavel"),
            Triple("advisory", "Poradenství", "support_agent"),
            Triple("fees", "Poplatky", "credit_card"),
            Triple("alimony", "Alimenty", "family_restroom"),
        ))
        group("investments", "Investice", "trending_up", subs = listOf(
            Triple("realty", "Nemovitosti", "apartment"),
            Triple("movables", "Vozidla, movitý majetek", "directions_car"),
            Triple("financial", "Finanční investice", "show_chart"),
            Triple("savings", "Spoření", "savings"),
            Triple("collections", "Sbírky", "collections"),
        ))

        // --- Příjmová skupina ---
        group("income", "Příjem", "payments", type = CategoryType.INCOME, subs = listOf(
            Triple("wage", "Plat, mzda, fakturace", "payments"),
            Triple("interest", "Úroky, dividendy", "trending_up"),
            Triple("sale", "Prodej", "sell"),
            Triple("rental", "Příjem z pronájmu", "home"),
            Triple("grants", "Příspěvky a granty", "volunteer_activism"),
            Triple("lending", "Příjem ze zapůjčení", "handshake"),
            Triple("coupons", "Šeky, kupóny, stravenky", "confirmation_number"),
            Triple("lottery", "Loterie, hazard", "casino"),
            Triple("refunds", "Refundace (daň, nákup)", "undo"),
            Triple("alimony", "Alimenty", "family_restroom"),
            Triple("gifts", "Dárky", "card_giftcard"),
        ))

        // --- Ostatní (výdaje) ---
        group("others", "Ostatní", "more_horiz", subs = listOf(
            Triple("missing", "Chybějící", "help_outline"),
        ))

        list += extras()
        return list
    }

    /**
     * Kategorie přidané po v5 seedu. Vkládají se i do existující DB (INSERT OR IGNORE
     * při otevření), takže nevyžadují destruktivní migraci / mazání dat.
     */
    fun extras(): List<CategoryEntity> = listOf(
        CategoryEntity(
            id = "work",
            name = "Práce / Podnikání",
            type = CategoryType.EXPENSE,
            parentId = null,
            icon = "business_center",
            sortOrder = 950,
            isDefault = true,
        ),
        CategoryEntity(
            id = "cash_withdrawal",
            name = "Výběr hotovosti",
            type = CategoryType.EXPENSE,
            parentId = null,
            icon = "local_atm",
            sortOrder = 951,
            isDefault = true,
        ),
    )
}
