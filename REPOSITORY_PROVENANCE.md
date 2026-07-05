# Repository provenance

This repository contains the Telesis Android source pushed from the remediated local source package.

## Application identity

- App name: Telesis
- Android application ID: `com.smeet.telesis`
- Kotlin namespace: `com.smeet.telesis`

## Reproducible build command

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
```

The GitHub Actions workflow `.github/workflows/android-apk.yml` runs those commands and uploads the APK artifact.
