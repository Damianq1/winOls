package com.winols.build

import java.io.File
import java.io.IOException

object GradleRunner {

    fun execute(projectDir: File, vararg tasks: String): Process {
        val gradlewFile = File(projectDir, "gradlew")

        // Zapewnienie uprawnień do uruchomienia pliku wykonywalnego gradlew
        if (gradlewFile.exists() && !gradlewFile.canExecute()) {
            gradlewFile.setExecutable(true)
        }

        val command = mutableListOf(gradlewFile.absolutePath).apply {
            addAll(tasks)
        }

        val processBuilder = ProcessBuilder(command).apply {
            directory(projectDir)
            redirectErrorStream(true)
        }

        return try {
            processBuilder.start()
        } catch (e: IOException) {
            throw IllegalStateException("Nie udało się uruchomić procesu Gradle: ${e.message}", e)
        }
    }
}