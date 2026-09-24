package com.winols.app.data

import com.winols.app.model.DataType
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BinaryBufferManager(fileSize: Int = 0) {
    private var buffer: ByteArray = ByteArray(fileSize)
    private var originalBackup: ByteArray = ByteArray(fileSize)

    val size: Int
        get() = buffer.size

    val rawBuffer: ByteArray
        get() = buffer

    fun loadFromFile(file: File) {
        val bytes = file.readBytes()
        loadFromBytes(bytes)
    }

    fun loadFromBytes(bytes: ByteArray) {
        buffer = bytes.copyOf()
        originalBackup = bytes.copyOf()
    }

    fun readValue(address: Long, type: DataType): Double {
        val idx = address.toInt()
        if (idx < 0 || idx + type.byteSize > buffer.size) {
            return 0.0
        }

        val bb = ByteBuffer.wrap(buffer, idx, type.byteSize)
        return when (type) {
            DataType.UBYTE -> (buffer[idx].toInt() and 0xFF).toDouble()
            DataType.SBYTE -> buffer[idx].toDouble()
            DataType.UWORD_LE -> {
                bb.order(ByteOrder.LITTLE_ENDIAN)
                (bb.short.toInt() and 0xFFFF).toDouble()
            }
            DataType.SWORD_LE -> {
                bb.order(ByteOrder.LITTLE_ENDIAN)
                bb.short.toDouble()
            }
            DataType.UWORD_BE -> {
                bb.order(ByteOrder.BIG_ENDIAN)
                (bb.short.toInt() and 0xFFFF).toDouble()
            }
            DataType.SWORD_BE -> {
                bb.order(ByteOrder.BIG_ENDIAN)
                bb.short.toDouble()
            }
            DataType.UDWORD_LE -> {
                bb.order(ByteOrder.LITTLE_ENDIAN)
                (bb.int.toLong() and 0xFFFFFFFFL).toDouble()
            }
            DataType.SDWORD_LE -> {
                bb.order(ByteOrder.LITTLE_ENDIAN)
                bb.int.toDouble()
            }
            DataType.UDWORD_BE -> {
                bb.order(ByteOrder.BIG_ENDIAN)
                (bb.int.toLong() and 0xFFFFFFFFL).toDouble()
            }
            DataType.SDWORD_BE -> {
                bb.order(ByteOrder.BIG_ENDIAN)
                bb.int.toDouble()
            }
        }
    }

    fun writeValue(address: Long, type: DataType, rawValue: Double) {
        val idx = address.toInt()
        if (idx < 0 || idx + type.byteSize > buffer.size) return

        val bb = ByteBuffer.allocate(type.byteSize)
        when (type) {
            DataType.UBYTE, DataType.SBYTE -> {
                buffer[idx] = rawValue.toInt().toByte()
            }
            DataType.UWORD_LE, DataType.SWORD_LE -> {
                bb.order(ByteOrder.LITTLE_ENDIAN).putShort(rawValue.toInt().toShort())
                System.arraycopy(bb.array(), 0, buffer, idx, 2)
            }
            DataType.UWORD_BE, DataType.SWORD_BE -> {
                bb.order(ByteOrder.BIG_ENDIAN).putShort(rawValue.toInt().toShort())
                System.arraycopy(bb.array(), 0, buffer, idx, 2)
            }
            DataType.UDWORD_LE, DataType.SDWORD_LE -> {
                bb.order(ByteOrder.LITTLE_ENDIAN).putInt(rawValue.toLong().toInt())
                System.arraycopy(bb.array(), 0, buffer, idx, 4)
            }
            DataType.UDWORD_BE, DataType.SDWORD_BE -> {
                bb.order(ByteOrder.BIG_ENDIAN).putInt(rawValue.toLong().toInt())
                System.arraycopy(bb.array(), 0, buffer, idx, 4)
            }
        }
    }

    fun getDifferences(): List<Long> {
        val diffList = mutableListOf<Long>()
        val minLen = minOf(buffer.size, originalBackup.size)
        for (i in 0 until minLen) {
            if (buffer[i] != originalBackup[i]) {
                diffList.add(i.toLong())
            }
        }
        return diffList
    }

    fun saveToFile(destination: File) {
        destination.writeBytes(buffer)
    }
}