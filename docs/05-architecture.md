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
│   ├── categorize/     # Categorizer, SeedRules, MerchantText (normalization)
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
  8 / 1.2.3).
- **Released APKs** are kept in [`releases/`](../releases/) as `app-release-<version>.apk`.

## Testing
There is no automated test suite, and the module has only the `main` source set. Changes are
verified by building and running on an emulator or device against real data (for example a Fio
import on real accounts and a backup/restore round trip). The pure logic in `core/*` is written
so it can be unit-tested without Android.
