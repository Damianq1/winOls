package com.winols.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Zapewnia w pełni asynchroniczną i nieblokującą obsługę plików binarnych (.bin/.hex).
 */
class AsyncBinaryOperations(private val bufferManager: BinaryBufferManager) {

    suspend fun loadFileAsync(file: File): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            bufferManager.loadFromFile(file)
            bufferManager.size
        }
    }

    suspend fun loadStreamAsync(stream: InputStream, isHexFormat: Boolean = false): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            if (isHexFormat) {
                bufferManager.loadFromHexStream(stream)
            } else {
                val bytes = stream.use { it.readBytes() }
                bufferManager.loadFromBytes(bytes)
            }
            bufferManager.size
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

    suspend fun exportHexAsync(startAddress: Long = bufferManager.fileBaseAddress): String = withContext(Dispatchers.Default) {
        IntelHexCodec().encodeToHex(bufferManager.rawBuffer, startAddress)
    }

    suspend fun calculateDifferencesAsync(): List<Long> = withContext(Dispatchers.Default) {
        bufferManager.getDifferences()
    }
}