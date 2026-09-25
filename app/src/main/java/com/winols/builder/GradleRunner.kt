package com.winols.builder

import java.io.File
import java.io.IOException

class GradleRunner(private val projectDir: File) {

    /**
     * Uruchamia Gradle wrapper z zapewnieniem odpowiednich uprawnień wykonywania.
     */
    fun executeCommand(vararg args: String): Process {
        val gradlew = File(projectDir, "gradlew")

        if (!gradlew.exists()) {
            throw IOException("Plik gradlew nie istnieje w katalogu: ${projectDir.absolutePath}")
        }

        // Zapewnienie uprawnień wykonywalności (odpowiednik chmod +x ./gradlew)
        if (!gradlew.canExecute()) {
            val success = gradlew.setExecutable(true, false)
            if (!success) {
                // Alternatywna próba przez wywołanie chmod na systemach POSIX / Android
                try {
                    ProcessBuilder("chmod", "+x", gradlew.absolutePath).start().waitFor()
                } catch (e: Exception) {
                    throw IOException("Nie udało się nadać uprawnień wykonywania dla ${gradlew.absolutePath}", e)
                }
            }
        }

        val command = mutableListOf(gradlew.absolutePath).apply {
            addAll(args)
        }

        return ProcessBuilder(command)
            .directory(projectDir)
            .redirectErrorStream(true)
            .start()
    }
}