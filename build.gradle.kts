import org.gradle.api.tasks.wrapper.Wrapper

plugins {
    // Bazowe wtyczki projektu
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}

tasks.withType<Wrapper> {
    gradleVersion = "8.7"
    distributionType = Wrapper.DistributionType.ALL
    // Ustawienie umask / uprawnień unixowych dla generowanego skryptu gradlew
    doLast {
        val gradlewFile = project.file("gradlew")
        if (gradlewFile.exists()) {
            gradlewFile.setExecutable(true, false)
        }
    }
}