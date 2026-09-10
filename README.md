# Finance Tracker

A clean, completely offline personal-finance app for Android. Built natively in Kotlin with Room (SQLite), Material 3, MVVM, and Navigation Component. No internet, no ads, no tracking, no subscriptions — my financial data stays on my device.

Built as a personal project with the help of Google Gemini; v2 redesign done with Claude.

---

## Features

- **Home** — greeting, a **month picker** (‹ › arrows), and a hero card with net for the month, income vs. spent with a spend-to-income bar, and your all-time balance. Below it: daily average, projected month-end spend, spending change vs. last month, top category, the top spending categories with bars, **upcoming planned payments** (next 30 days), **detected recurring payments**, and recent transactions.
- **Add / edit transaction** — Expense/Income toggle, a big amount field, category chips, quick date chips (Today / Yesterday / Pick…), an optional note, and delete when editing.
- **Activity** — full history grouped under day headers ("Today", "Yesterday", "Wed 17 Jun") with a per-day net, **search** by note or category, All / Expenses / Income filters, tap to edit and **swipe left to delete with undo**. CSV import/export live in the ⋮ menu.
- **Reports** — modelled on how Monarch, Copilot and YNAB present money, and built around one question per section:
  - **Period navigation** — ‹ › through months; tap the period label to switch to **quarters or years**. Every number has a comparison against the **previous period or the same period last year** (choose at the top right).
  - **Spending / Income tabs** — total with ▲/▼ change vs. the comparison period, per-day and per-transaction averages; a **trend chart** for the last 6 periods with a dashed average line (tap a bar to jump to that period); a **stacked share bar** plus a category list showing amount, share, transaction count and change vs. the comparison period; **spending pace** (cumulative spend by day vs. the previous month, Copilot-style); **spending by day of week**; **top places & notes**; and the **largest transactions**.
  - **Tap any category** to drill down: share, count, average, change, a 6-period history chart, and every transaction in that category for the period (tap to edit).
  - **Cash flow tab** — net with savings rate (or overspend), income vs. spending bar, a **Sankey diagram** showing how income sources flow into spending categories and what was saved, income vs. spending for the last 12 months, and your cumulative **balance over time**.
- **Planned** — plan a future payment and **tick it off when paid**; that logs a real transaction dated today. Upcoming and done sections, relative due dates ("Tomorrow", "In 5 days", "Overdue"), swipe to delete with undo.
- **Reminders** — a notification on the morning of a planned payment's date (survives reboots).
- **Settings** — manage categories (emoji, name and a **colour palette**), export/import CSV, and app info.
- **Offline-first** — everything is stored locally in `finance_tracker.db`; no internet permission, no ads, no tracking.

---

## Tech stack

| Area | Choice |
|---|---|
| Language | Kotlin 1.9.22 (java.time for periods) |
| Min / Target SDK | 26 / 34 |
| Architecture | MVVM (ViewModel + LiveData + Repository) |
| UI | View system, Material Components 1.11, ConstraintLayout, ViewBinding |
| Navigation | AndroidX Navigation Component (single Activity, multiple Fragments) |
| Persistence | Room 2.6.1 (SQLite) with KSP |
| Async | Kotlin Coroutines |
| Charts | [MPAndroidChart v3.1.0](https://github.com/PhilJay/MPAndroidChart) + custom Sankey / bar views |
| Build | Gradle Kotlin DSL, AGP 8.2.2 |
| Tests | JUnit 4 unit tests for the domain layer (`./gradlew testDebugUnitTest`) |

---

## Screens

The app is a single Activity with bottom navigation between four destinations, plus three full-screen pages reached from them:

- `DashboardFragment` (Home) — month picker, hero, insight tiles, top categories, upcoming, recurring, recent
- `TransactionsFragment` (Activity) — searchable, filterable history with swipe-to-delete
- `ReportsFragment` — Spending / Income / Cash flow tabs, period + comparison controls, `CategoryDetailSheet` drill-down
- `PlannedFragment` — future payments you can tick off into real transactions
- `AddTransactionFragment` — add or edit a transaction (opened from any + button or by tapping a row)
- `SettingsFragment` → `CategoriesFragment` — category management and CSV import/export

---

## Project structure

```
app/src/main/
├── java/com/personal/financetracker/
│   ├── data/
│   │   ├── Transaction.kt          Room @Entity
│   │   ├── Category.kt             Room @Entity + default seed lists
│   │   ├── PlannedPayment.kt       Room @Entity for future payments
│   │   ├── *Dao.kt                 Room DAOs
│   │   ├── AppDatabase.kt          Room DB (v2), seeds defaults synchronously, v1→v2 migration
│   │   ├── Repository.kt           Single source of truth for the app
│   │   └── CsvTransfer.kt          CSV export/import parsing
│   ├── domain/
│   │   ├── Period.kt               Month / quarter / year periods (java.time, DST-safe)
│   │   └── Analytics.kt            Pure calculations: summaries, categories, trends, pace, weekday, Sankey, recurring
│   ├── notify/                     Planned-payment reminders (AlarmManager + boot receiver)
│   ├── ui/
│   │   ├── MainActivity.kt         Bottom nav host; hides nav on full-screen pages
│   │   ├── common/                 PeriodPickerView, SegmentedControl, RatioBarView, StackedBarView, SankeyView, chart styling
│   │   ├── dashboard/              Home + ViewModel
│   │   ├── transactions/           Activity list, adapters, search + swipe delete
│   │   ├── add/                    Add/edit transaction
│   │   ├── reports/                Reports + ViewModel + CategoryDetailSheet
│   │   ├── planned/                Planned payments
│   │   ├── categories/             Category management
│   │   └── settings/               Settings
│   └── util/Formatters.kt          Currency + date helpers
├── res/
│   ├── layout/                     Screens, report sections, list rows
│   ├── drawable/                   Vector icons + shapes
│   ├── values/                     colors, dimens, strings, themes (typography + component styles)
│   └── navigation/nav_graph.xml
└── test/                           JUnit tests for Period and Analytics
```


## Customising

**Currency.** Currency formatting lives in `app/src/main/java/com/personal/financetracker/util/Formatters.kt`:

```kotlin
private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.GERMANY)
```

Out of the box this renders amounts as Euros (€). Change the `Locale` to suit your country — for example `Locale.UK` for GBP (£), `Locale.US` for USD ($), or `Locale("en", "IN")` for INR (₹).

**Default categories.** Edit `defaultExpenseCategories` and `defaultIncomeCategories` in `data/Category.kt`. They are seeded when the database is first created, so to re-seed wipe app data or uninstall and reinstall.

**Theme & colours.** Tweak `res/values/colors.xml` and `res/values/themes.xml`.

---

## Data and privacy

All data is stored locally in a SQLite database file named `finance_tracker.db`, inside the app's private storage on the device. Nothing is sent off-device — the app declares no internet permission.

The only permissions used are `POST_NOTIFICATIONS` (to show planned-payment reminders) and `RECEIVE_BOOT_COMPLETED` (to restore reminders after a reboot). CSV export/import goes through the system share/file-picker, so you stay in control of where data goes.

Uninstalling the app deletes the database. The app database is at schema **version 2**; upgrading from an older build runs a migration that adds the `planned_payments` table without touching existing data.

---

PRs and ideas welcome.

---

## Credits

- Built by Amyth, with Google Gemini as a coding assistant; v2 redesign with Claude
- Report design informed by how Monarch Money, Copilot Money and YNAB present spending, cash flow and comparisons
- Charts by [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart)
- Icons from Material Symbols

---

## License

MIT — do whatever you like with the code. If you build something cool on top of it, drop a link.
