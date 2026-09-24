package com.winols.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Asynchroniczne operacje wejścia/wyjścia (I/O) gwarantujące
 * brak obciążenia i blokowania głównego wątku UI.
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

    suspend fun calculateDifferencesAsync(): List<Long> = withContext(Dispatchers.Default) {
        bufferManager.getDifferences()
    }
}