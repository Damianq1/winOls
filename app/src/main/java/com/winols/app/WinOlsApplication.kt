package com.winols.app

import android.app.Application
import com.winols.BuildConfig
import timber.log.Timber

class WinOlsApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        initLogging()
    }

    private fun initLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
        Timber.i("WinOlsApplication zainicjalizowana pomyślnie.")
    }

    private class ReleaseTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            // W środowisku produkcyjnym odfiltrowujemy verbose i debug
            if (priority == android.util.Log.VERBOSE || priority == android.util.Log.DEBUG) {
                return
            }
            // Miejsce na integrację z Crashlytics/zewnętrznym serwisem błędów
        }
    }
}