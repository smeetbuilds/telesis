# Phase 2 Remediation Checklist

Status source of truth for the Phase 1 audit remediation.

## Critical
- [Done] CRIT-1 — Backup restore duplicates/corrupts ledger. Implemented deterministic restore hashes and duplicate-safe merge restore.
- [Done] CRIT-2 — Recurring materialization is not transactional. Wrapped recurring expense generation + recurrence advancement in Room `withTransaction` and added idempotent recurring hashes.

## High
- [Done] SEC-1 — PIN lock uses fast SHA-256. Replaced new PIN storage with PBKDF2-HMAC-SHA256 and legacy SHA-256 migration on successful unlock.
- [Done] SEC-2 — CSV formula injection. Neutralized formula-leading exported CSV cells.
- [Deferred] SEC-3 — Gradle wrapper integrity not pinned/verifiable. Reason: this sandbox cannot fetch the official Gradle checksum, and adding a guessed checksum would either break builds or create false supply-chain assurance. Added `GRADLE_WRAPPER_SECURITY.md` with exact trusted remediation steps.
- [Done] PERF-3 — Transaction list/search capped at 300 rows. Added DAO flow for all local transactions and wired repository/search to it.
- [Done] UX-4 — Manual edit/date support missing. Added edit action, pre-filled edit dialog, date field, and repository update flow.
- [Done] SMS-1 — SMS amount extraction can pick wrong amount. Replaced first-match amount parsing with contextual candidate scoring to avoid balance/limit amounts.
- [Done] ARCH-3 — Missing tests. Added unit tests for SMS parser and money parsing; added CI job to run tests/lint/build.
- [Done] ARCH-4 — BroadcastReceiver launches detached coroutine. Switched to `goAsync()` and guaranteed `finish()` in `finally`.
- [Deferred] BUILD-1 — Full APK build not verified in sandbox. Reason: Gradle wrapper starts but cannot resolve `services.gradle.org` due sandbox DNS/network restrictions. Build command and CI workflow are included.

## Medium
- [Done] SEC-4 — SMS permission sensitivity. Strengthened in-app privacy copy and added `SMS_PERMISSION_NOTICE.md`.
- [Done] PERF-1 — SMS import N+1 DB work. Cached categories and rules during SMS batch import.
- [Done] PERF-2 — Subscription detection recomputes too often. Added stale-window throttling with forced refresh on material data changes/import/restore.
- [Done] ARCH-2 — Backup JSON manually string-built. Replaced manual backup JSON string builder with `JSONObject`/`JSONArray` builder.
- [Done] UX-1 — Add Expense dialog can overflow. Added scrollable, height-constrained, IME-aware dialog body.
- [Done] UX-2 — Backup/export row can overflow. Replaced narrow horizontal row with stacked full-width actions.
- [Done] UX-3 — Bottom navigation cramped. Shortened labels and only shows selected label persistently.
- [Deferred] ARCH-1 — MainActivity monolithic. Reason: splitting ~800 lines of UI across feature files is structural refactoring with high regression risk and no immediate build/security defect; queued as follow-up tech debt.
- [Done] BUILD-2 — No CI config. Added GitHub Actions Android CI workflow.

## Low
- [Done] Documentation polish and final rescan.
