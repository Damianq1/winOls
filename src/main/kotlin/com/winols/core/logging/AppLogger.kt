package com.winols.core.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object AppLogger {
    inline fun <reified T> getLogger(): Logger {
        return LoggerFactory.getLogger(T::class.java)
    }

    private val rootLogger: Logger = LoggerFactory.getLogger("WinOlsApp")

    fun d(message: String) {
        rootLogger.debug(message)
    }

    fun i(message: String) {
        rootLogger.info(message)
    }

    fun w(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            rootLogger.warn(message, throwable)
        } else {
            rootLogger.warn(message)
        }
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            rootLogger.error(message, throwable)
        } else {
            rootLogger.error(message)
        }
    }
}