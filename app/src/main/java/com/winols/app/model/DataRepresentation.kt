package com.winols.app.model

import java.nio.ByteOrder

enum class BitWidth(val byteSize: Int) {
    BITS_8(1),
    BITS_16(2),
    BITS_32(4)
}

enum class SignMode {
    UNSIGNED,
    SIGNED
}

data class DataFormat(
    val bitWidth: BitWidth = BitWidth.BITS_16,
    val signMode: SignMode = SignMode.UNSIGNED,
    val byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN
) {
    fun readRawValue(buffer: ByteArray, offset: Int): Long {
        if (offset + bitWidth.byteSize > buffer.size) return 0L
        
        return when (bitWidth) {
            BitWidth.BITS_8 -> {
                val b0 = buffer[offset].toInt()
                if (signMode == SignMode.SIGNED) b0.toLong() else (b0 and 0xFF).toLong()
            }
            BitWidth.BITS_16 -> {
                val b0 = buffer[offset].toInt() and 0xFF
                val b1 = buffer[offset + 1].toInt() and 0xFF
                val raw16 = if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                    (b1 shl 8) or b0
                } else {
                    (b0 shl 8) or b1
                }
                if (signMode == SignMode.SIGNED) raw16.toShort().toLong() else raw16.toLong()
            }
            BitWidth.BITS_32 -> {
                val b0 = buffer[offset].toLong() and 0xFF
                val b1 = buffer[offset + 1].toLong() and 0xFF
                val b2 = buffer[offset + 2].toLong() and 0xFF
                val b3 = buffer[offset + 3].toLong() and 0xFF
                val raw32 = if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                    (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
                } else {
                    (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
                }
                if (signMode == SignMode.SIGNED) raw32.toInt().toLong() else raw32
            }
        }
    }

    fun writeRawValue(buffer: ByteArray, offset: Int, value: Long) {
        if (offset + bitWidth.byteSize > buffer.size) return
        
        when (bitWidth) {
            BitWidth.BITS_8 -> {
                buffer[offset] = (value and 0xFF).toByte()
            }
            BitWidth.BITS_16 -> {
                val b0 = (value and 0xFF).toByte()
                val b1 = ((value shr 8) and 0xFF).toByte()
                if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                    buffer[offset] = b0
                    buffer[offset + 1] = b1
                } else {
                    buffer[offset] = b1
                    buffer[offset + 1] = b0
                }
            }
            BitWidth.BITS_32 -> {
                val b0 = (value and 0xFF).toByte()
                val b1 = ((value shr 8) and 0xFF).toByte()
                val b2 = ((value shr 16) and 0xFF).toByte()
                val b3 = ((value shr 24) and 0xFF).toByte()
                if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                    buffer[offset] = b0
                    buffer[offset + 1] = b1
                    buffer[offset + 2] = b2
                    buffer[offset + 3] = b3
                } else {
                    buffer[offset] = b3
                    buffer[offset + 1] = b2
                    buffer[offset + 2] = b1
                    buffer[offset + 3] = b0
                }
            }
        }
    }
}