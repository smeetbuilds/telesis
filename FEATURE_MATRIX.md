# Telesis v1.0.0 — All Six Versions Feature Matrix

Android application ID / bundle identifier: `com.smeet.telesis`
Kotlin namespace: `com.smeet.telesis`

## v0.1.0 — First installable MVP

Included in final v1.0.0 source:

- Native Android project using Kotlin and Jetpack Compose.
- Material 3 premium dark UI foundation.
- Fully local Room database.
- Manual expense add/delete.
- Transaction list.
- Monthly dashboard total.
- No internet permission.
- Version screen.
- Changelog and version files.

## v0.2.0 — SMS import

Included in final v1.0.0 source:

- `READ_SMS` runtime permission flow.
- `RECEIVE_SMS` runtime permission request and manifest receiver.
- Local SMS inbox scanner.
- Debit, credit, and transfer detection.
- Amount extraction for ₹, INR, and Rs formats.
- Duplicate prevention using SHA-256 SMS hash.
- Review queue for uncertain SMS items.

## v0.3.0 — Smart expense parsing

Included in final v1.0.0 source:

- Merchant extraction and cleanup.
- Auto-category detection.
- Payment mode detection: UPI, card, wallet, ATM, bank, net banking, cash, unknown.
- Confidence scoring.
- OTP, login, verification, promo, and offer SMS ignore logic.
- Custom local SMS rules for future categorization and merchant overrides.

## v0.4.0 — Premium dashboard

Included in final v1.0.0 source:

- Premium dashboard hero card.
- Category budget cards.
- Monthly daily-spend bar visualization.
- Top merchants.
- Payment mode split.
- Better empty states.
- Premium dark color system and rounded-card UI.

## v0.5.0 — Budgets and rules

Included in final v1.0.0 source:

- Monthly category budgets.
- Budget progress calculation.
- Local custom SMS rules.
- Recurring expense manager.
- Weekly, monthly, and yearly recurring intervals.
- Due-recurring-expense generation into the ledger.
- Subscription detector based on repeated merchant + amount patterns.

## v1.0.0 — Stable personal-use release

Included in final v1.0.0 source:

- Stable Android application ID: `com.smeet.telesis`.
- Local JSON backup export.
- Local JSON backup restore/import.
- CSV export.
- PIN lock using salted SHA-256 hash.
- Biometric/device-credential unlock after PIN setup.
- Privacy screen.
- No backend, no Firebase, no analytics SDK, no login, and no internet permission.
- Updated README, changelog, version, build/install notes, and this feature matrix.

## Build limitation in this package

This package contains complete Android Studio source code. It does not contain a prebuilt APK because the current generation environment does not have the Android SDK/build tools available. Build the APK from Android Studio using **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
