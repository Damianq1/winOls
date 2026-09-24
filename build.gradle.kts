plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}

// Sekcje allprojects / subprojects z blokiem repositories zostały usunięte.
// Repozytoria są zarządzane centralnie w settings.gradle.kts.
tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}