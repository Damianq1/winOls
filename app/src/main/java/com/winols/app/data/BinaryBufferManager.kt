package com.winols.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Zarządza buforem pamięci pliku binarnego (Flash/EEPROM).
 * Wykorzystuje bezpośredni bufor java.nio.ByteBuffer dla maksymalnej wydajności I/O.
 */
class BinaryBufferManager private constructor(
    private var buffer: ByteBuffer,
    var sourceFile: File? = null
) {
    var isModified: Boolean = false
        private set

    val capacity: Int
        get() = buffer.capacity()

    val currentOrder: ByteOrder
        get() = buffer.order()

    fun setByteOrder(order: ByteOrder) {
        buffer.order(order)
    }

    @Synchronized
    fun getByte(offset: Int): Byte {
        checkBounds(offset, 1)
        return buffer.get(offset)
    }

    @Synchronized
    fun getUByte(offset: Int): Int {
        return getByte(offset).toInt() and 0xFF
    }

    @Synchronized
    fun getShort(offset: Int, order: ByteOrder = buffer.order()): Short {
        checkBounds(offset, 2)
        val prevOrder = buffer.order()
        buffer.order(order)
        val value = buffer.getShort(offset)
        buffer.order(prevOrder)
        return value
    }

    @Synchronized
    fun getUShort(offset: Int, order: ByteOrder = buffer.order()): Int {
        return getShort(offset, order).toInt() and 0xFFFF
    }

    @Synchronized
    fun getInt(offset: Int, order: ByteOrder = buffer.order()): Int {
        checkBounds(offset, 4)
        val prevOrder = buffer.order()
        buffer.order(order)
        val value = buffer.getInt(offset)
        buffer.order(prevOrder)
        return value
    }

    @Synchronized
    fun getUInt(offset: Int, order: ByteOrder = buffer.order()): Long {
        return getInt(offset, order).toLong() and 0xFFFFFFFFL
    }

    @Synchronized
    fun putByte(offset: Int, value: Byte) {
        checkBounds(offset, 1)
        buffer.put(offset, value)
        isModified = true
    }

    @Synchronized
    fun putShort(offset: Int, value: Short, order: ByteOrder = buffer.order()) {
        checkBounds(offset, 2)
        val prevOrder = buffer.order()
        buffer.order(order)
        buffer.putShort(offset, value)
        buffer.order(prevOrder)
        isModified = true
    }

    @Synchronized
    fun putInt(offset: Int, value: Int, order: ByteOrder = buffer.order()) {
        checkBounds(offset, 4)
        val prevOrder = buffer.order()
        buffer.order(order)
        buffer.putInt(offset, value)
        buffer.order(prevOrder)
        isModified = true
    }

    @Synchronized
    fun readBytes(offset: Int, length: Int): ByteArray {
        checkBounds(offset, length)
        val dest = ByteArray(length)
        val dup = buffer.duplicate()
        dup.position(offset)
        dup.get(dest, 0, length)
        return dest
    }

    @Synchronized
    fun writeBytes(offset: Int, data: ByteArray) {
        checkBounds(offset, data.size)
        val dup = buffer.duplicate()
        dup.position(offset)
        dup.put(data)
        isModified = true
    }

    @Synchronized
    fun getDirectBufferView(): ByteBuffer {
        return buffer.asReadOnlyBuffer()
    }

    private fun checkBounds(offset: Int, length: Int) {
        if (offset < 0 || offset + length > buffer.capacity()) {
            throw IndexOutOfBoundsException(
                "Przekroczenie zakresu bufora: offset=$offset, długość=$length, rozmiar bufora=${buffer.capacity()}"
            )
        }
    }

    suspend fun saveToFile(targetFile: File = sourceFile ?: throw IllegalStateException("Brak docelowego pliku")): Unit =
        withContext(Dispatchers.IO) {
            val bytes = synchronized(this@BinaryBufferManager) {
                val data = ByteArray(buffer.capacity())
                val dup = buffer.duplicate()
                dup.position(0)
                dup.get(data)
                data
            }
            FileOutputStream(targetFile).use { fos ->
                fos.write(bytes)
            }
            synchronized(this@BinaryBufferManager) {
                sourceFile = targetFile
                isModified = false
            }
        }

    companion object {
        suspend fun loadFromFile(file: File, byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN): BinaryBufferManager =
            withContext(Dispatchers.IO) {
                val fileSize = file.length().toInt()
                val directBuffer = ByteBuffer.allocateDirect(fileSize)
                directBuffer.order(byteOrder)

                FileInputStream(file).use { fis ->
                    val channel = fis.channel
                    channel.read(directBuffer)
                }
                directBuffer.flip()
                BinaryBufferManager(directBuffer, file)
            }

        fun fromByteArray(data: ByteArray, byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN): BinaryBufferManager {
            val directBuffer = ByteBuffer.allocateDirect(data.size)
            directBuffer.order(byteOrder)
            directBuffer.put(data)
            directBuffer.flip()
            return BinaryBufferManager(directBuffer, null)
        }
    }
}