# Build status

- Kotlin core sources were compiled successfully with `kotlinc` in the packaging environment.
- XML resources and Android manifest were parsed successfully.
- Intel HEX round-trip, bulk map editing and ADD8 checksum were exercised with runtime tests.
- Full Android Gradle build could not be executed in the packaging environment because external Gradle artifacts were not reachable from this environment. GitHub Actions is configured to perform the authoritative `:app:assembleDebug` build.

The repository contains a self-contained `gradlew` bootstrap script that downloads Gradle 8.2 when network access is available.
