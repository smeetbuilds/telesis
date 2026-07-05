# Changelog

## v1.0.0 - 2026-07-05

### Added
- Created native Android app using Kotlin and Jetpack Compose.
- Added premium dark luxury UI using Material 3 components.
- Added fully local Room database with expenses, categories, SMS logs, rules, budgets, recurring expenses, and subscriptions.
- Added Android application ID / bundle identifier: `com.smeet.telesis`.
- Added default auto-created categories during first app launch and SMS import.
- Added manual expense creation with amount, merchant, category, payment mode, and note.
- Added transaction list with search and delete.
- Added local SMS inbox scanner using `READ_SMS`.
- Added incoming SMS receiver using `RECEIVE_SMS`.
- Added transaction SMS parser for debit, credit, transfers, UPI, card, wallet, ATM, bank, and net banking messages.
- Added amount extraction for ₹, INR, and Rs formats.
- Added merchant extraction and cleanup.
- Added auto-category engine for food, groceries, shopping, travel, fuel, bills, subscriptions, healthcare, education, entertainment, transfers, cash withdrawal, income, and other.
- Added duplicate SMS prevention through SHA-256 hash matching.
- Added SMS import review queue for low-confidence or transfer-like transactions.
- Added approve and ignore actions for review queue items.
- Added local category budgets and monthly budget progress.
- Added dashboard with monthly spend, today spend, remaining budget, top category, recent activity, review count, fixed commitments, subscriptions count, and daily spending bars.
- Added analytics screen with payment split, top merchants, category intelligence, daily spending trend, and subscription detector.
- Added user-defined local SMS rules for category correction and merchant override.
- Added recurring expenses manager with weekly, monthly, and yearly intervals.
- Added due-recurring-expense materialization into the transaction ledger.
- Added dedicated subscription detector using repeated merchant + amount patterns.
- Added local JSON backup export.
- Added local JSON backup restore/import.
- Added CSV export for spreadsheet review.
- Added local PIN lock with salted SHA-256 hash storage.
- Added biometric/device-credential unlock after PIN setup.
- Added version and privacy screens inside app.

### Changed
- Combined all planned earlier version features into one stable personal-use v1.0.0 codebase.
- Removed debug application ID suffix so debug and release builds use the same app identifier: `com.smeet.telesis`.

### Security / Privacy
- App intentionally does not declare `INTERNET` permission.
- Android cloud backup disabled for app database and shared preferences.
- Raw SMS bodies are not persisted as permanent transaction records; only parsed expense data and SMS hash/status are saved.
- PIN is stored as salted SHA-256 hash, not plaintext.
- Biometric unlock is optional and requires PIN to be enabled first.

### Build Note
- APK cannot be compiled inside this ChatGPT sandbox because Android SDK and Gradle wrapper/build tooling are not installed in the runtime.
- Open the source package in Android Studio to build the debug APK or signed release APK.
- Google Play distribution is not targeted because `READ_SMS`/`RECEIVE_SMS` permissions are heavily restricted for public Play Store apps.

## v1.0.0-wrapper-fix - 2026-07-05

### Fixed
- Added missing Gradle wrapper launcher files: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties`, and `gradle/wrapper/gradle-wrapper.jar`.
- Repackaged the project so Android Studio and external build systems can detect and run the Gradle wrapper from the project root.


## v1.0.0-buildfix.1 - 2026-07-05

### Fixed
- Fixed Compose compile blocker in `PremiumCard` by changing the slot receiver from invalid `Column.() -> Unit` to `ColumnScope.() -> Unit`.
- Added Room kapt schema arguments to avoid schema export configuration warnings during annotation processing.
- Simplified launcher vector path data to build-safe VectorDrawable path commands.
- Re-verified Gradle wrapper files are present at project root.

### Verified
- Project ZIP unpacks cleanly.
- Gradle wrapper JAR is a valid Java archive and reaches Gradle distribution bootstrap.
- XML files parse successfully.
- Manifest class references resolve to source files.
- Drawable/XML resource references resolve to existing resources.
- No `INTERNET` permission is declared.

## v1.0.0-phase2-remediated - 2026-07-05

### Fixed
- Prevented duplicate ledger corruption during JSON backup restore using deterministic restore hashes.
- Made recurring expense materialization idempotent and transactional.
- Replaced new PIN storage with PBKDF2-HMAC-SHA256 and legacy SHA-256 migration.
- Neutralized CSV formula injection vectors in exported spreadsheet cells.
- Removed 300-row transaction list/search cap.
- Added transaction editing and explicit expense date entry.
- Improved SMS amount extraction to avoid selecting balances or limits as transaction amounts.
- Fixed incoming SMS receiver async reliability using `goAsync()`.
- Reduced SMS batch import database churn by caching rules/categories.
- Replaced manual JSON backup builder with `JSONObject`/`JSONArray`.
- Improved small-screen layout for add/edit and backup/export controls.
- Added Android CI workflow and parser/money unit tests.

### Deferred
- Gradle distribution checksum pinning requires fetching the official checksum from a trusted network.
- Full APK build verification requires network access to Gradle distribution/dependencies.
- Splitting `MainActivity.kt` into feature files is retained as follow-up architecture debt.
