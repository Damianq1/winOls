package com.winols.build

import java.io.File
import java.io.IOException

class GradleRunner(private val projectDir: File) {

    /**
     * Uruchamia wrapper Gradle z zapewnieniem odpowiednich uprawnień wykonywalnych (POSIX).
     */
    fun runGradleCommand(vararg args: String): Process {
        val gradlewFile = File(projectDir, "gradlew")

        if (gradlewFile.exists()) {
            // Nadanie uprawnień do wykonania pliku (chmod +x) zapobiega błędowi [Errno 13] Permission denied
            if (!gradlewFile.canExecute()) {
                val success = gradlewFile.setExecutable(true, false)
                if (!success) {
                    // Alternatywne wywołanie chmod na systemach Linux/Android
                    try {
                        Runtime.getRuntime().exec(arrayOf("chmod", "+x", gradlewFile.absolutePath)).waitFor()
                    } catch (e: Exception) {
                        throw IOException("Nie udało się nadać uprawnień wykonywania dla ${gradlewFile.absolutePath}", e)
                    }
                }
            }
        } else {
            throw IOException("Plik gradlew nie istnieje w katalogu: ${projectDir.absolutePath}")
        }

        val command = mutableListOf(gradlewFile.absolutePath).apply {
            addAll(args)
        }

        return ProcessBuilder(command)
            .directory(projectDir)
            .redirectErrorStream(true)
            .start()
    }
}