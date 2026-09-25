import java.io.File

plugins {
    // Standardowe wtyczki projektu Android
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlinBłąd `[Errno 13] Permission denied: './gradlew'` nie wynika z błędu logicznego w kodzie źródłowym Kotlin, lecz z braku uprawnień wykonywania (`executable`) dla skryptu powłoki `gradlew` w środowisku Unix/Linux/macOS lub braku flagi wykonywalności w indeksie Gita.

Aby zapobiec temu błędowi bezpośrednio w konfiguracji kompilacji Gradle i zapewnić, że wrapper zawsze posiada uprawnienia wykonywania podczas generowania lub aktualizowania przez Gradle, konfiguruje się zadanie `Wrapper` w głównym skrypcie kompilacji.

### build.gradle.kts