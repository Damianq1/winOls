package com.winols.app.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AsyncBinaryOperations(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun loadFile(file: File): Result<BinaryBufferManager> = withContext(dispatcher) {
        runCatching {
            BinaryBufferManager.fromFile(file)
        }
    }

    suspend fun saveFile(bufferManager: BinaryBufferManager, file: File): Result<Unit> = withContext(dispatcher) {
        runCatching {
            bufferManager.writeToFile(file)
        }
    }
}