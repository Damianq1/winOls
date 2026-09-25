package com.winols.builder

import java.io.File
import java.io.IOException

class GradleRunner(private val projectDir: File) {

    /**
     * Uruchamia wrapper Gradle, upewniając się wcześniej,
     * że plik skryptu posiada prawa do wykonywania (chmod +x / executable).
     */
    fun runGradle(vararg tasks: String): Process {
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        val gradlewScript = if (isWindows) {
            File(projectDir, "gradlew.bat")
        } else {
            File(projectDir, "gradlew")
        }

        if (!gradlewScript.exists()) {
            throw IOException("Nie znaleziono skryptu Gradle Wrapper w: ${gradlewScript.absolutePath}")
        }

        // Naprawa błędu [Errno 13] Permission denied: nadanie uprawnień do uruchomienia
        if (!isWindows && !gradlewScript.canExecute()) {
            val success = gradlewScript.setExecutable(true)
            if (!success) {
                // Alternatywne wymuszenie przez ProcessBuilder w środowiskach POSIX
                Runtime.getRuntime().exec(arrayOf("chmod", "+x", gradlewScript.absolutePath)).waitFor()
            }
        }

        val command = mutableListOf<String>().apply {
            add(gradlewScript.absolutePath)
            addAll(tasks)
        }

        val processBuilder = ProcessBuilder(command).apply {
            directory(projectDir)
            redirectErrorStream(true)
        }

        return processBuilder.start()
    }
}