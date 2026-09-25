package com.winols.editor.builder

import java.io.File
import java.io.IOException

class GradleRunner(private val projectDir: File) {

    /**
     * Uruchamia zadanie Gradle, upewniając się wcześniej, że skrypt gradlew ma uprawnienia wykonywania.
     */
    fun execute(task: String): Process {
        val gradlewFile = File(projectDir, "gradlew")

        if (!gradlewFile.exists()) {
            throw IOException("Plik gradlew nie istnieje w katalogu: ${projectDir.absolutePath}")
        }

        // Naprawa błędu [Errno 13] Permission denied: './gradlew'
        if (!gradlewFile.canExecute()) {
            val permissionGranted = gradlewFile.setExecutable(true, false)
            if (!permissionGranted) {
                // Alternatywna próba zmiany uprawnień za pomocą chmod w środowiskach uniksowych
                Runtime.getRuntime().exec(arrayOf("chmod", "+x", gradlewFile.absolutePath)).waitFor()
            }
        }

        val command = listOf(gradlewFile.absolutePath, task)
        val processBuilder = ProcessBuilder(command)
            .directory(projectDir)
            .redirectErrorStream(true)

        return processBuilder.start()
    }
}