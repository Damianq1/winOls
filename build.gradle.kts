import org.gradle.api.tasks.Exec

plugins {
    // Bazowe wtyczki projektu WinOls
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}

// Zadanie wymuszające uprawnienia wykonywania dla skryptu gradlew w środowiskach uniksowych
tasks.register<Exec>("ensureGradlewExecutable") {
    description = "Nadaje uprawnienia wykonywania skryptowi gradlew."
    group = "build setup"
    onlyIf {
        !System.getProperty("os.name").lowercase().contains("windows")
    }
    commandLine("chmod", "+x", "./gradlew")
}

// Zapewnienie poprawnych uprawnień przy generowaniu wrappera
tasks.named<Wrapper>("wrapper") {
    gradleVersion = "8.7"
    distributionType = Wrapper.DistributionType.ALL
    doLast {
        if (!System.getProperty("os.name").lowercase().contains("windows")) {
            file("gradlew").setExecutable(true, false)
        }
    }
}