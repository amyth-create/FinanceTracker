# Finance Tracker

A clean, completely offline personal-finance app for Android. Built natively in Kotlin with Room (SQLite), Material 3, MVVM, and Navigation Component. No internet, no ads, no tracking, no subscriptions — my financial data stays on my device.

Built as a personal project with the help of Google Gemini.

---

## Features

- **Dashboard** — total balance, income vs spending for the month, and the latest transactions at a glance
- **Add transaction** — pick income or expense, choose a category, set the amount, date, and an optional note
- **Transactions list** — full history with filter chips (All / Expenses / Income) and tap-to-delete
- **Reports** — last-6-months bar chart, category breakdown with percentage bars, monthly snapshot, and savings rate
- **Categories** — 14 default expense categories and 7 default income categories, all editable; add fully custom categories with your own emoji + colour
- **Defaults seeded automatically** — first launch populates the database so the app is usable immediately
- **Offline-first** — everything is stored locally in `finance_tracker.db`; the app requests no internet permission

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

The app is a single Activity with bottom navigation between four destinations:

- `DashboardFragment` — summary tiles + recent transactions
- `TransactionsFragment` — full filterable history
- `AddTransactionFragment` — entry form (opened via the central + button)
- `ReportsFragment` — charts + category breakdown
- `CategoriesFragment` — manage default and custom categories

---

## Project structure

```
app/src/main/
├── java/com/personal/financetracker/
│   ├── data/
│   │   ├── Transaction.kt          Room @Entity
│   │   ├── Category.kt             Room @Entity + default seed lists
│   │   ├── TransactionDao.kt
│   │   ├── CategoryDao.kt
│   │   ├── AppDatabase.kt          Room DB, seeds defaults on first create
│   │   └── Repository.kt           Single source of truth for the app
│   ├── ui/
│   │   ├── MainActivity.kt
│   │   ├── dashboard/              DashboardFragment + ViewModel
│   │   ├── transactions/           List + Adapter + ViewModel
│   │   ├── add/                    AddTransactionFragment + ViewModel
│   │   ├── reports/                Charts + ViewModel
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

---

## Getting the code

```bash
git clone https://github.com/<your-username>/FinanceTracker.git
cd FinanceTracker
```

---

## Build and run from Android Studio

1. **Install Android Studio** — https://developer.android.com/studio
2. **Open the project** — File → Open → select the `FinanceTracker` folder (do not use *New Project*)
3. **Wait for Gradle sync** — first sync downloads all dependencies (1–3 minutes). Wait until the status bar reads *Gradle sync finished*.
4. **Pick a device:**
   - **Physical phone:** enable Developer Options (Settings → About Phone → tap *Build Number* 7 times), turn on **USB Debugging**, plug the phone into your computer, and accept the debug prompt
   - **Emulator:** Tools → Device Manager → create any virtual device with API 26+
5. **Press the green ▶ Run button.** Android Studio builds, installs, and launches the app.

---

## Build a shareable APK (for friends)

If you'd rather hand a friend an installable file than have them open Android Studio:

```bash
# from the project root
./gradlew assembleDebug
```

The APK lands at:

```
app/build/outputs/apk/debug/app-debug.apk
```

Send that file. To install it, the receiver needs to:

1. Copy `app-debug.apk` to their Android phone (USB, email, Drive, whatever)
2. Open it from the file manager
3. Approve the *Install from unknown sources* prompt for that file manager
4. Tap **Install**

> Debug APKs are signed with the auto-generated debug key, which is fine for personal use. To publish on the Play Store you'd need to generate a release keystore and switch the build type to `release`.

You can also publish the APK as a **Release** on GitHub (Releases → Draft a new release → attach `app-debug.apk`) so anyone can download it from the repo page.

---

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

All data is stored locally in a SQLite database file named `finance_tracker.db`, inside the app's private storage on the device. Nothing is sent off-device. The app declares no internet, location, or storage permissions.

Uninstalling the app deletes the database. There is currently no built-in import/export — that's an obvious next feature.

---

## Roadmap / nice-to-haves

- CSV / JSON export and import
- Budgets and per-category limits
- Recurring transactions
- Dark theme polish
- Cloud backup (optional, opt-in)

PRs and ideas welcome.

---

## Credits

- Built by Amyth, with Google Gemini as a coding assistant
- Charts by [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart)
- Icons from Material Symbols

---

## License

MIT — do whatever you like with the code. If you build something cool on top of it, drop a link.
