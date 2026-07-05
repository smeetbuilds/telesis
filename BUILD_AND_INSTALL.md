# Build and Install Telesis v1.0.0

## Identifier

Android application ID / bundle identifier:

```text
com.smeet.telesis
```

## Debug build

Preferred build method:

1. Open the project folder in Android Studio.
2. Let Gradle sync finish.
3. Go to **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

Expected APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

This project does not include a fake prebuilt APK. Build it locally in Android Studio so the APK is generated against your installed Android SDK.

## Install

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Release build

Create a private signing key in Android Studio:

Android Studio > Build > Generate Signed Bundle / APK > APK > Create new key store.

Then generate the signed APK.

## Local-only verification checklist

After installation:

1. Open App Info > Permissions and confirm only SMS and biometric-related permissions are requested.
2. Confirm app does not have network permission.
3. Add one manual expense.
4. Run SMS import.
5. Verify imported items appear either in transactions or review queue.
6. Set a PIN and restart app.
7. Enable biometric unlock and test it.
8. Add a recurring expense and use Add Due.
9. Run subscription detection after importing repeated payments.
10. Export JSON backup, restore JSON backup, and export CSV.


## Gradle wrapper included

This corrected package includes the required Gradle wrapper files:

```text
gradlew
gradlew.bat
gradle/wrapper/gradle-wrapper.properties
gradle/wrapper/gradle-wrapper.jar
```

Build from the project root:

```bash
./gradlew :app:assembleDebug
```

Windows:

```bat
gradlew.bat :app:assembleDebug
```

Expected APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```
