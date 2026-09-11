# 04 — Fio connector

Read-only import of transactions from **Fio banka** through its public REST API
("API Bankovnictví"). Heller only **downloads transactions**. It never sends payments.

> Official sources: [fio.cz/bank-services/internetbanking-api](https://www.fio.cz/bank-services/internetbanking-api)
> and the manual [API_Bankovnictvi.pdf](https://www.fio.cz/docs/cz/API_Bankovnictvi.pdf).

Code: `core/fio/` (client, parser, sync manager, worker, scheduler), `data/repo/FioRepository.kt`
(import logic), `core/security/TokenCrypto.kt` (token encryption), and
`feature/accounts/AccountFioSection.kt` (UI).

## 1. Token
- In **Fio internet banking → Settings → API → Add new token**, the user creates a token with
  the **"Account monitoring only"** permission. It is read-only, so no payment can be sent with it.
- A token belongs to **one bank account**. More Fio accounts need more tokens, and Heller
  supports any number of them.
- The connection is set up **in the account's edit screen** (More → Accounts → account →
  "Fio connection"), not in a separate menu. Each Fio token is linked to one Heller account.

## 2. Endpoint
Base URL: `https://fioapi.fio.cz/v1/rest/`

Heller uses a single endpoint:

| Purpose | Method | URL |
|---|---|---|
| Transactions for a period | GET | `periods/{token}/{from}/{to}/transactions.json` |

- Dates are formatted `YYYY-MM-DD`.
- **Every sync downloads the last 90 days.** Without strong authorization, Fio serves at most 90
  days of history. Because import is idempotent (see §6), overlapping windows are harmless.
  Heller therefore doesn't use the `last/` pointer or `set-last-*` endpoints.
- The HTTP client is plain `HttpURLConnection` with a 30 s connect timeout and a 90 s read
  timeout. There is no HTTP library.

## 3. When sync runs

| Trigger | Mechanism |
|---|---|
| Connecting an account | "Connect and sync" in the account screen |
| Every app start or return to the foreground | `FioSyncManager.syncNow()`, called from `HellerApp` on `ON_START` |
| Once a day in the background | `FioSyncWorker`, a WorkManager periodic job that requires network |

- The daily job is (re)scheduled with `ExistingPeriodicWorkPolicy.UPDATE` **on every app open**.
  Some OEMs kill scheduled work after a force-stop, and re-scheduling on open keeps the daily
  job alive.
- The foreground sync is single-flight. It skips connections synced less than 30 s ago.

## 4. Rate limit and errors
- Fio allows **1 request per token per 30 seconds**. A second request gets **HTTP 409**, which
  Heller reports as "rate limited". The next run simply tries again.

| Response | Result shown to the user |
|---|---|
| 200 | Import (§5) |
| 409 | "Fio allows only 1 request per 30 s. Try again in a moment." |
| 404 | Invalid token. The user should check the copy/paste. |
| 422 | Data older than 90 days needs the token unlocked in internet banking (Settings → API → lock icon → confirm), then a sync within 10 minutes |
| Other HTTP | "Fio rejected the request (HTTP n)" |
| Network / timeout | "Network error: …" |
| Invalid JSON | "Could not read data from Fio." |

## 5. Response parsing (`FioParser`)
Each transaction is an object of `columnN: { value, name, id }` entries. Missing or `null`
columns are tolerated. A transaction without an ID, amount or date is skipped.

| Fio column | Meaning | Used for |
|---|---|---|
| `column22` | Transaction ID | `fioTransactionId` (dedup) |
| `column0` | Date | `dateTime` (the date at 12:00 local time) |
| `column1` | Amount, signed, in major units | `amountMinor` (absolute value) and the direction |
| `column14` | Currency | Parsed but not used, since the app is single-currency |
| `column2` | Counter-account number | Payee fallback, own-transfer detection |
| `column10` | Counter-account name | `payee` (first choice) |
| `column5` | Variable symbol | Parsed |
| `column16` | Message for recipient | Description (first choice) |
| `column7` | User identification | Description (fallback) |
| `column25` | Comment | Description (second fallback) |
| `column8` | Transaction type | Categorization heuristics |
| `column9` | Performed by | Detecting the account owner |

From the statement header (`accountStatement.info`), Heller reads:
- `accountId`, the bank account number, used to detect transfers between your own connected
  accounts
- `closingBalance`, the real balance

## 6. Importing a transaction (`FioRepository.importJson`)
1. **Amount:** `column1` converted with `BigDecimal` → ×100 → `HALF_UP` → `Long`. A negative
   amount makes an **EXPENSE**, and zero or positive makes an **INCOME**. `amountMinor` is stored
   as an absolute value.
2. **Payee and note:**
   - The payee is the counter-account name. If that is missing, Heller extracts the
     **merchant** from the card-payment text: it strips a "Nákup:", "Platba kartou:", "Výběr:"
     or "Kredit:" prefix and keeps the text before the first comma. The last fallback is the
     counter-account number.
   - The note is the description (message → user identification → comment), unless it equals
     the payee.
3. **Categorization** with `Categorizer` (see [01-features.md §4](01-features.md#4-automatic-categorization)).
   A counterparty equal to the account owner makes the record a **TRANSFER**.
4. **Transfer between your own connected accounts:**
   - The counter-account number is normalized to digits only, and the bank code after `/` is
     dropped.
   - It is compared with the account numbers of the *other* connected Fio accounts. It matches
     on equality, or on a suffix match of at least 6 digits, which handles account prefixes.
   - A match makes the record a **TRANSFER** without a category, which keeps it out of
     statistics.
5. **Business account:** any incoming non-transfer transaction gets the category
   `income_wage` ("Salary, wages, invoicing").
6. **Insert with deduplication:** `INSERT OR IGNORE` against the unique index
   **(accountId, fioTransactionId)**.
   - The index is per account, not global, because Fio gives **both sides of an internal
     transfer the same transaction ID**. A global index would drop the second side.
7. **Balance:**
   - Heller sets the account's initial balance so that `initial + Σ(records) = closingBalance`.
   - The displayed balance therefore always equals the real bank balance, even though only 90
     days are imported.
8. **After a successful import:**
   - Previously uncategorized records are recategorized, which picks up new rules.
   - All planned payments are re-matched with transactions.

Fio records are marked `source = FIO`. In the UI:
- They can be edited (category, note, marking as a transfer).
- They can't be deleted, because they would come back on the next sync.
- Fio accounts don't accept manual records, which keeps amounts identical to internet banking.
- "Pay now" is hidden for planned payments on a Fio account.

## 7. Token storage
- Connections are stored in DataStore (`settings` → `fio_connections`) as a JSON array of
  `{token, accountId, lastSync, fioAccountNumber}`.
- The **token is encrypted** with `TokenCrypto`:
  - AES-256-GCM with a key in **AndroidKeyStore** (alias `heller_fio_token_key`)
  - The key is hardware-backed where available and can't be exported.
  - Stored format: `enc1:` + Base64(IV[12] ‖ ciphertext + GCM tag)
  - Legacy plaintext tokens are read as-is and encrypted on the next write.
  - A token that can't be decrypted, for example after the key is invalidated, drops its
    connection. The user then enters the token again.
- Tokens never go into Room or logs.
- **Password-protected backups** include the connections with decrypted tokens *inside the
  encrypted payload*. On restore they are encrypted again with the new device's Keystore key,
  so bank connections survive a reinstall.

## 8. Recurring payment detection (`core/recurring/RecurringDetector`)
The Fio token API doesn't list standing orders, so Heller infers them from the imported history.

1. It takes expenses of at least 50.00 (to skip coffee, public transport and similar small
   spending).
2. It groups them by normalized merchant key plus the **exact amount**.
3. A group qualifies when all of these hold:
   - It spans at least 2 distinct months. A 90-day window often shows a monthly order only
     twice.
   - It has no more occurrences than months + 1. Frequent same-amount spending isn't a standing
     order.
   - The median gap between occurrences is 22–40 days, a monthly cadence.
4. For each candidate:
   - The name comes from the latest occurrence.
   - The category is suggested by the categorizer.
   - The next start is the last occurrence plus 1 month.
5. Results are sorted by amount, descending. Confirmed candidates become monthly planned
   payments.

## 9. Limitations
- **Read-only.** The app can never send a payment.
- **History is limited to 90 days** unless the token is unlocked with strong authorization.
- **Standing orders** aren't exposed by the API. They are inferred (§8).
- **Other banks:** a PSD2 account-information connection, as Wallet uses, would need an AISP
  license from the Czech National Bank. Heller supports only Fio.
