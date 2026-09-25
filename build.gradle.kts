import org.gradle.api.tasks.wrapper.Wrapper

plugins {
    // Podstawowe wtyczki projektu
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}

tasks.named<Wrapper>("wrapper") {
    gradleVersion = "8.7"
    distributionType = Wrapper.DistributionType.BIN
    
    // Wymuszenie uprawnień do uruchamiania (chmod +x) dla skryptu gradlew w środowiskach uniksowych
    doLast {
        val gradlewFile = file("gradlew")
        if (gradlewFile.exists()) {
            gradlewFile.setExecutable(true, false)
            gradlewFile.setReadable(true, false)
        }
    }
}