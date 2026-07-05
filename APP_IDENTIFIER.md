# App Identifier

Android application ID / bundle identifier:

```text
com.smeet.telesis
```

This is configured in:

```text
app/build.gradle.kts
```

Relevant Gradle block:

```kotlin
defaultConfig {
    applicationId = "com.smeet.telesis"
    versionCode = 100
    versionName = "1.0.0"
}
```

Kotlin source namespace:

```text
com.smeet.telesis
```

The Android application ID is the identifier installed on the phone. The Kotlin namespace is only the code namespace.
