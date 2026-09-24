package com.winols.app.data

import com.winols.app.model.BitWidth
import com.winols.app.model.DataFormatConfig
import com.winols.app.model.DisplayRadix
import com.winols.app.model.Signedness
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Silnik dekodowania i formatowania surowych bajtów zgodnie z DataFormatConfig.
 */
object DataDecoder {

    fun readRawValue(buffer: ByteBuffer, offset: Int, config: DataFormatConfig): Long {
        if (offset < 0 || offset + config.bitWidth.bytes > buffer.capacity()) {
            return 0L
        }

        buffer.order(config.byteOrder)
        return when (config.bitWidth) {
            BitWidth.BIT_8 -> {
                val b = buffer.get(offset)
                if (config.signedness == Signedness.SIGNED) b.toLong() else (b.toInt() and 0xFF).toLong()
            }
            BitWidth.BIT_16 -> {
                val s = buffer.getShort(offset)
                if (config.signedness == Signedness.SIGNED) s.toLong() else (s.toInt() and 0xFFFF).toLong()
            }
            BitWidth.BIT_32 -> {
                val i = buffer.getInt(offset)
                if (config.signedness == Signedness.SIGNED) i.toLong() else (i.toLong() and 0xFFFFFFFFL)
            }
        }
    }

    fun toPhysicalValue(rawValue: Long, config: DataFormatConfig): Double {
        return (rawValue * config.factor) + config.offset
    }

    fun formatToString(rawValue: Long, config: DataFormatConfig): String {
        return when (config.radix) {
            DisplayRadix.HEX -> {
                val hexChars = config.bitWidth.bytes * 2
                val mask = (1L shl (config.bitWidth.bytes * 8)) - 1
                val maskedVal = rawValue and mask
                val hexStr = maskedVal.toString(16).uppercase()
                hexStr.padStart(hexChars, '0')
            }
            DisplayRadix.DECIMAL -> {
                if (config.factor != 1.0 || config.offset != 0.0) {
                    "%.2f".format(toPhysicalValue(rawValue, config))
                } else {
                    rawValue.toString()
                }
            }
            DisplayRadix.BINARY -> {
                val bitCount = config.bitWidth.bytes * 8
                val mask = (1L shl bitCount) - 1
                (rawValue and mask).toString(2).padStart(bitCount, '0')
            }
        }
    }
}