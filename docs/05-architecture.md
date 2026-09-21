# 05 — Architecture and tech stack

## Stack

| Area | Choice |
|---|---|
| Language | Kotlin 2.0.21 (JVM target 17) |
| UI | Jetpack Compose (BOM 2024.12.01), Material 3 recolored to monochrome, `material-icons-extended` |
| Architecture | MVVM with unidirectional data flow; repositories as the single source of truth |
| DI | Hilt 2.52 (+ `hilt-navigation-compose`, `hilt-work`) |
| Database | Room 2.6.1 (KSP) |
| Settings | DataStore Preferences 1.1.1 |
| Async | Kotlin Coroutines 1.9 + Flow |
| Navigation | Navigation Compose 2.8.5 (string routes, no transitions) |
| Background work | WorkManager 2.9.1 (daily Fio sync) |
| Networking | `HttpURLConnection` + `org.json` (no extra HTTP/JSON library) |
| Crypto | JCA: AndroidKeyStore AES-GCM (tokens), PBKDF2 + AES-GCM (backups) |
| Charts | Custom Compose drawing (`TrendChart`, `BarMeter`); no chart library |
| Money | `Long` minor units + `BigDecimal` + `NumberFormat` |
| Build | AGP 8.7.3, Gradle wrapper, version catalog `gradle/libs.versions.toml` |
| SDK | minSdk 26 (Android 8.0), compileSdk / targetSdk 35 |

## Layers

```
UI (Compose)  →  ViewModel (StateFlow UiState)  →  Repository  →  Room DAO / DataStore / FioApiClient
      ▲ events               ▲ business logic            ▲ data
      └──────── state ───────┘
```

- **UI:** composables read state with `collectAsStateWithLifecycle()` and send events to the
  ViewModel. Each screen's ViewModel lives in the same file as the screen.
- **ViewModel (Hilt):** combines repository `Flow`s into one `UiState` (`stateIn`,
  `WhileSubscribed(5 s)`) and handles actions.
- **Repository (`data/repo`):** business rules, for example transfers, the Fio import,
  categorization and planned payment reconciliation.
- **Pure logic (`core/*`):** stateless objects that are easy to test in isolation:
  `Categorizer`, `RecurringDetector`, `PlannedMatcher`, `PlannedPayments`, `Periods`, `Money`,
  `FioParser`.
- **Data:** Room DAOs return `Flow`, so the UI redraws after a manual change or a background
  import without extra plumbing.

## Project structure (single `:app` module)

```
app/src/main/java/cz/heller/
├── MainActivity.kt, HellerApplication.kt
├── di/                 # Hilt modules (Room database, migrations, category seeding)
├── core/
│   ├── categorize/     # Categorizer, TxFeatures, Lexicon (+ SeedRules, GenericLexicon, generated BrandLexicon),
│   │                   # CategoryModel (identity memory), NaiveBayes, MerchantText (normalization)
│   ├── designsystem/   # theme (Color, Type, Shape, Theme), components, CategoryIcons
│   ├── fio/            # FioApiClient, FioParser, FioSyncManager, FioSyncWorker, FioSyncScheduler
│   ├── money/          # Money (format/parse), AppCurrency
│   ├── navigation/     # HellerApp (NavHost), routes, CalmBottomBar, RootViewModel
│   ├── recurring/      # RecurringDetector, PlannedMatcher
│   ├── security/       # TokenCrypto (Keystore), BackupCrypto (password)
│   └── time/           # Periods, PlannedPayments (occurrence math)
├── data/
│   ├── db/             # entities, enums, converters, DAOs, HellerDatabase, DefaultCategories, CategoryNames
│   ├── repo/           # Account/Category/Record/Budget/PlannedPayment, Fio, Categorization, Recurring
│   ├── settings/       # SettingsRepository (DataStore), CategoryRulesStore
│   └── backup/         # BackupManager
└── feature/
    ├── dashboard/  records/  addrecord/  statistics/  planned/
    ├── accounts/   categories/  budgets/  analytics/  recurring/
    └── backup/     onboarding/  more/
app/src/test/java/cz/heller/core/categorize/   # JVM unit tests + evaluation harness
app/src/test/resources/categorize/eval.tsv    # evaluation dataset (anonymized transactions)
tools/lexicon/generate-brands.mjs             # regenerates BrandLexicon.kt from the OSM Name Suggestion Index
tools/lexicon/generate-poi.mjs                # builds assets/poi_index.bin from Overture Maps Places (CZ/SK/PL)
tools/lexicon/fio-csv-to-eval.mjs             # converts a Fio CSV export into the evaluation format
tools/lexicon/backup-to-eval.mjs              # joins category labels from an app backup onto a Fio CSV export
```

## Key decisions
- **Offline-first, no backend.** All data is local, and the only network traffic is the Fio
  import.
- **Reactive end to end.** DAO `Flow` → ViewModel `StateFlow` → Compose.
- **Exact money.** Amounts are stored as positive `Long` minor units, and the direction comes
  from the record type. Parsing and formatting go through `BigDecimal`.
- **Single currency, display only.** `AppCurrency` only sets the symbol (`Money.applyCurrency` at
  startup). The number format is always space-grouped with a decimal comma (`cs-CZ` style),
  whatever the UI language.
- **Localization.** `values/` holds English, the default, and `values-cs/`, `values-de/` and
  `values-pl/` hold the translations. Preset category names are stored in the database in Czech,
  as seeded, and are shown through string resources keyed by their stable slug
  (`CategoryNames`). User-created categories show their stored name.
- **Security.** Fio tokens are encrypted with a Keystore key in DataStore, and backups are
  encrypted with a password.
- **No destructive migrations.** Every schema change needs an explicit `Migration`. Room crashes
  rather than wipe user data. See [03-data-model.md](03-data-model.md).
- **E-Ink.** A custom black-and-white theme, no ripple, no animations or navigation transitions,
  and flat bordered components. See [06-design-eink.md](06-design-eink.md).
- **Automatic sync instead of a refresh gesture.** Fio syncs when the app opens and daily in the
  background, so there is no pull-to-refresh and no sync button.

## Building

Requirements: **JDK 17** (point `JAVA_HOME` at it) and the **Android SDK** (`local.properties`
or `ANDROID_HOME`). Android Studio sets both up automatically when you open the project root.

```bash
./gradlew :app:assembleDebug      # debug APK
./gradlew :app:installDebug       # install on a connected device / emulator
./gradlew :app:assembleRelease    # minified + resource-shrunk release APK
```

- **Release signing** reads `keystore.properties` (`storeFile`, `storePassword`, `keyAlias`,
  `keyPassword`) from the project root. The keystore and the properties file must stay private.
  If the file is missing, the release build is signed with the debug key, so a fresh clone still
  builds.
- **Versioning** lives in `app/build.gradle.kts` (`versionCode` / `versionName`, currently
  10 / 1.4.0).
- **Brand lexicon** is generated, not hand-written. To refresh it from the current Name
  Suggestion Index run `node tools/lexicon/generate-brands.mjs` and commit the regenerated
  `BrandLexicon.kt`. The mapping from OSM shop types to Heller categories, the country filter
  and the blacklist of brand names that are ordinary words live in the script.
- **POI index** (`assets/poi_index.bin`) is generated from Overture Maps Places. Download the
  places for the Czech, Slovak and Polish bounding boxes with the `overturemaps` CLI
  (`overturemaps download --bbox=… -f geojsonseq --type=place`) and run
  `node tools/lexicon/generate-poi.mjs cz.geojsonl sk.geojsonl pl.geojsonl`. The mapping from
  the Overture taxonomy to Heller categories lives in the script. The index holds only FNV-1a
  hashes of normalized names, so names cannot be recovered from the APK.
- **Released APKs** are kept in [`releases/`](../releases/) as `app-release-<version>.apk`.

## Testing
The pure logic in `core/categorize` has JVM unit tests (JUnit 4, no Android, no emulator):

```bash
./gradlew :app:testDebugUnitTest
```

- `LexiconTest` and `CategorizerTest` pin the matching rules: whole-token matching, longest
  phrase wins, manual rules beat brands, memory beats the lexicon, Bayes only with text evidence.
- `EvalHarnessTest` replays `src/test/resources/categorize/eval.tsv` chronologically, confirming
  each record after it is seen, and prints accuracy overall and on first-seen merchants plus
  every miss. It fails below 90 % overall, 85 % on first-seen, or with more than two wrong
  assignments. Raise the thresholds as the lexicon improves. The sample is hand-written and
  therefore optimistic.
- `RealDataReportTest` runs on a **real, unlabeled** export placed at
  `src/test/resources/categorize/local/real.tsv` (the `local/` folder is gitignored). Convert a
  Fio internet banking CSV with `node tools/lexicon/fio-csv-to-eval.mjs pohyby.csv <that path>`.
  The test asserts nothing. It prints cold-start coverage by layer, the most frequent
  uncategorized merchants, and a random sample of assignments (one per merchant) for a manual
  precision check. Rows whose `expected` column is filled in are also replayed chronologically
  with each label confirmed after it is seen, which simulates real use of memory and Bayes.
  Labels come from the app's own database: `tools/lexicon/backup-to-eval.mjs` decrypts a
  password-protected backup in memory (the password is typed by the user), joins the
  categories onto the CSV by Fio transaction ID and never writes tokens anywhere.
  On the author's export of 3,337 transactions (2025–2026): the cold start categorizes about
  82 % (of which the POI index adds about 4 percentage points), 15 % have text but no match,
  and 3 % are transfers with no note. On the 665 rows with a user label, the simulated run gets
  98 % right overall and 96 % on first-seen merchants; memory alone is 411 right and 2 wrong,
  and both of those were inconsistent labels. Most remaining misses are payment gateways
  without a merchant name, transfers to people, and labels inherited from the old engine.

Everything else is verified by building and running on an emulator or device against real data
(for example a Fio import on real accounts and a backup/restore round trip).
