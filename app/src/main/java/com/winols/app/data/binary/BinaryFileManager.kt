package com.winols.app.data.binary

import com.winols.app.core.dispatcher.CoroutineDispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

class BinaryFileManager(
    private val dispatchers: CoroutineDispatchers
) {
    /**
     * Wczytuje plik binarny pamięciowo (Memory-Mapped I/O) w Dispatchers.IO,
     * zapewniając natychmiastowy dostęp do dużych wsadów ECU bez zapychania pamięci RAM.
     */
    suspend fun loadBinary(file: File): ByteArray = withContext(dispatchers.io) {
        RandomAccessFile(file, "r").use { raf ->
            val channel = raf.channel
            val buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())
            val bytes = ByteArray(channel.size().toInt())
            buffer.get(bytes)
            bytes
        }
    }

    /**
     * Bezpieczny asynchroniczny zapis zmodyfikowanego wsadu.
     */
    suspend fun saveBinary(file: File, data: ByteArray) = withContext(dispatchers.io) {
        file.outputStream().use { fos ->
            fos.write(data)
            fos.flush()
        }
    }
}