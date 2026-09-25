package com.winols.app.domain.model

import java.nio.ByteOrder

/**
 * Niskopoziomowy bufor pamięci wsadu ECU z obsługą przesunięcia bazowego i porządku bajtów.
 */
class EcuMemoryBuffer(
    val rawBytes: ByteArray,
    val baseAddress: Long = 0L,
    var byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN
) {
    val size: Int get() = rawBytes.size

    fun getByte(address: Long): Int {
        val offset = toOffset(address)
        return rawBytes[offset].toInt() and 0xFF
    }

    fun setByte(address: Long, value: Int) {
        val offset = toOffset(address)
        rawBytes[offset] = (value and 0xFF).toByte()
    }

    fun getWord(address: Long): Int {
        val offset = toOffset(address)
        require(offset + 1 < rawBytes.size) { "Out of bounds read at offset $offset" }
        val b0 = rawBytes[offset].toInt() and 0xFF
        val b1 = rawBytes[offset + 1].toInt() and 0xFF
        return if (byteOrder == ByteOrder.BIG_ENDIAN) {
            (b0 shl 8) or b1
        } else {
            (b1 shl 8) or b0
        }
    }

    fun setWord(address: Long, value: Int) {
        val offset = toOffset(address)
        require(offset + 1 < rawBytes.size) { "Out of bounds write at offset $offset" }
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            rawBytes[offset] = ((value shr 8) and 0xFF).toByte()
            rawBytes[offset + 1] = (value and 0xFF).toByte()
        } else {
            rawBytes[offset] = (value and 0xFF).toByte()
            rawBytes[offset + 1] = ((value shr 8) and 0xFF).toByte()
        }
    }

    fun getDWord(address: Long): Long {
        val offset = toOffset(address)
        require(offset + 3 < rawBytes.size) { "Out of bounds read at offset $offset" }
        val b0 = (rawBytes[offset].toLong() and 0xFF)
        val b1 = (rawBytes[offset + 1].toLong() and 0xFF)
        val b2 = (rawBytes[offset + 2].toLong() and 0xFF)
        val b3 = (rawBytes[offset + 3].toLong() and 0xFF)

        return if (byteOrder == ByteOrder.BIG_ENDIAN) {
            (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
        } else {
            (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
        }
    }

    fun getSubArray(startAddress: Long, length: Int): ByteArray {
        val offset = toOffset(startAddress)
        require(offset + length <= rawBytes.size) { "Range exceeds buffer size" }
        return rawBytes.copyOfRange(offset, offset + length)
    }

    private fun toOffset(address: Long): Int {
        val offset = (address - baseAddress).toInt()
        require(offset in 0 until rawBytes.size) {
            "Address 0x${address.toString(16).uppercase()} out of bounds [0x${baseAddress.toString(16)}..0x${(baseAddress + rawBytes.size - 1).toString(16)}]"
        }
        return offset
    }
}