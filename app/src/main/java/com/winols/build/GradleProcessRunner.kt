package com.winols.build

import java.io.File
import java.io.IOException

class GradleProcessRunner(private val projectDir: File) {

    fun executeGradleCommand(vararg tasks: String): Result<String> {
        return runCatching {
            val gradlewScript = File(projectDir, "gradlew")
            
            if (!gradlewScript.exists()) {
                throw IOException("Plik gradlew nie został znaleziony w ścieżce: ${gradlewScript.absolutePath}")
            }

            // Naprawa problemu [Errno 13] Permission denied - nadanie uprawnień do wykonania
            if (!gradlewScript.canExecute()) {
                gradlewScript.setExecutable(true, false)
            }

            val command = mutableListOf(gradlewScript.absolutePath).apply {
                addAll(tasks)
            }

            val process = ProcessBuilder(command)
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                throw IOException("Kompilacja Gradle zakończona kodem błędu $exitCode:\n$output")
            }

            output
        }
    }
}