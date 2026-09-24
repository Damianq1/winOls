package com.winols.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Zapewnia asynchroniczne i bezpieczne dla wątku operacje wejścia/wyjścia
 * na plikach binarnych Flash / EEPROM.
 */
class AsyncBinaryOperations(private val bufferManager: BinaryBufferManager) {

    suspend fun loadFileAsync(file: File): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = file.readBytes()
            bufferManager.loadFromBytes(bytes)
            bytes.size
        }
    }

    suspend fun loadStreamAsync(stream: InputStream): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = stream.use { it.readBytes() }
            bufferManager.loadFromBytes(bytes)
            bytes.size
        }
    }

    suspend fun saveToFileAsync(destination: File): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            bufferManager.saveToFile(destination)
        }
    }

    suspend fun saveToStreamAsync(outputStream: OutputStream): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            outputStream.use { it.write(bufferManager.rawBuffer) }
        }
    }
}