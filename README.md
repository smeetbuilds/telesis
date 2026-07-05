# Telesis

> **A private, local-first Android expense tracker that reads transaction SMS on-device and turns them into a clean personal finance dashboard.**

Telesis is planned as an open-source Android app for people who want a personal expense tracker without cloud lock-in, ads, analytics, accounts, or server-side data collection. It is designed for native Android with Kotlin, Jetpack Compose, and Room.

<p align="center">
  <a href="https://github.com/SMITGHORI/telesis/actions/workflows/android-apk.yml"><img src="https://github.com/SMITGHORI/telesis/actions/workflows/android-apk.yml/badge.svg" alt="Android APK build" /></a>
  <a href="https://github.com/SMITGHORI/telesis/releases"><img src="https://img.shields.io/badge/APK-download%20from%20releases-16a34a" alt="Download APK from GitHub Releases" /></a>
  <img src="https://img.shields.io/badge/platform-Android-3ddc84" alt="Android" />
  <img src="https://img.shields.io/badge/privacy-local--only-111827" alt="Local only" />
</p>

---

## Current status

The repository has been initialized, but the full Android source push is still pending because the connected GitHub transport available in this chat cannot directly push a local folder and blocked the compressed archive route.

The intended Android application ID is:

```text
com.smeet.telesis
```

---

## Product direction

Telesis should remain:

- local-first;
- no backend;
- no Firebase;
- no analytics SDK;
- no internet permission;
- personal sideloaded Android use;
- SMS parsing on-device only.

---

## Intended core features

- Manual expense add, edit, date entry, and delete
- SMS inbox import using `READ_SMS`
- Incoming transaction SMS receiver using `RECEIVE_SMS`
- Debit, credit, transfer, ATM, bank, UPI, card, and wallet detection
- Smart amount extraction that avoids common balance/limit amounts
- Merchant extraction and cleanup
- Duplicate SMS prevention using hashes
- Review queue for uncertain imports
- Auto-created default categories
- Auto-categorization during SMS import
- Custom local SMS rules
- Category budgets
- Recurring expenses
- Due recurring expense generation
- Subscription candidate detection
- Monthly spending dashboard
- Daily spending bars
- Payment-mode split
- Top merchant insights
- PIN lock
- Biometric/device-credential unlock
- Local JSON backup export/restore
- CSV export with spreadsheet formula-injection protection

---

## Intended APK build workflow

The intended workflow path is:

```text
.github/workflows/android-apk.yml
```

It should run:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
```

Then it should upload the generated debug APK as a GitHub Actions artifact and attach APKs to GitHub Releases when version tags are pushed.

---

## SMS permission notice

Telesis is intended to request SMS permissions for one reason: to detect transaction messages on the same phone and convert them into local expenses.

Declared sensitive permissions in the final Android app should be:

```xml
<uses-permission android:name="android.permission.READ_SMS" />
<uses-permission android:name="android.permission.RECEIVE_SMS" />
```

This project is not designed for Play Store distribution. Public distribution with SMS permissions has policy restrictions. Use this app only when you understand and accept the permission model.
