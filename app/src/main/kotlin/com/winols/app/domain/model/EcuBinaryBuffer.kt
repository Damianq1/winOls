package com.winols.app.domain.model

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Reprezentuje bufor binarny ECU z obsługą endianowości i odczytu typów stosowanych w mapach (8-bit, 16-bit, 32-bit).
 */
class EcuBinaryBuffer(val rawBytes: ByteArray) {

    val size: Int get() = rawBytes.size
    private val buffer: ByteBuffer = ByteBuffer.wrap(rawBytes)

    fun readU8(offset: Int): Int {
        return rawBytes[offset].toInt() and 0xFF
    }

    fun readU16(offset: Int, endian: ByteOrder = ByteOrder.BIG_ENDIAN): Int {
        buffer.order(endian)
        return buffer.getShort(offset).toInt() and 0xFFFF
    }

    fun read32(offset: Int, endian: ByteOrder = ByteOrder.BIG_ENDIAN): Long {
        buffer.order(endian)
        return buffer.getInt(offset).toLong() and 0xFFFFFFFFL
    }

    fun getHexDumpPreview(maxBytes: Int = 512): String {
        val limit = minOf(rawBytes.size, maxBytes)
        val sb = StringBuilder()

        for (i in 0 until limit step 16) {
            sb.append(String.format("%06X: ", i))
            val lineBytes = minOf(16, limit - i)

            for (j in 0 until lineBytes) {
                sb.append(String.format("%02X ", rawBytes[i + j]))
            }
            for (j in lineBytes until 16) {
                sb.append("   ")
            }
            sb.append(" |")
            for (j in 0 until lineBytes) {
                val b = rawBytes[i + j].toInt() and 0xFF
                val ch = if (b in 32..126) b.toChar() else '.'
                sb.append(ch)
            }
            sb.append("|\n")
        }
        return sb.toString()
    }
}