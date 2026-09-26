package com.winols

import com.winols.core.logging.AppLogger

class WinOlsApplication {
    private val logger = AppLogger.getLogger<WinOlsApplication>()

    fun initialize() {
        AppLogger.i("Inicjalizacja modułów aplikacji WinOls...")
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            AppLogger.e("Nieobsługiwany wyjątek w wątku: ${thread.name}", throwable)
        }

        AppLogger.i("Aplikacja WinOls została pomyślnie uruchomiona.")
    }
}

fun main() {
    val app = WinOlsApplication()
    app.initialize()
}