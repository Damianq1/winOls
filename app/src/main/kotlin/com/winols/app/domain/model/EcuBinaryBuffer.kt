package com.winols.app.domain.model

class EcuBinaryBuffer(val rawBytes: ByteArray) {
    val size: Int get() = rawBytes.size

    fun readByte(offset: Int): UByte {
        require(offset in 0 until size) { "Przekroczono zakres bufora ECU." }
        return rawBytes[offset].toUByte()
    }

    fun readWord16(offset: Int, littleEndian: Boolean = false): UShort {
        val b0 = readByte(offset).toInt()
        val b1 = readByte(offset + 1).toInt()
        return if (littleEndian) {
            ((b1 shl 8) or b0).toUShort()
        } else {
            ((b0 shl 8) or b1).toUShort()
        }
    }

    fun getChunk(offset: Int, length: Int): ByteArray {
        val actualLength = minOf(length, size - offset)
        return rawBytes.copyOfRange(offset, offset + actualLength)
    }

    fun calculateChecksum32(): Long {
        var sum = 0L
        for (b in rawBytes) {
            sum = (sum + (b.toInt() and 0xFF)) and 0xFFFFFFFFL
        }
        return sum
    }
}