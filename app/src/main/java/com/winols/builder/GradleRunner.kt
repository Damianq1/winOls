package com.winols.builder

import java.io.File
import java.io.IOException

class GradleRunner(private val projectDir: File) {

    @Throws(IOException::class, InterruptedException::class)
    fun execute(vararg tasks: String): Int {
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        val gradlewFileName = if (isWindows) "gradlew.bat" else "gradlew"
        val gradlewFile = File(projectDir, gradlewFileName)

        if (!gradlewFile.exists()) {
            throw IOException("Gradle wrapper script not found: ${gradlewFile.absolutePath}")
        }

        // Naprawa błędu [Errno 13] Permission denied:
        // Na systemach Unix/Linux/Android nadajemy uprawnienia wykonywalności przed uruchomieniem
        if (!isWindows && !gradlewFile.canExecute()) {
            val executableSet = gradlewFile.setExecutable(true, false)
            if (!executableSet) {
                // Alternatywny fallback przez proces chmod, gdy uprawnienia natywne JVM zawiodą
                ProcessBuilder("chmod", "+x", gradlewFile.absolutePath)
                    .directory(projectDir)
                    .start()
                    .waitFor()
            }
        }

        val command = mutableListOf<String>().apply {
            add(gradlewFile.absolutePath)
            addAll(tasks)
        }

        val process = ProcessBuilder(command)
            .directory(projectDir)
            .redirectErrorStream(true)
            .start()

        return process.waitFor()
    }
}