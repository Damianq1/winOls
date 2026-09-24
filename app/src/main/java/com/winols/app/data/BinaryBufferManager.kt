package com.winols.app.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class BinaryBufferManager(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private var buffer: ByteBuffer? = null
    private val lock = ReentrantReadWriteLock()

    suspend fun loadFile(file: File): Result<Int> = withContext(ioDispatcher) {
        runCatching {
            RandomAccessFile(file, "r").use { raf ->
                val channel = raf.channel
                val size = channel.size().toInt()
                val memoryBuffer = ByteBuffer.allocateDirect(size).order(ByteOrder.LITTLE_ENDIAN)
                channel.read(memoryBuffer)
                memoryBuffer.flip()

                lock.write {
                    buffer = memoryBuffer
                }
                size
            }
        }
    }

    suspend fun saveFile(targetFile: File): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            lock.readOto implementacja asynchronicznych operacji binarnych, odczytu/zapisu plików oraz skanowania pamięci z wykorzystaniem Kotlin Coroutines (`Dispatchers.IO` oraz `Dispatchers.Default`), zapewniająca pełną responsywność głównego wątku UI.

### app/src/main/java/com/winols/app/data/BinaryBufferManager.kt