# 03 — Data model

Local **Room** (SQLite) database `heller.db`, **schema version 8**, with five tables. There is no
server and no sync. User settings, Fio connections and learned categorization rules live in
**DataStore**, outside Room.

Code: `data/db/Entities.kt`, `Enums.kt`, `Converters.kt`, `Daos.kt`, `HellerDatabase.kt`, and
`di/DatabaseModule.kt` (migrations and seeding).

## Conventions
- **Money:** `amountMinor: Long` in minor units (×100). It is **always positive**, and the
  direction comes from the record `type` (and `transferOut` for transfers). Currency is not
  stored per row, because the app is single-currency. See
  [00-overview.md](00-overview.md#language-and-currency). Money is never `float`/`double`;
  arithmetic uses `BigDecimal`.
- **Time:** `dateTime` is epoch milliseconds, shown in the device time zone. Imported Fio
  records get 12:00 local time on their booking date. Date-only values, such as planned payment
  anchors, are **epoch days** (`Long`).
- **IDs:** random UUID strings. Preset categories use stable slugs instead (see
  [07-default-categories.md](07-default-categories.md)).
- **Reactive reads:** DAOs return `Flow<…>`. Totals and balances are computed in SQL
  (`SUM`, `GROUP BY`).

## Tables

### `accounts` — `AccountEntity`
| Field | Type | Note |
|---|---|---|
| id | String | PK |
| name | String | |
| type | `AccountType` | CASH, CHECKING, CREDIT_CARD, SAVINGS, INVESTMENT, OTHER |
| initialBalanceMinor | Long | Opening balance. For Fio accounts it is recomputed on every sync so the balance matches the bank |
| icon | String | Icon key |
| excludeFromStats | Boolean | Excludes the account from net worth. The UI has no toggle, so it is always `false` |
| archived | Boolean | Hides the account from lists and net worth. The UI has no toggle, so it is always `false` |
| isBusiness | Boolean | Business account: incoming payments count as income (added in v7) |
| sortOrder | Int | |
| createdAt / updatedAt | Long | Epoch ms |

The **current balance** isn't stored. It is computed as `initialBalanceMinor + Σ signed
records`:
- Income adds and expense subtracts.
- A transfer adds or subtracts according to `transferOut`.

### `categories` — `CategoryEntity`
| Field | Type | Note |
|---|---|---|
| id | String | PK. A slug for presets, a UUID for user categories |
| name | String | Stored name (Czech for presets; the UI shows the localized name) |
| type | `CategoryType` | INCOME, EXPENSE |
| parentId | String? | `null` for a group or top-level category, set for a subcategory |
| icon | String | Key into `CategoryIcons` |
| sortOrder | Int | Presets follow seed order, and user categories get 100,000 so they sort after presets |
| isDefault | Boolean | From the preset seed |

Indices: `parentId`, `type`.

### `records` — `RecordEntity`
| Field | Type | Note |
|---|---|---|
| id | String | PK |
| type | `RecordType` | EXPENSE, INCOME, TRANSFER |
| accountId | String | Owning account |
| categoryId | String? | `null` for transfers and uncategorized records |
| amountMinor | Long | Always positive |
| dateTime | Long | Epoch ms |
| payee | String? | Payee or payer (filled by the Fio import) |
| note | String? | |
| paymentType | `PaymentType`? | CASH, CARD, TRANSFER, OTHER. Not used by the current UI |
| transferAccountId | String? | Counter-account of a two-sided (manual) transfer |
| transferRecordId | String? | ID of the partner record of a two-sided transfer |
| transferOut | Boolean? | For transfers: `true` is outgoing (minus), `false` is incoming (plus). `null` otherwise |
| source | `RecordSource` | MANUAL, FIO, IMPORT |
| fioTransactionId | Long? | Fio transaction ID (dedup) |
| createdAt / updatedAt | Long | |

Indices: `accountId`, `categoryId`, `dateTime`, and **unique `(accountId, fioTransactionId)`**.
Fio gives both sides of an internal transfer the same ID, so the dedup key is scoped to the
account (changed in v8).

**Transfers come in two shapes:**
- **Two-sided (manual):** two records, one per account, linked by `transferRecordId` and
  `transferAccountId`, with opposite `transferOut`. They can only be deleted, and deleting one
  removes both.
- **One-sided:** a single record with `type = TRANSFER` and `transferOut` set, but no partner.
  The Fio import creates these for transfers between the user's own accounts. The user creates
  them by marking an expense or income as a transfer. The account balance stays correct, and the
  record counts as neither income nor expense.

### `planned_payments` — `PlannedPaymentEntity`
| Field | Type | Note |
|---|---|---|
| id, name | String | |
| type | `RecordType` | EXPENSE or INCOME (TRANSFER is not used) |
| accountId | String | |
| categoryId | String? | |
| amountMinor | Long | |
| frequencyUnit | `FrequencyUnit` | DAY, WEEK, MONTH, YEAR |
| frequencyCount | Int | Interval = every *count* units (e.g. MONTH × 6 = twice a year) |
| startEpochDay | Long | Anchor / first occurrence |
| endEpochDay | Long? | Optional end |
| note | String? | |
| paidThroughEpochDay | Long? | Last occurrence that is paid. The due occurrence is the first one after it (added in v6) |
| createdAt / updatedAt | Long | |

Occurrences are computed on the fly (`core/time/PlannedPayments`) and never stored. "Pay now"
and matching only move `paidThroughEpochDay` forward. There are no reminders and no automatic
record creation.

### `budgets` — `BudgetEntity`
| Field | Type | Note |
|---|---|---|
| id, name | String | |
| categoryGroupIds | List<String> | IDs of category groups, serialized by a converter. Empty means all expenses |
| amountMinor | Long | Limit |
| period | `BudgetPeriod` | WEEK, MONTH, YEAR |
| notifyThresholdPct | Int | Default 80. Stored, but not used by the current UI |
| createdAt / updatedAt | Long | |

Spending is computed from records in the groups' subcategories within the current period.

## Relationships

```
Account 1───* Record *───0..1 Category (self-referencing parentId → group)
Account 1───* PlannedPayment *───0..1 Category
Budget  *───* Category group (via categoryGroupIds)
Record  0..1───0..1 Record (transfer partner)
```

The relationships are logical only. There are no SQL foreign keys, and repositories keep them
consistent. For example, deleting a category group deletes its subcategories, and their records
become "No category".

## DataStore (outside Room)

| Store | Key | Content |
|---|---|---|
| `settings` | `onboarding_done` | Boolean |
| `settings` | `currency` | Currency code (default `CZK`) |
| `settings` | `fio_connections` | JSON array `[{token (encrypted), accountId, lastSync, fioAccountNumber}]` (see [04-fio-connector.md §7](04-fio-connector.md#7-token-storage)) |
| `categorization` | `rules_json` | Learned rules: JSON object `{merchantKey: categoryId}` |

Older installs stored a single connection under `fio_token`, `fio_account_id` and
`fio_last_sync`. On startup it is migrated into the list and the old keys are removed.

## Migrations and seeding
- **No destructive migrations.** Every schema change needs an explicit `Migration`, and without
  one Room fails rather than wipe user data.
  - v5 → v6: `planned_payments.paidThroughEpochDay`
  - v6 → v7: `accounts.isBusiness`
  - v7 → v8: the global unique index on `fioTransactionId` is replaced by a unique index on
    `(accountId, fioTransactionId)`
- **Category seed:** the full preset tree is inserted on `onCreate`. The later extras are
  inserted with `INSERT OR IGNORE` on every `onOpen`, so existing databases get them without a
  migration.
- `exportSchema = false`, so no schema JSON files are generated.

## Backup format
A backup file is the SQLite database plus a JSON metadata block (currency and Fio connections),
framed as `[metaLen (4 B)] ‖ meta ‖ db`. The whole payload is encrypted with the user's
password:

```
MAGIC "HELLERB1" (8) ‖ iterations (4, big-endian) ‖ salt (16) ‖ IV (12) ‖ ciphertext + GCM tag
```

- The key comes from PBKDF2-HMAC-SHA256 with 300,000 iterations, and the payload is encrypted
  with AES-256-GCM.
- Iterations and salt sit in the header, so they can change without breaking old backups.
- Older plain SQLite backups (header `SQLite format 3`) can still be restored without a
  password.
- Restore writes the database to a temporary file and then renames it over the live one, so the
  database is either fully old or fully new.
