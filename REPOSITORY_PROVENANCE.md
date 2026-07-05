# Repository provenance

This repository was initialized from the ChatGPT GitHub connector session.

## Current verified state

- Repository: `SMITGHORI/telesis`
- App name: `Telesis`
- Intended Android application ID: `com.smeet.telesis`
- APK workflow path: `.github/workflows/android-apk.yml`

## Important source-status note

The full local Android source folder could not be pushed through the connector in this session because the available GitHub actions do not expose a direct local-folder push operation, and the compressed archive transport path was blocked.

Therefore, the repository-facing README and CI workflow have been added, but the full Android source still needs to be pushed through normal local Git.

## Local push command

```bash
cd /path/to/telesis
rm -rf .git
git init
git remote add origin https://github.com/SMITGHORI/telesis.git
git branch -M main
git add .
git commit -m "feat: add Telesis Android app"
git push -u origin main --force
```

## Intended build commands

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
```
