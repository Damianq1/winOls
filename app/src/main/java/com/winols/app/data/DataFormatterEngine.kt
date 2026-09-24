package com.winols.app.data

import com.winols.app.model.DataRepresentation
import java.nio.ByteOrder

/**
 * Bezstanowy silnik konwersji pomiędzy surowymi bajtami a formatem HEX / wartościami fizycznymi.
 */
object DataFormatterEngine {

    fun parseRawValue(
        buffer: BinaryBufferManager,
        offset: Int,
        representation: DataRepresentation
    ): Long {
        val isLE = representation.byteOrder == ByteOrder.LITTLE_ENDIAN
        return when (representation.bitWidth) {
            8 -> {
                val b = buffer.getByte(offset).toLong()
                if (representation.isSigned) b else (b and 0xFFL)
            }
            16 -> {
                val b0 = buffer.getByte(offset).toLong() and 0xFFL
                val b1 = buffer.getByte(offset + 1).toLong() and 0xFFL
                val raw = if (isLE) (b1 shl 8) or b0 else (b0 shl 8) or b1
                if (representation.isSigned) raw.toShort().toLong() else raw
            }
            32 -> {
                val b0 = buffer.getByte(offset).toLong() and 0xFFL
                val b1 = buffer.getByte(offset + 1).toLong() and 0xFFL
                val b2 = buffer.getByte(offset + 2).toLong() and 0xFFL
                val b3 = buffer.getByte(offset + 3).toLong() and 0xFFL
                val raw = if (isLE) {
                    (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
                } else {
                    (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
                }
                if (representation.isSigned) raw.toInt().toLong() else raw
            }
            else -> throw IllegalArgumentException("Nieobsługiwana szerokość bitowa: ${representation.bitWidth}")
        }
    }

    fun rawToPhysical(rawValue: Long, factor: Double, offset: Double): Double {
        return (rawValue * factor) + offset
    }

    fun physicalToRaw(physicalValue: Double, factor: Double, offset: Double): Long {
        if (factor == 0.0) return 0L
        return Math.round((physicalValue - offset) / factor)
    }

    fun encodeValue(
        value: Long,
        bitWidth: Int,
        byteOrder: ByteOrder
    ): ByteArray {
        val isLE = byteOrder == ByteOrder.LITTLE_ENDIAN
        return when (bitWidth) {
            8 -> byteArrayOf(value.toByte())
            16 -> {
                val b0 = (value and 0xFFL).toByte()
                val b1 = ((value shr 8) and 0xFFL).toByte()
                if (isLE) byteArrayOf(b0, b1) else byteArrayOf(b1, b0)
            }
            32 -> {
                val b0 = (value and 0xFFL).toByte()
                val b1 = ((value shr 8) and 0xFFL).toByte()
                val b2 = ((value shr 16) and 0xFFL).toByte()
                val b3 = ((value shr 24) and 0xFFL).toByte()
                if (isLE) byteArrayOf(b0, b1, b2, b3) else byteArrayOf(b3, b2, b1, b0)
            }
            else -> throw IllegalArgumentException("Nieobsługiwana szerokość bitowa: $bitWidth")
        }
    }

    fun formatAsHex(buffer: BinaryBufferManager, offset: Int, length: Int): String {
        val bytes = buffer.getBytes(offset, length)
        val sb = java.lang.StringBuilder(bytes.size * 3)
        for (b in bytes) {
            sb.append(String.format("%02X ", b))
        }
        return sb.toString().trimEnd()
    }
}