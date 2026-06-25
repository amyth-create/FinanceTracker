# Finance Tracker

A clean, completely offline personal-finance app for Android. Built natively in Kotlin with Room (SQLite), Material 3, MVVM, and Navigation Component. No internet, no ads, no tracking, no subscriptions — my financial data stays on my device.

Built as a personal project with the help of Google Gemini.

---

## Features

- **Dashboard** — a gradient balance hero, a month selector to browse any month, and at-a-glance insight cards:
  - **Lifestyle spend** — Food + Shopping + Fun combined, with its share of the month's spending, so discretionary outlay is obvious
  - **Insights** — top category, spend vs last month (colour-coded), daily average, and projected month-end total
  - **Recurring & subscriptions** — automatically detects repeating payments (same category + note across 3+ months) and estimates the next date
- **Add transaction** — pick income or expense, choose a category, set the amount, date, and an optional note
- **Transactions list** — full history **grouped under date headers** with a per-day net total, filter chips (All / Expenses / Income), and tap-to-delete
- **CSV import / export** — export your history and re-import it (or another app's data); unknown categories are created automatically
- **Reports** — interactive, tap anything to read exact figures:
  - **Single month** or **compare two months** side-by-side (category-by-category difference in € and %)
  - **Category donut** (tap a slice), a **monthly bar chart** with value labels, and a **balance-over-time trend line** across your whole history
- **Future payments (Planned)** — jot down a payment you plan to make on a future date; **tick it off when you actually pay** and it logs a real transaction (dated today) that flows into your totals, charts, and history. Un-ticking removes it
- **Reminders** — a notification on the morning of a planned payment's date (survives reboots)
- **Categories** — 13 default expense + 7 default income categories, all editable; add custom categories with your own emoji + colour
- **Offline-first** — everything is stored locally in `finance_tracker.db`; no internet permission, no ads, no tracking

---

## Tech stack

| Area | Choice |
|---|---|
| Language | Kotlin 1.9.22 |
| Min / Target SDK | 26 / 34 |
| Architecture | MVVM (ViewModel + LiveData + Repository) |
| UI | View system, Material Components 1.11, ConstraintLayout, ViewBinding |
| Navigation | AndroidX Navigation Component (single Activity, multiple Fragments) |
| Persistence | Room 2.6.1 (SQLite) with KSP |
| Async | Kotlin Coroutines |
| Charts | [MPAndroidChart v3.1.0](https://github.com/PhilJay/MPAndroidChart) |
| Build | Gradle Kotlin DSL, AGP 8.2.2 |

---

## Screens

The app is a single Activity with bottom navigation between five destinations:

- `DashboardFragment` — balance hero, month selector, insight cards, recurring detection, recent transactions
- `TransactionsFragment` — full history grouped by day, with filters and CSV import/export
- `ReportsFragment` — interactive donut, trend line, monthly bars, and single/compare modes
- `PlannedFragment` — future payments you can tick off into real transactions
- `CategoriesFragment` — manage default and custom categories

`AddTransactionFragment` is the entry form, opened via the central **+** button.

---

## Project structure

```
app/src/main/
├── java/com/personal/financetracker/
│   ├── data/
│   │   ├── Transaction.kt          Room @Entity
│   │   ├── Category.kt             Room @Entity + default seed lists
│   │   ├── PlannedPayment.kt       Room @Entity for future payments
│   │   ├── TransactionDao.kt
│   │   ├── CategoryDao.kt
│   │   ├── PlannedPaymentDao.kt
│   │   ├── AppDatabase.kt          Room DB (v2), seeds defaults, v1→v2 migration
│   │   └── Repository.kt           Single source of truth for the app
│   ├── notify/
│   │   ├── ReminderScheduler.kt    Schedules/cancels planned-payment alarms
│   │   ├── ReminderReceiver.kt     Posts the reminder notification
│   │   └── BootReceiver.kt         Re-schedules reminders after reboot
│   ├── ui/
│   │   ├── MainActivity.kt
│   │   ├── dashboard/              Insights, lifestyle, recurring + ViewModel
│   │   ├── transactions/           Grouped list + adapters + CSV import/export
│   │   ├── add/                    AddTransactionFragment + ViewModel
│   │   ├── reports/                Interactive charts + ViewModel
│   │   ├── planned/                Future payments screen + adapter + ViewModel
│   │   └── categories/             Manage categories + Adapter + ViewModel
│   └── util/
│       └── Formatters.kt           Currency + date helpers
└── res/
    ├── layout/                     XML layouts for screens and list items
    ├── drawable/                   Vector icons + shapes
    ├── anim/                       Fragment fade/slide transitions
    ├── menu/bottom_nav_menu.xml
    ├── mipmap-*/                   Launcher icons
    └── values/                     colors.xml, strings.xml, themes.xml
```


## Customising

**Currency.** Currency formatting lives in `app/src/main/java/com/personal/financetracker/util/Formatters.kt`:

```kotlin
private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.GERMANY)
```

Out of the box this renders amounts as Euros (€). Change the `Locale` to suit your country — for example `Locale.UK` for GBP (£), `Locale.US` for USD ($), or `Locale("en", "IN")` for INR (₹).

**Default categories.** Edit `defaultExpenseCategories` and `defaultIncomeCategories` in `data/Category.kt`. They are only seeded on first launch (when the categories table is empty), so to re-seed wipe app data or uninstall and reinstall.

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

- Built by Amyth, with Google Gemini as a coding assistant
- Charts by [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart)
- Icons from Material Symbols

---

## License

MIT — do whatever you like with the code. If you build something cool on top of it, drop a link.
