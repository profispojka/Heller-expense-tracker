# Heller — calm personal finance tracker

<img src="assets/logo/heller-lockup.svg" alt="Heller" height="64">

Offline-first Android app for tracking income and expenses, built for **black-and-white E-Ink
phones** (primarily the **Mudita Kompakt**). Feature set inspired by
**Wallet by BudgetBakers** ([budgetbakers.com](https://budgetbakers.com/en/products/wallet/)),
extended with a read-only **Fio banka connector** for automatic transaction import.

**Current version: 1.4.0** · [Download APK](releases/app-release-1.4.0.apk) · older builds in [`releases/`](releases/)

## Highlights

- **Monochrome E-Ink design:** black ink on white paper, high contrast, no animations and no ripple. Charts use lines, fills and bars, never color ([docs/06](docs/06-design-eink.md)).
- **Offline-first:** all data stays on the phone (Room/SQLite). The only network access is the Fio import you opt into.
- **Languages:** English, Czech, German, Polish. The app follows the system language.
- **Currencies:** CZK, EUR, USD, GBP, CHF, PLN, SEK, NOK, DKK, HUF, RON, UAH, CAD, AUD, JPY, INR. You pick one during onboarding.

## Features

### Everyday tracking
- **Records:** expense, income and transfer, with date and time, note, and a hierarchical category picker. Records can be edited and deleted. A record imported or entered as income/expense can be *marked as a transfer between your own accounts*, which takes it out of statistics without changing the balance.
- **Records list:** grouped by day, with daily totals (transfers excluded) and an income/expense summary. You can filter by type, account and category.
- **Accounts:** multiple accounts with computed balances. An account can be flagged as a **business account**, so incoming payments count as income without categorizing each sender.
- **Categories:** 35 preset categories in 13 groups (31 expense, 4 income), and your own. Small on purpose: one category per thing you would budget or track separately. Each category has one of 72 monochrome icons ([docs/07](docs/07-default-categories.md)).

### Overview & insights
- **Dashboard:** net worth, balance trend, expense trend with % change against the previous period (30 days / 12 weeks / 6 months / 1 year), top expenses, upcoming payments and recent records. The income trend appears only when a business account is connected.
- **Statistics:** monthly Balance, Expenses, Income, Cash flow and **Forecast** cards, with a month switcher.
  - Expenses break down by category group and drill down to individual records.
  - Forecast lists next month's planned payments.
- **Budgets and analytics:** budgets per category group and period, with a monochrome spending meter and overspend warning. Analytics shows a monthly summary, spending by category and a 6-month cash flow.

### Planned payments
- Recurring payments with flexible frequency (weekly, monthly, quarterly, 2× or 3× a year, yearly, or custom), an optional end date, and **"Pay now"**, which creates the real record.
- **Payment matching:** after a Fio sync, a planned payment matches itself to the transaction with the same merchant, a similar amount and a date near the due date. You can also pick the matching transaction by hand. Overdue payments get a badge.
- **Recurring payment detection:** Fio doesn't expose standing orders, so Heller infers them from transaction history (same amount, monthly cadence). Suggestions you confirm become planned payments.

### Fio banka import
- Read-only connection using a Fio *API Bankovnictví* token ("account monitoring only"). The app never makes payments.
- Multiple Fio accounts, each with its own token. Sync runs when the app opens, plus a **daily background sync** through WorkManager.
- Deduplication by (account, transaction ID). Card payments get the merchant name extracted.
- **Transfers between your own connected accounts** are detected and kept out of statistics.
- The displayed balance is the **real bank balance** (Fio `closingBalance`). Without strong authorization, Fio returns only the last 90 days.
- **Automatic categorization:**
  - A layered offline engine: bank transaction type, what you assigned to the same counter-account or merchant before, a lexicon of ~3,000 brands (from the OpenStreetMap Name Suggestion Index) and generic CZ/SK/PL/DE/EN words, and a naive Bayes model trained on your confirmed records.
  - Auto-assigned categories are marked "auto" until you confirm them. Uncategorized records show up to three suggested categories as one-tap chips, and the records filter has an "Uncategorized" inbox.
  - Accuracy is tracked by a JVM evaluation harness (`./gradlew :app:testDebugUnitTest`).
- Fio tokens are encrypted with an AES-256-GCM key held in **Android Keystore**, which never leaves the device.

Details are in [docs/04](docs/04-fio-connector.md).

### Backup & restore
- Exports the whole database, including bank connections, to a file through the system file picker. No extra permissions are needed.
- **Password-protected:** AES-256-GCM with a key derived via PBKDF2-HMAC-SHA256 (300,000 iterations). You can restore on another phone or after reinstalling, but a forgotten password can't be recovered.
- A backup can be restored straight from onboarding, which skips account setup.

## Tech stack

| Area | Choice |
|---|---|
| Package | `cz.heller` |
| Platform | Android, native · minSdk 26 · target/compileSdk 35 |
| Language / UI | Kotlin 2.0 · Jetpack Compose (Material 3, recolored to monochrome) |
| Architecture | MVVM · Hilt · Navigation Compose · Kotlin Flow |
| Data | Room (SQLite) · DataStore · WorkManager (background Fio sync) |
| Networking | `HttpURLConnection` + `org.json` (no extra HTTP library) |
| Target device | **Mudita Kompakt** (E-Ink Android); works on any Android 8+ device |

## Building

You need **JDK 17** and the **Android SDK**. The easiest route is to open the project root in Android Studio.

```bash
./gradlew :app:assembleDebug     # debug APK
./gradlew :app:installDebug      # install on a connected device/emulator
./gradlew :app:assembleRelease   # minified release APK
```

Release signing reads `keystore.properties` from the project root. That file and the keystore are outside git. If the file is missing, the release build falls back to the debug key, so clones and CI still build.

## Project structure

```
app/src/main/java/cz/heller/
├── core/
│   ├── categorize/    # auto-categorization rule engine
│   ├── designsystem/  # monochrome theme, E-Ink components, charts
│   ├── fio/           # Fio API client, parser, sync manager + worker
│   ├── money/         # Money formatting, currencies
│   ├── navigation/    # bottom bar, NavHost (no animations)
│   ├── recurring/     # recurring-payment detection, planned-payment matching
│   ├── security/      # Keystore token crypto, password backup crypto
│   └── time/          # periods, planned-payment occurrences
├── data/              # Room DB, repositories, settings, backup
└── feature/           # screens: dashboard, records, addrecord, accounts,
                       # categories, budgets, analytics, planned, recurring,
                       # statistics, backup, onboarding, more
```

## Documentation

| File | Contents |
|---|---|
| [docs/00-overview.md](docs/00-overview.md) | What Heller is, principles, languages and currency, scope, comparison with Wallet |
| [docs/01-features.md](docs/01-features.md) | Complete feature set by area |
| [docs/02-screens.md](docs/02-screens.md) | Every screen, its content, actions and transitions |
| [docs/03-data-model.md](docs/03-data-model.md) | Room entities, DataStore keys, migrations, backup format |
| [docs/04-fio-connector.md](docs/04-fio-connector.md) | Fio API, sync triggers, parsing, import rules, token storage, recurring detection |
| [docs/05-architecture.md](docs/05-architecture.md) | Tech stack, layers, project structure, key decisions, building and releases |
| [docs/06-design-eink.md](docs/06-design-eink.md) | Visual style: black-and-white E-Ink (tokens, typography, components, charts) |
| [docs/07-default-categories.md](docs/07-default-categories.md) | Preset category tree, slugs and localization |

## Data attribution

Automatic categorization ships two derived datasets, both regenerated by scripts in `tools/lexicon/`:

- **Brand names** from the [OpenStreetMap Name Suggestion Index](https://github.com/osmlab/name-suggestion-index) (BSD-3-Clause).
- **Local business names** for Czechia, Slovakia and Poland from [Overture Maps Places](https://overturemaps.org) (CDLA-Permissive-2.0), which aggregates data from Meta, Microsoft, Foursquare (Apache-2.0) and OpenStreetMap contributors. The app stores only 64-bit hashes of normalized names and a category per hash, never the names themselves.

## Intentionally out of scope

Record search (filters cover it), theme switching (single paper/ink theme), labels (categories cover
it), payment reminders/notifications, outgoing bank payments.
