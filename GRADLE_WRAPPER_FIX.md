# Gradle Wrapper

This project includes the Gradle wrapper files required by Android Studio and CI tools:

- `gradlew`
- `gradlew.bat`
- `gradle/wrapper/gradle-wrapper.properties`
- `gradle/wrapper/gradle-wrapper.jar`

The wrapper is configured to use Gradle 8.11.1:

```text
distributionUrl=https\://services.gradle.org/distributions/gradle-8.11.1-bin.zip
```

Use from the project root:

```bash
./gradlew :app:assembleDebug
```

On Windows:

```bat
gradlew.bat :app:assembleDebug
```

Expected debug APK path:

```text
app/build/outputs/apk/debug/app-debug.apk
```
