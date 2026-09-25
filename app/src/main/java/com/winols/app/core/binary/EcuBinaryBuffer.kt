package com.winols.app.core.binary

import java.nio.ByteBuffer
import java.nio.ByteOrder as JavaByteOrder

/**
 * Zoptymalizowany bufor w pamięci do odczytu i manipulacji wsadami ECU (Flash/EEPROM).
 */
class EcuBinaryBuffer(private val rawData: ByteArray) {

    val size: Int get() = rawData.size

    fun getRawBytes(): ByteArray = rawData.copyOf()

    fun readValue(offset: Int, type: ValueType, endianness: DataEndianness): Double {
        if (offset + type.byteSize > rawData.size || offset < 0) {
            throw IndexOutOfBoundsException("Błąd adresowania: Offset 0x${offset.toString(16).uppercase()} poza rozmiarem pliku (0x${rawData.size.toString(16).uppercase()})")
        }

        val buffer = ByteBuffer.wrap(rawData, offset, type.byteSize).apply {
            order(if (endianness == DataEndianness.BIG_ENDIAN) JavaByteOrder.BIG_ENDIAN else JavaByteOrder.LITTLE_ENDIAN)
        }

        return when (type) {
            ValueType.UBYTE -> (buffer.get().toInt() and 0xFF).toDouble()
            ValueType.SBYTE -> buffer.get().toDouble()
            ValueType.UWORD_16 -> (buffer.short.toInt() and 0xFFFF).toDouble()
            ValueType.SWORD_16 -> buffer.short.toDouble()
            ValueType.UDWORD_32 -> (buffer.int.toLong() and 0xFFFFFFFFL).toDouble()
            ValueType.SDWORD_32 -> buffer.int.toDouble()
        }
    }

    fun writeValue(offset: Int, value: Double, type: ValueType, endianness: DataEndianness) {
        if (offset + type.byteSize > rawData.size || offset < 0) {
            throw IndexOutOfBoundsException("Błąd zapisu: Offset 0x${offset.toString(16).uppercase()} poza zakresem")
        }

        val buffer = ByteBuffer.allocate(type.byteSize).apply {
            order(if (endianness == DataEndianness.BIG_ENDIAN) JavaByteOrder.BIG_ENDIAN else JavaByteOrder.LITTLE_ENDIAN)
        }

        when (type) {
            ValueType.UBYTE -> buffer.put((value.toInt() and 0xFF).toByte())
            ValueType.SBYTE -> buffer.put(value.toInt().toByte())
            ValueType.UWORD_16 -> buffer.putShort((value.toInt() and 0xFFFF).toShort())
            ValueType.SWORD_16 -> buffer.putShort(value.toInt().toShort())
            ValueType.UDWORD_32 -> buffer.putInt((value.toLong() and 0xFFFFFFFFL).toInt())
            ValueType.SDWORD_32 -> buffer.putInt(value.toInt())
        }

        System.arraycopy(buffer.array(), 0, rawData, offset, type.byteSize)
    }

    fun calculateChecksum16(startOffset: Int, endOffset: Int): Int {
        var sum = 0
        for (i in startOffset until endOffset step 2) {
            val high = rawData[i].toInt() and 0xFF
            val low = if (i + 1 < endOffset) rawData[i + 1].toInt() and 0xFF else 0
            sum = (sum + ((high shl 8) or low)) and 0xFFFF
        }
        return sum
    }
}