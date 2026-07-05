# Phase 2 Remediation Report

## Scope
Remediated the Phase 1 production-readiness audit findings for Telesis v1.0.0 in severity order: Critical, High, Medium, then final rescan/documentation.

## Critical fixes
- Backup restore now uses deterministic `restore|...` hashes and checks existing hashes before inserting restored expenses. Re-importing the same backup no longer duplicates every transaction.
- Due recurring expense generation now runs insert + next-due advancement inside a Room transaction and writes idempotent recurring hashes to prevent repeated materialization.

## High-severity fixes
- PIN storage now uses PBKDF2-HMAC-SHA256 with 120,000 iterations for new PINs, with legacy SHA-256 migration after successful unlock.
- CSV export now neutralizes spreadsheet formula-leading cells (`=`, `+`, `-`, `@`).
- Transaction list/search now observes all local transactions instead of only the latest 300.
- Manual expenses now support editing and explicit date entry in `YYYY-MM-DD` format.
- SMS amount extraction now scores amount candidates and penalizes balances/limits/outstanding amounts.
- Incoming SMS receiver now uses `goAsync()` and finishes only after async import completes.
- Added unit tests for SMS parsing and money parsing.
- Added Android CI workflow for tests, lint, and debug APK build.

## Medium-severity fixes
- SMS permission copy now clearly states private sideloaded use and no-internet local parsing.
- Batch SMS import caches rules and categories to reduce repeated database lookups.
- Subscription detection is throttled unless a material import/restore/update forces refresh.
- Backup JSON export now uses `JSONObject`/`JSONArray`, not manual string concatenation.
- Add/edit expense dialog is scrollable, height-constrained, and IME-aware.
- Backup/export controls are stacked full-width buttons for narrow devices.
- Bottom nav labels were shortened and selected-label behavior was tightened.

## Deferred items
- Gradle wrapper checksum pinning: deferred because this sandbox cannot fetch the official checksum. `GRADLE_WRAPPER_SECURITY.md` explains the exact trusted fix.
- Full APK build verification: deferred because this sandbox cannot resolve `services.gradle.org`. The wrapper starts correctly and CI/build commands are included.
- MainActivity split: deferred as follow-up architecture debt because a broad UI-file split is high-regression-risk and not required for build/security correctness.

## Verification performed
- XML resource parse check passed.
- Gradle wrapper JAR integrity check as a valid ZIP/JAR passed.
- Kotlin/JVM compile check for parser-related pure subset passed with local Room annotation stubs.
- Gradle wrapper launch attempted; blocked by DNS for `services.gradle.org`.
- Static rescan confirmed original risky patterns were removed or explicitly deferred.
