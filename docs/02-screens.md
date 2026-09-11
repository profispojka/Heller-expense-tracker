# 02 — Screens

Every screen: its purpose, content, actions and transitions. There are no animations and
navigation switches instantly, as required by E-Ink (see [06-design-eink.md](06-design-eink.md)).

## Navigation skeleton

```
┌─────────────────────────────────────────────────────────────┐
│ [ Overview ] [ Payments ] [ Statistics ] [ Records ] [ More ] │
└─────────────────────────────────────────────────────────────┘
```

- **Bottom bar** with 5 tabs. The selected tab is **underlined** (a 4 dp bar with a bold label),
  not a dark bubble. The bar shows only on top-level screens.
- **FAB (+)** appears only on **Overview** (new record) and **Payments** (new planned payment).
- Detail and edit screens open full-screen and have a back or close button in the top bar.
- **More** contains Accounts, Categories, Budgets and Data backup.

---

## A. Onboarding

### A1. Onboarding
- **Shown when** the database has no account yet.
- **Content:**
  - A welcome title
  - **Currency** chips (16 currencies)
  - The first account's form: name, type, initial balance
  - A **Start** button
- **Restore from backup** opens the system file picker.
  - An encrypted backup asks for its password, and a wrong password keeps the dialog open.
  - An old unencrypted backup asks for confirmation.
  - An invalid file shows an error.
  - After a successful restore the app restarts straight into the main screen.

---

## B. Overview (Dashboard)

### B1. Overview
- **Net worth** — the sum of all account balances.
- **Accounts** — each account's name, type and balance.
- **Trend cards** — balance trend and spending trend. Each is a line chart with a gray fill, the
  period total and the % change against the previous period, with a period switcher (30 days /
  12 weeks / 6 months / 1 year). The **income trend** appears only with a Fio-connected business
  account.
- **Top expenses this month** — the largest categories.
- **Upcoming payments** — the 5 nearest planned payment occurrences with date and amount, plus an
  "Overdue" badge. Tapping one opens **G4 (match payment)**.
- **Recent records** — tapping one opens C2.
- **FAB (+)** opens C3.
- There is no sync button and no pull-to-refresh. Fio syncs automatically when the app opens.

---

## C. Records

### C1. Records list
- **Summary:** income and expenses for the current filter.
- **Filter:** type, account and category group, plus a clear-filter action.
- **List:** grouped by day. Each group header has the date and the day's total, which excludes
  transfers.
- **Row:** category icon, category or payee name, account, and the amount with its sign
  (**+** income in bold, **−** expense).
- **Empty states:** "No records" when there are none, and "Nothing matches the filter" when the
  filter hides everything.
- **Tap** opens C2.

### C2. Record detail
- **Content:** a large amount, type, account (or from/to accounts for a two-sided transfer),
  category, date, payee and note.
- **Edit** opens C3. It is available for everything except two-sided transfers, which can only be
  deleted.
- **Delete** asks for confirmation, and a transfer is deleted with both sides. It is available
  only for manual records. Fio records would come back on the next sync.

### C3. New / edit record
- **Type switch:** **Expense | Income | Transfer**. The active chip is inverted (black
  background, white text).
- **Amount:** a plain numeric field that opens the system number keyboard. There is no custom
  keypad.
- **Expense and income** fields:
  - **Account** — a row that opens a dialog. For new records, Fio-connected accounts are left out.
  - **Category** — a row that opens C4.
  - **Date and time** — Material date picker, then time picker. Defaults to now.
  - **Note**
- **Transfer (new):** **From account** and **To account**.
- **Transfer (editing an existing record):** only the account plus a hint. The record gets
  marked as a transfer between your own accounts, without a counter-account.
- **Save.** If the category changed and the same payee has other uncategorized payments, a sheet
  offers **"Apply to all"** or **"Just this one"**.
- If every account is Fio-connected, a new record can't be added. A hint explains why and
  suggests creating, for example, a cash account.

### C4. Category picker
- A hierarchical list of groups and subcategories for the given type (expense or income).
  Tapping one returns it to the form.
- The same picker is used by the planned payment form (G3).

---

## D. Accounts (More → Accounts)

### D1. Accounts list
- **Content:** accounts with type and balance, a delete icon that asks for confirmation, and
  **+ Add account**.
- **Tap** opens D2.

### D2. New / edit account
- **Form:** name, type (chips), initial balance, and the **Business account** switch with a hint.
- **Fio connection** section, shown when editing:
  - **Not connected:** a short guide to getting a token, the **Fio token** field and
    **Connect and sync**. After the first sync, if recurring payments are found, a prompt
    offers **"Yes, add"**, **"Rather select manually"** (opens D3) or **No**.
  - **Connected:**
    - Last sync time and a note that sync runs daily in the background
    - **Find recurring payments (standing orders)**, which opens D3
    - **Disconnect from Fio**, after a confirmation. Imported records stay.

### D3. Recurring payments
- **Content:** detected recurring payment candidates (name, amount, "monthly · next date").
  Each has a checkbox.
- **Add selected (n)** creates monthly planned payments.

---

## E. Categories (More → Categories)

### E1. Categories
- An **Expenses / Income** switch, and a hierarchy of groups with their subcategories and icons.
- **+ New category**, and tap to edit. Deleting asks for confirmation and explains what happens
  to subcategories and records.

### E2. New / edit category
- Name, parent category (optional, or "None (top level)"), and an **icon** grid.

---

## F. Budgets and analytics (More → Budgets)

### F1. Budgets
- A **Budgets | Analytics** switch at the top.
- **Budgets:** cards with name, a meter (bar fill) and "Remaining X" or "⚠ Over by X".
  **+ New budget**, and delete with confirmation.
- **Analytics:**
  - This month: income, expenses, net
  - Expenses by category, as a bar ranking with %
  - Cash flow over 6 months. For each month, the top bar is income and the bottom bar is
    expenses.

### F2. New budget
- Name, limit, period (Week / Month / Year) and category groups. Empty groups means all
  expenses.

---

## G. Planned payments (Payments tab)

### G1. Planned payments
- **Header:** "Next month — {month}", with **You'll pay** and **You'll receive**.
- **List:** name, amount, frequency, and next occurrence or "Ended". Overdue payments are marked.
- **Tap** opens G2, and the **FAB (+)** opens G3.

### G2. Planned payment detail
- **Content:** amount, type, account, category, frequency, next occurrence, end, note.
- **Pay now** creates the real record and moves the payment to its next occurrence. It is hidden
  for Fio accounts.
- **Edit** opens G3, and **Delete** asks for confirmation.

### G3. New / edit planned payment
- Type, name, account, category (via C4), amount, frequency presets, start (first payment) and an
  optional end with a clear action.

### G4. Match payment
- Lists candidate transactions for the payment's due occurrence. Picking one marks the
  occurrence as paid.

---

## H. Statistics (Statistics tab)

### H1. Statistics
- **Cards** for the selected month: **Balance, Expenses, Income, Cash flow, Forecast**
  ("expenses next month").
- **Month switcher** at the bottom: `<` / "This month ▼" / `>`. The FAB is hidden here.
- Tapping **Expenses** opens H2, **Income** opens H3, and **Forecast** opens H4.

### H2. Expenses
- Category groups with %, amount and a bar. Tapping a group expands its records, and tapping a
  record opens C2.

### H3. Income
- The individual income records of the month. Tapping one opens C2.

### H4. Forecast
- The planned payments due next month.

---

## I. Data backup (More → Data backup)

### I1. Backup
- **Intro:** the app is offline, so a backup is the only copy outside the phone.
- **Back up data:**
  1. The password dialog asks for the password twice, with a minimum length and not digits only.
  2. The system "save file" picker opens.
  3. The encrypted backup is written.
- **Restore from backup:**
  1. The system file picker opens.
  2. The file type is detected, and the flow continues as in A1 (password, confirmation or
     error).
  3. The data is overwritten and the app restarts.

---

## Transitions (simplified)

```
Start → (no account) Onboarding ─ Start / Restore → Overview
Overview ─FAB→ New record → (Save) → back
Overview → Upcoming payment → Match payment
Records → Record detail → Edit → (Save) → back
Payments ─FAB→ New planned payment;  Payments → Detail → Pay now / Edit / Delete
Statistics → Expenses / Income / Forecast → Record detail
More → Accounts → Account (edit) → Fio connection → Recurring payments
More → Categories / Budgets (+ Analytics) / Data backup
```

## Shared UX rules
- **Empty states** have a short message and a clear call to action, for example "No records
  yet. Add the first one with +."
- **Deleting** always asks for confirmation. Where there are linked entities, the dialog says
  what happens to them.
- **Amounts** use the selected currency symbol and a space-grouped number format with two
  decimals. The sign is typographic: `+ 1 250,00 Kč`, `− 350,00 Kč`.
- **Accessibility:** high contrast, large touch targets (52 dp buttons, 64 dp bottom bar), and
  content descriptions on icons.
