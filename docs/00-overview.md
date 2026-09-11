# 00 — Overview and scope

## What Heller is

Heller is an offline-first personal finance tracker for Android, built for **black-and-white
E-Ink phones**, primarily the **Mudita Kompakt**. It shows the user **where their money goes**.

Its feature set is modeled on Wallet by BudgetBakers: accounts, records, categories, budgets,
statistics and planned payments. On top of that it **automatically imports transactions from
Fio banka**, auto-categorizes them, detects recurring payments and matches planned payments with
real transactions.

The main value: fast manual entry, a clear overview, and a Fio account that fills itself.

## Principles

1. **Offline-first.** The app works fully without internet and all data stays on the phone. The
   only network access is the Fio import, which the user opts into.
2. **Fast entry.** Adding an expense takes a few taps (FAB → amount → save). Imported Fio
   transactions need no manual entry.
3. **Exact money.** Amounts are never stored or computed as `float`/`double`. They are stored in
   minor units (`Long`, ×100) and computed with `BigDecimal`.
4. **Privacy.** Fio tokens are encrypted with an Android Keystore key. Backups are encrypted with
   a user password. Nothing leaves the device unless the user exports it.
5. **Calm E-Ink design.** Monochrome, high contrast, no animations, lots of white space. See
   [06-design-eink.md](06-design-eink.md).

## Language and currency

- **UI languages:** English (default), Czech, German and Polish. The app follows the system
  language. The names of the preset categories are localized too.
- **Currency:** the app works in a single currency, chosen during onboarding. There are 16
  options: CZK, EUR, USD, GBP, CHF, PLN, SEK, NOK, DKK, HUF, RON, UAH, CAD, AUD, JPY, INR. The
  currency only sets the symbol shown. Amounts are always stored as minor units ×100, and there
  are no exchange rates or conversions.
- **Bank connector:** only Fio banka, the Czech bank with a public read-only API.

## Scope

### Included
- Accounts (cash, checking, credit card, savings, investments, other) with computed balances and
  an optional **business account** flag
- Records: **expense / income / transfer** between accounts, plus marking a record as a transfer
  between your own accounts
- Hierarchical categories: 82 presets plus user-defined ones, each with a monochrome icon
- Automatic categorization that learns from the user's choices
- Dashboard: net worth, trend charts, top expenses, upcoming payments, recent records
- Statistics per month: balance, expenses, income, cash flow, forecast
- Budgets per category group, with weekly, monthly or yearly periods
- Analytics: monthly summary, spending by category, 6-month cash flow
- Planned (recurring) payments, "Pay now", automatic and manual matching with transactions
- Recurring payment detection from Fio history
- Fio import (multiple accounts, daily background sync)
- Password-protected local backup and restore, which also works from onboarding

### Intentionally not included
- Record search (the records filter covers it)
- Theme switching (a single paper/ink theme)
- Labels/tags (categories cover them)
- Push notifications and payment reminders
- A settings screen (currency is chosen in onboarding; everything else lives under "More")
- Templates, custom budget periods and budget rollover
- Multiple currencies at once and exchange rates
- Outgoing bank payments (the Fio token is read-only)
- Cloud sync and banks other than Fio

## Heller compared with Wallet

| Wallet has | Heller | Note |
|---|---|---|
| Sync with 15,000+ banks (PSD2) | Fio only | PSD2 access would need an AISP license from the Czech National Bank |
| Cloud, web, shared accounts | Local only | Encrypted file backup instead |
| AI categorization | Rule engine: learned rules, merchant dictionary, heuristics | No ML |
| Colorful charts and categories | Monochrome: bars, lines, icons, signs | Designed for E-Ink |
| Premium subscription | Everything free and local | No user accounts, no payments |
