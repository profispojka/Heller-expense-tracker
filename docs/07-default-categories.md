# 07 — Default categories

Heller ships with a small preset category tree: **13 groups, 31 expense categories and 4 income
categories** (35 rows in total, since four groups have no subcategories and are used directly).
It is seeded when the database is first created. Icons are monochrome (E-Ink), see
`core/designsystem/CategoryIcons.kt`.

Versions before 1.4 used the 82-category taxonomy of Wallet by BudgetBakers. Real usage showed
that fewer than half of those were ever used and that most categorization mistakes fell on
boundaries nobody cares about (bar vs. restaurant, public vs. long-distance transport, gift vs.
donation), so the tree was collapsed. The old slugs are mapped to the new ones by
`data/db/CategoryAliases.kt`, and the v9 → v10 migration rewrites records, planned payments,
budgets and the parents of user-created categories accordingly.

Implementation:
- `data/db/DefaultCategories.kt` holds the tree, with stable slug IDs and icon keys.
- `data/db/CategoryNames.kt` maps each slug to a localized string resource.
- `data/db/CategoryAliases.kt` maps legacy slugs to current ones.

## Rules
- **IDs are stable slugs.** A group uses `food`, and a subcategory uses `{group}_{sub}`, for
  example `food_groceries`. The lexicon and the categorizer refer to these IDs.
- **A group without subcategories is itself a category** (`investments`, `charity`,
  `cash_withdrawal`, `others`). Groups with subcategories can also be picked directly.
- **Types.** The **Income** group and its subcategories are `INCOME`. All other categories are
  `EXPENSE`.
- **Names are localized.** The database stores the Czech seed name, and the UI shows the string
  resource for the slug in English, Czech, German or Polish. User-created categories have random
  UUID IDs and show their stored name.
- **Seeding** uses `INSERT OR IGNORE`, so it never overwrites existing rows. The full tree goes
  in on `onCreate` and in the v9 → v10 migration.

## Expense categories

| Group (slug) | Categories (slug) |
|---|---|
| **Food & drinks** (`food`) | Groceries (`food_groceries`) · Restaurants & cafés (`food_dining`) |
| **Transport** (`transport`) | Public transport (`transport_public`, incl. trains, buses, flights) · Taxi (`transport_taxi`) · Fuel (`transport_fuel`) · Car insurance (`transport_car_insurance`) · Car: other (`transport_car_other`, service, parking, rental, leasing, tolls) |
| **Housing** (`housing`) | Rent & mortgage (`housing_rent_mortgage`) · Utilities & services (`housing_utilities`) · Home & maintenance (`housing_home`, repairs, furniture, garden) |
| **Shopping** (`shopping`) | Electronics (`shopping_electronics`) · Clothing & footwear (`shopping_clothes`) · Drugstore & cosmetics (`shopping_drugstore`) · Other shopping (`shopping_other`, incl. postal services, kids, pets, office) |
| **Health & sport** (`health`) | Health & pharmacy (`health_care`) · Sport & wellness (`health_sport`) |
| **Leisure** (`leisure`) | Culture & entertainment (`leisure_culture`, incl. books, hobbies) · Holidays & travel (`leisure_holidays`) · Education (`leisure_education`) |
| **Subscriptions & communication** (`comm`) | Phone & internet (`comm_phone_internet`) · Apps & subscriptions (`comm_apps`, personal software, games, streaming) |
| **Business** (`work`) | Software & hosting (`work_tools`, Vercel, Supabase, cloud, AI tools, invoicing) · Other business (`work_other`) |
| **Finance** (`financial`) | Insurance (`financial_insurance`, life, property, travel) · Taxes & levies (`financial_taxes`, incl. health and social insurance, TV licence) · Loans & instalments (`financial_loans`) · Fees & fines (`financial_fees`) |
| **Investments & savings** (`investments`) | *(no subcategories)* |
| **Gifts & charity** (`charity`) | *(no subcategories; gifts to people and donations)* |
| **Cash withdrawal** (`cash_withdrawal`) | *(no subcategories)* |
| **Other** (`others`) | *(no subcategories)* |

## Income categories

| Group (slug) | Categories (slug) |
|---|---|
| **Income** (`income`) | Salary & invoicing (`income_wage`) · Refunds (`income_refunds`) · Interest & dividends (`income_interest`) · Other income (`income_other`) |

## Categories used by automatic logic
- `income_wage` — all incoming payments on a business account.
- `income_refunds` — card refunds ("Kredit"), and income from a merchant known to the lexicon.
  Income from an institution (a `financial_*`, `housing_*` or `investments` match) keeps that
  category instead.
- `financial_fees` — Fio transactions whose type contains "poplatek".
- `cash_withdrawal` — Fio transactions whose type contains "bankomat".
- `comm_phone_internet` — Fio transactions whose type contains "dobití".
- Anything else comes from the user's confirmed history, the lexicon (`SeedRules`,
  `BrandLexicon`, `GenericLexicon`) or the on-device Bayes model. The engine validates every
  result against the categories that exist, so a deleted category is never assigned. See
  [01-features.md §4](01-features.md#4-automatic-categorization).

## Legacy mapping (v9 → v10)

| Old | New |
|---|---|
| `food_restaurant`, `food_bar` | `food_dining` |
| `transport_long_distance` | `transport_public` |
| `vehicle_fuel` | `transport_fuel` |
| `vehicle_vehicle_insurance` | `transport_car_insurance` |
| `vehicle_parking`, `vehicle_maintenance`, `vehicle_rental`, `vehicle_leasing` | `transport_car_other` |
| `housing_rent`, `housing_mortgage` | `housing_rent_mortgage` |
| `housing_services` | `housing_utilities` |
| `housing_maintenance`, `shopping_home_garden` | `housing_home` |
| `housing_property_insurance` | `financial_insurance` |
| `shopping_health_beauty` | `shopping_drugstore` |
| `shopping_jewelry`, `shopping_kids`, `shopping_pets`, `shopping_office_tools`, `shopping_free_time`, `comm_postal` | `shopping_other` |
| `shopping_gifts`, `life_charity` | `charity` |
| `life_healthcare` | `health_care` |
| `life_sport`, `life_wellness` | `health_sport` |
| `life_culture`, `life_hobbies`, `life_books` | `leisure_culture` |
| `life_holidays` | `leisure_holidays` |
| `life_education` | `leisure_education` |
| `life_tv`, `comm_software` | `comm_apps` |
| `comm_phone`, `comm_internet` | `comm_phone_internet` |
| `work` | `work_tools` |
| `transport_business_trips` | `work_other` |
| `financial_fines`, `financial_advisory`, `financial_alimony` | `financial_fees` |
| `investments_*` | `investments` |
| `income_sale`, `income_rental`, `income_grants`, `income_lending`, `income_coupons`, `income_lottery`, `income_alimony`, `income_gifts` | `income_other` |
| `life_life_events`, `others_missing` | `others` |
