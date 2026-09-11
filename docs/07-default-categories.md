# 07 — Default categories

Heller ships with a preset category tree modeled on the taxonomy of Wallet by BudgetBakers. It is
seeded when the database is first created. Colors from Wallet are dropped (monochrome E-Ink), and
each category gets a monochrome icon instead.

Implementation:
- `data/db/DefaultCategories.kt` holds the tree, with stable slug IDs and icon keys.
- `data/db/CategoryNames.kt` maps each slug to a localized string resource.
- `core/designsystem/CategoryIcons.kt` defines the icons.

## Rules
- **IDs are stable slugs.** A group uses `food`, and a subcategory uses `{group}_{sub}`, for
  example `food_groceries`. Categorization rules and the seed dictionary refer to these IDs.
- **Types.** The **Income** group and its subcategories are `INCOME`. All other categories are
  `EXPENSE`.
- **Names are localized.** The database stores the Czech seed name, and the UI shows the string
  resource for the slug in English, Czech, German or Polish. User-created categories have random
  UUID IDs and show their stored name.
- **Seeding** uses `INSERT OR IGNORE`, so it never overwrites existing rows:
  - The full tree goes in on `onCreate`.
  - Categories added later, the "extras", are inserted on every database open, so existing
    installs get them without a migration.

## Totals
- **11 groups + 69 subcategories = 80** from the original seed
- **2 later top-level additions**: *Work / Business* and *Cash withdrawal*
- **82 preset categories** in total

## Expense categories

| Group (slug) | Subcategories |
|---|---|
| **Food & drinks** (`food`) | Groceries · Restaurants, fast food · Bar, café |
| **Shopping** (`shopping`) | Clothing & footwear · Jewelry, accessories · Health, beauty · Kids · Home, garden · Pets, animals · Electronics, accessories · Gifts, treats · Office, tools · Free time · Drugstore |
| **Housing** (`housing`) | Rent · Mortgage · Utilities · Services · Maintenance, repairs · Property insurance |
| **Transport** (`transport`) | Public transport · Taxi · Long-distance travel · Business trips |
| **Vehicle** (`vehicle`) | Fuel · Parking · Vehicle maintenance · Rental · Vehicle insurance · Leasing |
| **Life, entertainment** (`life`) | Healthcare, doctor · Wellness, beauty · Active sport, fitness · Culture, sports events · Life events · Hobbies · Education, personal growth · Books, audio, subscriptions · TV, streaming · Holidays, trips, hotels · Charity, donations |
| **Communication, PC** (`comm`) | Phone, mobile · Internet · Software, apps, games · Postal services |
| **Financial expenses** (`financial`) | Taxes · Insurance · Loans, instalments · Fines · Advisory · Fees · Alimony |
| **Investments** (`investments`) | Real estate · Vehicles, movable property · Financial investments · Savings · Collections |
| **Other** (`others`) | Missing |
| **Work / Business** (`work`) | *(no subcategories; added later)* |
| **Cash withdrawal** (`cash_withdrawal`) | *(no subcategories; added later)* |

## Income categories

| Group (slug) | Subcategories |
|---|---|
| **Income** (`income`) | Salary, wages, invoicing · Interest, dividends · Sale · Rental income · Benefits and grants · Income from lending · Cheques, coupons, meal vouchers · Lottery, gambling · Refunds (tax, purchase) · Alimony · Gifts |

## Categories used by automatic logic
- `income_wage` (**Salary, wages, invoicing**) — all incoming payments on a business account.
- `income_refunds` (**Refunds**) — card refunds, when the income text contains "kredit".
- `financial_fees` (**Fees**) — Fio transactions whose type contains "poplatek" (fee).
- Anything else comes from learned rules or the seed merchant dictionary (`SeedRules`).
