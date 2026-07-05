# Gradle Wrapper Security

The project includes `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, and `gradle/wrapper/gradle-wrapper.properties` so Android Studio and CI can bootstrap the declared Gradle version.

Current distribution:

```text
gradle-8.11.1-bin.zip
```

Supply-chain note: this sandbox cannot fetch `https://services.gradle.org/distributions/gradle-8.11.1-bin.zip.sha256`, so the wrapper distribution checksum was not guessed or fabricated. On the build machine, regenerate and pin the official checksum with a trusted network:

```bash
gradle wrapper --gradle-version 8.11.1 --distribution-type bin
curl -fsS https://services.gradle.org/distributions/gradle-8.11.1-bin.zip.sha256
```

Then add the official value to `gradle/wrapper/gradle-wrapper.properties` as:

```properties
distributionSha256Sum=<official_sha256>
```

Do not add an unverified checksum. A wrong checksum breaks builds; a forged checksum defeats the supply-chain control.
