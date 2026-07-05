# Telesis v1.0.0 QA Checklist

Use this checklist after building and installing the APK on your Android phone.

## Install and launch

- App installs as Telesis.
- App identifier is `com.smeet.telesis`.
- App opens without login.
- Dashboard loads with empty states.

## Permissions and privacy

- App requests SMS access only when opening SMS import.
- App requests biometric/device credential only when biometric unlock is enabled.
- App does not declare `INTERNET` permission.
- App Info does not show network access permission.

## Manual expenses

- Add a manual UPI expense.
- Add a manual cash expense.
- Search transaction list by merchant/category/mode.
- Delete a transaction.

## SMS import

- Grant `READ_SMS` and `RECEIVE_SMS`.
- Run SMS scan.
- Confirm clear debit messages import directly.
- Confirm uncertain or transfer-like messages appear in Review Queue.
- Approve one review item.
- Ignore one review item.
- Re-run SMS scan and confirm duplicates are not imported again.

## Categories and budgets

- Confirm default categories are auto-created.
- Set a category monthly budget.
- Confirm dashboard remaining budget updates.
- Confirm category budget progress updates.

## Rules

- Add a rule such as `ZOMATO -> Food`.
- Re-import relevant SMS or import future SMS.
- Confirm matching merchant/category behavior.

## Analytics

- Confirm payment split appears after transactions exist.
- Confirm top merchants appear.
- Confirm daily spend bars appear after dated transactions exist.
- Confirm category intelligence appears.

## Recurring and subscriptions

- Add a recurring monthly expense.
- Use Add Due and confirm transaction is generated.
- Run subscription detection after repeated merchant + amount transactions exist.
- Confirm subscription candidates show confidence and next expected date.

## Backup and export

- Export JSON backup.
- Export CSV.
- Restore JSON backup into a fresh install or after clearing data.
- Confirm expenses, categories, rules, and recurring items restore.

## Locking

- Enable PIN.
- Close and reopen app.
- Unlock with PIN.
- Enable biometric/device credential.
- Close and reopen app.
- Unlock with biometric/device credential.
