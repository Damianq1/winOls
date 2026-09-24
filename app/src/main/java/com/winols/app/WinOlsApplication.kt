package com.winols.app

import android.app.Application
import java.io.File

/**
 * Punkt wejścia aplikacji Android konfigurujący środowisko pracy,
 * cache pamięci podręcznej i dostęp do instancji modelu w czasie działania aplikacji.
 */
class WinOlsApplication : Application() {

    var currentSession: ProjectSession? = null
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    fun openProject(file: File): ProjectSession {
        val model = BinModel.fromFile(file)
        model.initializeProject()

        val session = ProjectSession(
            id = file.name,
            binModel = model,
            originalSnapshot = model.bufferManager.getByteArray()
        )
        currentSession = session
        return session
    }

    fun closeProject() {
        currentSession = null
    }

    private fun BinModel.initializeProject() {
        this.scanAndRegisterMaps()
    }

    companion object {
        lateinit var instance: WinOlsApplication
            private set
    }
}