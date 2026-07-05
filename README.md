# Telesis

> **A local-first Android expense tracker that turns transaction SMS into a clean personal finance dashboard.**

Telesis is an open-source Android app for personal expense tracking. It is built natively with **Kotlin**, **Jetpack Compose**, and **Room**, with a strict privacy-first direction: no account, no cloud sync, no ads, no analytics SDK, and no internet permission.

<p align="center">
  <img src="docs/screenshots/dashboard.svg" alt="Telesis dashboard preview" width="30%" />
  <img src="docs/screenshots/sms-review.svg" alt="Telesis SMS review queue preview" width="30%" />
  <img src="docs/screenshots/settings.svg" alt="Telesis settings preview" width="30%" />
</p>

<p align="center">
  <a href="https://github.com/smeetbuilds/telesis/actions/workflows/android-apk.yml"><img src="https://github.com/smeetbuilds/telesis/actions/workflows/android-apk.yml/badge.svg" alt="Android APK build" /></a>
  <a href="https://github.com/smeetbuilds/telesis/releases/tag/latest"><img src="https://img.shields.io/badge/APK-download%20latest-16a34a" alt="Download latest APK" /></a>
  <img src="https://img.shields.io/badge/platform-Android-3ddc84" alt="Android" />
  <img src="https://img.shields.io/badge/privacy-local--only-111827" alt="Local only" />
</p>

---

## Download APK

The latest debug APK is published by GitHub Actions after a successful `main` build:

**Download:** https://github.com/smeetbuilds/telesis/releases/tag/latest

Release assets:

- `telesis-debug.apk` — installable Android APK
- `telesis-debug.apk.sha256` — checksum for verification

> This is a personal sideload build. It is not a Play Store release.

---

## Why Telesis exists

Most expense trackers either need manual entry forever or send your financial data to a remote service. Telesis takes a different route: it reads transaction SMS locally, parses debit/credit messages on your phone, and stores the result in a private local database.

The goal is simple: **personal finance tracking without surrendering personal finance data.**

---

## Features

### Expense tracking

- Add, edit, and delete expenses manually
- Track income, expenses, and transfers
- Set categories and budgets
- View monthly totals, daily spend, top merchants, and payment-mode breakdowns

### SMS transaction import

- Reads bank, card, wallet, UPI, and ATM transaction SMS locally
- Detects debit, credit, transfer, and cash withdrawal messages
- Extracts amount, merchant, account hint, and payment mode
- Avoids common duplicate imports using transaction hashes
- Review queue for uncertain SMS matches

### Automation

- Custom local parsing rules
- Auto-categorization
- Recurring expense generation
- Subscription candidate detection

### Privacy and backup

- No internet permission
- No account system
- No backend
- No analytics SDK
- Local Room database
- JSON backup and restore
- CSV export with spreadsheet formula-injection protection
- Optional PIN / biometric lock

---

## Permissions

Telesis uses sensitive permissions only for local transaction detection:

```xml
<uses-permission android:name="android.permission.READ_SMS" />
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="android.permission.USE_BIOMETRIC" />

<uses-feature
    android:name="android.hardware.telephony"
    android:required="false" />
```

The app intentionally does **not** declare `android.permission.INTERNET`.

Because SMS permissions are sensitive, this project is intended for personal sideloaded use unless future distribution requirements are reviewed carefully.

---

## Tech stack

- Kotlin
- Jetpack Compose
- Material 3
- Room
- DataStore Preferences
- Coroutines / Flow
- GitHub Actions for APK builds

Application ID:

```text
com.smeet.telesis
```

---

## Build locally

Requirements:

- Android Studio
- JDK 17
- Android SDK 35

Commands:

```bash
chmod +x ./gradlew
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
```

Generated APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## GitHub Actions

The workflow at `.github/workflows/android-apk.yml` runs on `main`, pull requests, tags, and manual dispatch.

It performs:

```bash
./gradlew :app:testDebugUnitTest --stacktrace
./gradlew :app:lintDebug --stacktrace
./gradlew :app:assembleDebug --stacktrace
```

After a successful `main` build, it publishes a refreshed `latest` GitHub Release containing the debug APK.

---

## Project structure

```text
app/src/main/java/com/smeet/telesis/
├── data/       # Room entities, DAO, database, repository
├── sms/        # SMS parser, category engine, receiver
├── ui/         # Compose UI components and theme
├── util/       # Money, date, security helpers
├── MainActivity.kt
└── TelesisApp.kt
```

---

## Current status

Telesis is an early open-source personal app. The debug APK pipeline is active, and the app is intended for testing, iteration, and careful local use.

Known practical limitation: SMS parsing differs by bank, wallet, UPI app, card issuer, and country. Parser rules should be expanded based on real message formats.

---

## Contributing

Useful contributions include:

- More SMS parser patterns
- Better category rules
- UI polish
- Accessibility improvements
- Export/import testing
- Bank-specific parsing fixtures

Please avoid adding internet/network SDKs unless the privacy model is explicitly discussed first.

---

## Disclaimer

Telesis is not financial advice, accounting software, or a banking product. It is a local personal expense tracker. Always verify imported transactions against your actual bank or card statement.
