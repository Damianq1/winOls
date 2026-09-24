package com.winols.app.data

import com.winols.app.model.DataType
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BinaryBufferManager(private val initialCapacity: Int = 0) {
    private var buffer: ByteArray = ByteArray(initialCapacity)
    val size: Int get() = buffer.size

    fun loadFromFile(file: File) {
        buffer = file.readBytes()
    }

    fun loadFromBytes(bytes: ByteArray) {
        buffer = bytes.copyOf()
    }

    fun getRawBytes(): ByteArray = buffer.copyOf()

    fun readValue(address: Int, dataType: DataType): Double {
        require(address >= 0 && address + dataType.byteSize <= buffer.size) {
            "Address out of bounds: $address"
        }
        val bb = ByteBuffer.wrap(buffer, address, dataType.byteSize)
        return when (dataType) {
            DataType.UBYTE -> (buffer[address].toInt() and 0xFF).toDouble()
            DataType.SBYTE -> buffer[address].toDouble()
            DataType.UWORD_LE -> bb.order(ByteOrder.LITTLE_ENDIAN).short.toInt().and(0xFFFF).toDouble()
            DataType.UWORD_BE -> bb.order(ByteOrder.BIG_ENDIAN).short.toInt().and(0xFFFF).toDouble()
            DataType.SWORD_LE -> bb.order(ByteOrder.LITTLE_ENDIAN).short.toDouble()
            DataType.SWORD_BE -> bb.order(ByteOrder.BIG_ENDIAN).short.toDouble()
            DataType.UDWORD_LE -> bb.order(ByteOrder.LITTLE_ENDIAN).int.toLong().and(0xFFFFFFFFL).toDouble()
            DataType.UDWORD_BE -> bb.order(ByteOrder.BIG_ENDIAN).int.toLong().and(0xFFFFFFFFL).toDouble()
            DataType.SDWORD_LE -> bb.order(ByteOrder.LITTLE_ENDIAN).int.toDouble()
            DataType.SDWORD_BE -> bb.order(ByteOrder.BIG_ENDIAN).int.toDouble()
            DataType.FLOAT_LE -> bb.order(ByteOrder.LITTLE_ENDIAN).float.toDouble()
            DataType.FLOAT_BE -> bb.order(ByteOrder.BIG_ENDIAN).float.toDouble()
        }
    }

    fun writeValue(address: Int, dataType: DataType, value: Double) {
        require(address >= 0 && address + dataType.byteSize <= buffer.size) {
            "Address out of bounds: $address"
        }
        val bb = ByteBuffer.wrap(buffer, address, dataType.byteSize)
        when (dataType) {
            DataType.UBYTE -> buffer[address] = (value.toInt() and 0xFF).toByte()
            DataType.SBYTE -> buffer[address] = value.toInt().toByte()
            DataType.UWORD_LE -> bb.order(ByteOrder.LITTLE_ENDIAN).putShort(value.toInt().toShort())
            DataType.UWORD_BE -> bb.order(ByteOrder.BIG_ENDIAN).putShort(value.toInt().toShort())
            DataType.SWORD_LE -> bb.order(ByteOrder.LITTLE_ENDIAN).putShort(value.toInt().toShort())
            DataType.SWORD_BE -> bb.order(ByteOrder.BIG_ENDIAN).putShort(value.toInt().toShort())
            DataType.UDWORD_LE -> bb.order(ByteOrder.LITTLE_ENDIAN).putInt(value.toLong().toInt())
            DataType.UDWORD_BE -> bb.order(ByteOrder.BIG_ENDIAN).putInt(value.toLong().toInt())
            DataType.SDWORD_LE -> bb.order(ByteOrder.LITTLE_ENDIAN).putInt(value.toInt())
            DataType.SDWORD_BE -> bb.order(ByteOrder.BIG_ENDIAN).putInt(value.toInt())
            DataType.FLOAT_LE -> bb.order(ByteOrder.LITTLE_ENDIAN).putFloat(value.toFloat())
            DataType.FLOAT_BE -> bb.order(ByteOrder.BIG_ENDIAN).putFloat(value.toFloat())
        }
    }

    fun readBlock(address: Int, length: Int): ByteArray {
        require(address >= 0 && address + length <= buffer.size) { "Block out of bounds" }
        return buffer.copyOfRange(address, address + length)
    }

    fun writeBlock(address: Int, data: ByteArray) {
        require(address >= 0 && address + data.size <= buffer.size) { "Block write out of bounds" }
        System.arraycopy(data, 0, buffer, address, data.size)
    }
}