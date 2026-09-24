package com.winols.app.data

import com.winols.app.model.BitDepth
import com.winols.app.model.DataRepresentation
import com.winols.app.model.ValueType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class BinaryBufferManager {
    private var buffer: ByteBuffer? = null
    private var originalSnapshot: ByteArray? = null
    var capacity: Int = 0
        private set

    suspend fun loadFile(file: File): Boolean = withContext(Dispatchers.IO) {
        val fileLength = file.length()
        if (fileLength <= 0 || fileLength > 32 * 1024 * 1024) return@withContext false

        RandomAccessFile(file, "r").use { raf ->
            val channel = raf.channel
            val directBuf = ByteBuffer.allocateDirect(fileLength.toInt())
            while (directBuf.hasRemaining()) {
                if (channel.read(directBuf) == -1) break
            }
            directBuf.flip()
            buffer = directBuf
            capacity = fileLength.toInt()

            val snapshot = ByteArray(capacity)
            directBuf.position(0)
            directBuf.get(snapshot)
            directBuf.position(0)
            originalSnapshot = snapshot
        }
        true
    }

    suspend fun loadBytes(bytes: ByteArray): Unit = withContext(Dispatchers.Default) {
        val directBuf = ByteBuffer.allocateDirect(bytes.size)
        directBuf.put(bytes)
        directBuf.flip()
        buffer = directBuf
        capacity = bytes.size
        originalSnapshot = bytes.clone()
    }

    fun readRawValue(address: Int, representation: DataRepresentation): Long {
        val buf = buffer ?: throw IllegalStateException("Buffer not initialized")
        if (address < 0 || address + representation.bitDepth.bytesPerElement > capacity) {
            throw IndexOutOfBoundsException("Address $address out of buffer bounds ($capacity)")
        }

        buf.order(representation.byteOrder)
        return when (representation.bitDepth) {
            BitDepth.BITS_8 -> {
                val b = buf.get(address)
                if (representation.valueType == ValueType.SIGNED) b.toLong() else (b.toInt() and 0xFF).toLong()
            }
            BitDepth.BITS_16 -> {
                val s = buf.getShort(address)
                if (representation.valueType == ValueType.SIGNED) s.toLong() else (s.toInt() and 0xFFFF).toLong()
            }
            BitDepth.BITS_32 -> {
                val i = buf.getInt(address)
                if (representation.valueType == ValueType.SIGNED) i.toLong() else (i.toLong() and 0xFFFFFFFFL)
            }
        }
    }

    fun writeRawValue(address: Int, value: Long, representation: DataRepresentation) {
        val buf = buffer ?: throw IllegalStateException("Buffer not initialized")
        if (address < 0 || address + representation.bitDepth.bytesPerElement > capacity) {
            throw IndexOutOfBoundsException("Address $address out of buffer bounds ($capacity)")
        }

        buf.order(representation.byteOrder)
        when (representation.bitDepth) {
            BitDepth.BITS_8 -> buf.put(address, value.toByte())
            BitDepth.BITS_16 -> buf.putShort(address, value.toShort())
            BitDepth.BITS_32 -> buf.putInt(address, value.toInt())
        }
    }

    fun readBlock(address: Int, size: Int): ByteArray {
        val buf = buffer ?: throw IllegalStateException("Buffer not initialized")
        val safeSize = Math.max(0, Math.min(size, capacity - address))
        val output = ByteArray(safeSize)
        val duplicate = buf.duplicate()
        duplicate.position(address)
        duplicate.get(output, 0, safeSize)
        return output
    }

    fun writeBlock(address: Int, data: ByteArray) {
        val buf = buffer ?: throw IllegalStateException("Buffer not initialized")
        if (address < 0 || address + data.size > capacity) {
            throw IndexOutOfBoundsException("Block out of range at $address size ${data.size}")
        }
        val duplicate = buf.duplicate()
        duplicate.position(address)
        duplicate.put(data)
    }

    fun isModified(address: Int, length: Int): Boolean {
        val snapshot = originalSnapshot ?: return false
        val buf = buffer ?: return false
        if (address < 0 || address + length > capacity) return false

        for (i in 0 until length) {
            if (buf.get(address + i) != snapshot[address + i]) return true
        }
        return false
    }

    suspend fun saveToFile(file: File): Boolean = withContext(Dispatchers.IO) {
        val buf = buffer ?: return@withContext false
        RandomAccessFile(file, "rw").use { raf ->
            val channel = raf.channel
            val duplicate = buf.duplicate()
            duplicate.position(0)
            channel.truncate(capacity.toLong())
            while (duplicate.hasRemaining()) {
                channel.write(duplicate)
            }
        }
        val newSnapshot = ByteArray(capacity)
        val duplicate = buf.duplicate()
        duplicate.position(0)
        duplicate.get(newSnapshot)
        originalSnapshot = newSnapshot
        true
    }
}