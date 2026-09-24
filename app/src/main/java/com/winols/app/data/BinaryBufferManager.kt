package com.winols.app.data

import com.winols.app.model.DataType
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BinaryBufferManager(private var buffer: ByteArray) {

    constructor(size: Int) : this(ByteArray(size))

    val size: Int
        get() = buffer.size

    fun getRawBytes(): ByteArray = buffer.copyOf()

    fun loadFromFile(file: File) {
        buffer = file.readBytes()
    }

    fun saveToFile(file: File) {
        file.writeBytes(buffer)
    }

    fun readValue(address: Int, type: DataType): Double {
        if (address < 0 || address + type.byteSize > buffer.size) {
            throw IndexOutOfBoundsException("Address 0x${address.toString(16)} out of bounds for buffer size ${buffer.size}")
        }

        val bb = ByteBuffer.wrap(buffer, address, type.byteSize)
        return when (type) {
            DataType.UBYTE -> (buffer[address].toInt() and 0xFF).toDouble()
            DataType.SBYTE -> buffer[address].toDouble()
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
            DataType.ULONG_LE -> {
                bb.order(ByteOrder.LITTLE_ENDIAN)
                (bb.int.toLong() and 0xFFFFFFFFL).toDouble()
            }
            DataType.SLONG_LE -> {
                bb.order(ByteOrder.LITTLE_ENDIAN)
                bb.int.toDouble()
            }
            DataType.ULONG_BE -> {
                bb.order(ByteOrder.BIG_ENDIAN)
                (bb.int.toLong() and 0xFFFFFFFFL).toDouble()
            }
            DataType.SLONG_BE -> {
                bb.order(ByteOrder.BIG_ENDIAN)
                bb.int.toDouble()
            }
        }
    }

    fun writeValue(address: Int, type: DataType, rawValue: Double) {
        if (address < 0 || address + type.byteSize > buffer.size) {
            throw IndexOutOfBoundsException("Address 0x${address.toString(16)} out of bounds for buffer size ${buffer.size}")
        }

        val bb = ByteBuffer.wrap(buffer, address, type.byteSize)
        when (type) {
            DataType.UBYTE -> buffer[address] = rawValue.toInt().toByte()
            DataType.SBYTE -> buffer[address] = rawValue.toInt().toByte()
            DataType.UWORD_LE -> {
                bb.order(ByteOrder.LITTLE_ENDIAN)
                bb.putShort(rawValue.toInt().toShort())
            }
            DataType.SWORD_LE -> {
                bb.order(ByteOrder.LITTLE_ENDIAN)
                bb.putShort(rawValue.toInt().toShort())
            }
            DataType.UWORD_BE -> {
                bb.order(ByteOrder.BIG_ENDIAN)
                bb.putShort(rawValue.toInt().toShort())
            }
            DataType.SWORD_BE -> {
                bb.order(ByteOrder.BIG_ENDIAN)
                bb.putShort(rawValue.toInt().toShort())
            }
            DataType.ULONG_LE -> {
                bb.order(ByteOrder.LITTLE_ENDIAN)
                bb.putInt(rawValue.toLong().toInt())
            }
            DataType.SLONG_LE -> {
                bb.order(ByteOrder.LITTLE_ENDIAN)
                bb.putInt(rawValue.toInt())
            }
            DataType.ULONG_BE -> {
                bb.order(ByteOrder.BIG_ENDIAN)
                bb.putInt(rawValue.toLong().toInt())
            }
            DataType.SLONG_BE -> {
                bb.order(ByteOrder.BIG_ENDIAN)
                bb.putInt(rawValue.toInt())
            }
        }
    }
}