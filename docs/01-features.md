# 01 — Features

The complete feature set of Heller, grouped by area. Screens are described in
[02-screens.md](02-screens.md).

## 1. Accounts
- Any number of accounts. The types are cash, checking account, credit card, savings,
  investments and other.
- Each account has a name, type and initial balance.
- The current balance is computed as the initial balance plus or minus all records, transfers
  included.
- **Net worth** is the sum of all account balances.
- A **business account** treats every incoming payment (except transfers) as income in
  "Salary, wages, invoicing", no matter who sent it.
- A **Fio-connected account** fills only from the bank. It doesn't accept manual records, which
  keeps its amounts matching online banking. Its balance is the real bank balance (see
  [04-fio-connector.md](04-fio-connector.md)).

## 2. Records
- The types are **expense, income and transfer**.
- A record has an amount, an account, a category (not for transfers), date and time, and a note.
  Imported records also carry the payee from the bank.
- A **manual transfer** between two accounts is stored as two linked records, one per account.
  Deleting it removes both sides.
- **Marking as a transfer.** An existing expense or income can be changed to a transfer between
  your own accounts. This one-sided transfer drops out of expenses and income, and the account
  balance stays the same. It can be edited back later.
- **Editing and deleting.** Manual records can be edited and deleted. Fio records can be edited
  (category, note, type) but not deleted, because the next sync would import them again.
- The **records list** is grouped by day with daily totals, which exclude transfers. It has an
  income/expense summary and a filter by type, account and category group.
- **Applying a category to similar records.** When a record gets a category, Heller offers to
  apply the same category to other uncategorized payments from the same payee.

## 3. Categories
- There are **35 preset categories**: 13 groups with 31 expense and 4 income categories. Only
  the "Income" group is an income type; the rest are expense categories. The tree is
  deliberately small: a category exists only where a separate number in statistics or a budget
  makes sense. See [07-default-categories.md](07-default-categories.md).
- Users can add, edit and delete their own categories. Each has a name, a type (expense or
  income), an optional parent category and an icon from a grid of 72 monochrome icons.
- Deleting a group also deletes its subcategories. Their records stay and become "No category".
- Categories are picked on a separate hierarchical picker screen, not in a crowded grid.

## 4. Automatic categorization
- A pure, offline pipeline in `core/categorize/` runs on every Fio import and on every
  uncategorized record. Layers apply in this order, and the first confident one wins:
  1. **Own transfer**: the counterparty is the account owner (the most frequent "performed by"
     name). The record becomes a transfer.
  2. **Structural rules** from the bank transaction type: "Výběr z bankomatu" → Cash withdrawal,
     "Poplatek" → Fees, income whose text contains "kredit" → Refunds.
  3. **Identity memory** (`CategoryModel`): what the user themselves assigned to the same
     counter-account + variable symbol, the same counter-account, or the same normalized
     merchant. Built from **confirmed** records in the database, so it is backed up with the
     data and un-learns when the user changes a category. A counter-account that has received
     payments of mixed categories is not trusted on its own when the variable symbol is new.
  4. **Lexicon** (`Lexicon`): whole-token phrase matching, longest phrase first, with three
     sources by priority: manual rules (`SeedRules`), ~3,000 brands generated from the
     OpenStreetMap Name Suggestion Index (`BrandLexicon`, see `tools/lexicon/`), and generic
     words in Czech, Slovak, Polish, German and English (`GenericLexicon`, e.g. "pizz*",
     "lekarn*", "hotel*"). A longer phrase in the note beats a shorter one in the payee. Income
     from a known merchant without an income rule is a refund.
  5. **Local businesses** (`PoiIndex`): about 670,000 named places in Czechia, Slovakia and
     Poland from Overture Maps Places, shipped as 64-bit hashes of normalized names plus a
     category (`assets/poi_index.bin`, ~6 MB; see `tools/lexicon/generate-poi.mjs`). The
     longest sub-phrase of the payee found in the index wins. It is used when the lexicon has
     no match, or when the lexicon matched only a single generic word and the place matches on
     two or more tokens ("Sport Bar Beskyd" is a pub, not sport).
  6. **Naive Bayes** (`NaiveBayes`): trained on-device from confirmed records over payee and
     note words, transaction type and amount magnitude. It only decides when it has text
     evidence and a posterior of at least 0.7; otherwise it only contributes suggestions.
- A category assigned by the engine is marked **auto** (`categoryAuto`) and shown with an "auto"
  label. The user confirms it in the record detail or changes it. Only confirmed records train
  the memory and the Bayes model.
- An uncategorized record shows up to three **suggested categories** as chips, in the records
  list and in the detail. One tap assigns the category as confirmed and immediately re-runs the
  engine, so the same merchant's other records follow. The records filter has an
  "Uncategorized" chip that works as an inbox.
- After each successful sync, after each user assignment and on every app open, uncategorized
  records are re-run through the engine.
- Accuracy is measured by `EvalHarnessTest` on `app/src/test/resources/categorize/eval.tsv`,
  overall and on first-seen merchants. Drop an anonymized export of real data there to tune the
  lexicon against reality.

## 5. Dashboard
- Net worth and the list of accounts
- **Balance trend** and **spending trend** charts with a % change against the previous period of
  the same length. The periods are 30 days, 12 weeks, 6 months and 1 year.
- An **income trend** chart, shown only when a business account is connected to Fio
- Top expenses this month
- Upcoming payments: the 5 nearest planned payment occurrences, with an "Overdue" badge
- Recent records

## 6. Statistics
- Cards for the selected month: **Balance, Expenses, Income, Cash flow, Forecast**. A month
  switcher sits at the bottom.
- **Expenses** break down by category group (%, amount, bar). Tapping a group expands its records.
- **Income** shows the list of individual income records for the month.
- **Forecast** shows next month's expenses from planned payments. It counts only occurrences that
  actually fall into that month.

## 7. Budgets and analytics
- A budget has a name, a limit, a period (week, month or year) and a set of category groups.
  Empty groups means all expenses.
- Spending counts records in the group's subcategories within the current period.
- The status shows as a monochrome meter (bar fill) plus "Remaining X" or "⚠ Over by X".
- The **Analytics** tab has a monthly summary (income, expenses, net), expenses by category, and
  a 6-month cash flow.

## 8. Planned payments
- Recurring income or expenses have a name, type, account, category, amount, frequency, start
  date, optional end date and a note.
- Frequency is a unit (day, week, month or year) times a count. The presets are weekly, monthly,
  quarterly, 3×/year, 2×/year and yearly.
- The **"Next month"** summary (you'll pay / you'll receive) counts only occurrences that fall
  into next month.
- **Pay now** creates the real record. It is hidden for payments on Fio accounts, whose records
  come from the bank.
- **Payment matching.** Each planned payment remembers the occurrence it has been paid through
  (`paidThroughEpochDay`). Past-due occurrences get an "Overdue" badge.
  - **Automatic matching** runs after every Fio sync and when the overview opens. A transaction
    matches a due occurrence when all of these hold:
    - The type is the same.
    - The **normalized merchant** equals the payment name.
    - The amount is within ±3 % of the planned amount, with a minimum tolerance of 5.00.
    - The date is between 8 days before and 12 days after the due day.

    Occurrences more than 3 days in the future are never matched. Matching catches up several
    occurrences at once.
  - **Manual matching:** the user picks the transaction that paid the occurrence.
- **Recurring payment detection** infers standing orders and subscriptions from the imported
  history, because Fio's API doesn't expose them. Confirmed suggestions become monthly planned
  payments. The algorithm is in [04-fio-connector.md](04-fio-connector.md).

## 9. Fio banka import
- Read-only connection with a Fio *API Bankovnictví* token ("account monitoring only").
- Multiple Fio accounts, each with its own token and linked to one Heller account.
- Sync runs when the account is connected, on every app start or return to the foreground, and
  once a day in the background (WorkManager).
- Deduplication, merchant extraction, detection of transfers between your own accounts, business
  income, and the real bank balance.
- Full details are in [04-fio-connector.md](04-fio-connector.md).

## 10. Backup and restore
- **Back up:** the whole SQLite database plus metadata (currency and Fio connections, including
  tokens) goes into one file, saved through the system file picker (SAF, no extra permissions).
- The file is **encrypted with a user password**:
  - The key comes from PBKDF2-HMAC-SHA256 with 300,000 iterations and a random salt, and the
    content is encrypted with AES-256-GCM.
  - The password must be long enough and can't be digits only.
  - A forgotten password can't be recovered.
- **Restore** detects the file type. An encrypted backup asks for the password. An older
  unencrypted SQLite backup can still be restored, but it has no tokens or settings. Anything
  else is rejected. Restore writes the database atomically and restarts the app.
- A backup can be restored directly from onboarding, which skips account setup. Because the Fio
  tokens are included, bank connections come back after a reinstall or on a new phone.

## 11. Onboarding
- On first launch, when there are no accounts yet, the user either chooses a currency and creates
  the first account (name, type, initial balance), or restores from a backup.

## 12. Security
- Fio tokens are encrypted with an AES-256-GCM key in **Android Keystore**. The key is
  hardware-backed where the device supports it and can never be exported.
- Backups are encrypted with a password-derived key, so they are portable between devices.
- The app requests only the `INTERNET` permission, which it uses for Fio.
