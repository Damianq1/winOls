package com.winols.app.engine

import com.winols.app.model.*
import java.nio.ByteBuffer

class DataRepresentationEngine {

    /**
     * Ekstrakcja pojedynczej wartości o zadanym offsecie z uwzględnieniem konfiguracji endianness i signedness.
     */
    fun extractValue(buffer: ByteArray, offset: Int, config: DataFormatConfig): Number {
        val width = config.bitWidth.bytesCount
        if (offset < 0 || offset + width > buffer.size) {
            return 0
        }

        val bb = ByteBuffer.wrap(buffer, offset, width).order(config.endianness.byteOrder)
        return when (config.bitWidth) {
            BitWidth.BITS_8 -> {
                val b = buffer[offset]
                if (config.signedness == Signedness.SIGNED) b else b.toUByte().toInt()
            }
            BitWidth.BITS_16 -> {
                val s = bb.short
                if (config.signedness == Signedness.SIGNED) s else s.toUShort().toInt()
            }
            BitWidth.BITS_32 -> {
                val i = bb.int
                if (config.signedness == Signedness.SIGNED) i else i.toUInt().toLong()
            }
        }
    }

    /**
     * Konwersja bloku binarnego na serię punktów dla wykresu fali 2D.
     */
    fun extractWaveformData2D(
        buffer: ByteArray,
        startOffset: Int,
        lengthBytes: Int,
        config: DataFormatConfig
    ): FloatArray {
        val step = config.bitWidth.bytesCount
        val count = (lengthBytes / step).coerceAtMost((buffer.size - startOffset) / step)
        if (count <= 0) return FloatArray(0)

        val result = FloatArray(count)
        var cursor = startOffset

        for (i in 0 until count) {
            val raw = extractValue(buffer, cursor, config).toDouble()
            result[i] = ((raw * config.factor) + config.offset).toFloat()
            cursor += step
        }

        return result
    }

    /**
     * Ekstrakcja macierzy danych dla siatki tabeli / widoku 3D.
     */
    fun extractMatrix3D(
        buffer: ByteArray,
        startOffset: Int,
        rows: Int,
        cols: Int,
        config: DataFormatConfig
    ): Array<DoubleArray> {
        val step = config.bitWidth.bytesCount
        val matrix = Array(rows) { DoubleArray(cols) }
        var cursor = startOffset

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (cursor + step <= buffer.size) {
                    val raw = extractValue(buffer, cursor, config).toDouble()
                    matrix[r][c] = (raw * config.factor) + config.offset
                    cursor += step
                } else {
                    matrix[r][c] = 0.0
                }
            }
        }

        return matrix
    }

    /**
     * Formatowanie komórki widoku HEX Dump.
     */
    fun renderHexCell(buffer: ByteArray, offset: Int, config: DataFormatConfig): String {
        val value = extractValue(buffer, offset, config)
        return config.formatValue(value)
    }
}