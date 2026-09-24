package com.winols.app.data

import com.winols.app.model.DataType
import com.winols.app.model.Endianness
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Konwerter i formatowanie reprezentacji binarnej (HEX / DEC / FLOAT).
 * Obsługuje formatowanie 8-bit, 16-bit, 32-bit ze znakiem lub bez oraz porządek bajtów HiLo / LoHi.
 */
object DataRepresentationFormatter {

    enum class DisplayMode {
        HEX,
        DECIMAL_RAW,
        DECIMAL_PHYSICAL
    }

    /**
     * Formatuje wartość z bufora pod wskazanym adresem do reprezentacji tekstowej.
     */
    fun formatValue(
        bufferManager: BinaryBufferManager,
        offset: Int,
        dataType: DataType,
        endianness: Endianness,
        mode: DisplayMode,
        factor: Double = 1.0,
        factorOffset: Double = 0.0
    ): String {
        val raw = bufferManager.readValue(offset, dataType, endianness)

        return when (mode) {
            DisplayMode.HEX -> formatAsHex(bufferManager, offset, dataType, endianness)
            DisplayMode.DECIMAL_RAW -> formatAsDecimalRaw(raw, dataType)
            DisplayMode.DECIMAL_PHYSICAL -> {
                val physical = (raw * factor) + factorOffset
                if (dataType == DataType.FLOAT32 || factor % 1.0 != 0.0) {
                    String.format("%.2f", physical)
                } else {
                    String.format("%.0f", physical)
                }
            }
        }
    }

    /**
     * Konwertuje bajty na zapis heksadecymalny z zachowaniem porządku bajtów (Hi/Lo).
     */
    fun formatAsHex(
        bufferManager: BinaryBufferManager,
        offset: Int,
        dataType: DataType,
        endianness: Endianness
    ): String {
        val bytes = bufferManager.readRawBytes(offset, dataType.byteSize)
        val orderedBytes = if (endianness == Endianness.LITTLE_ENDIAN) {
            bytes.reversedArray()
        } else {
            bytes
        }

        val sb = StringBuilder()
        for (b in orderedBytes) {
            sb.append(String.format("%02X", b.toInt() and 0xFF))
        }
        return sb.toString()
    }

    private fun formatAsDecimalRaw(value: Double, dataType: DataType): String {
        return when (dataType) {
            DataType.UINT8 -> (value.toInt() and 0xFF).toString()
            DataType.INT8 -> value.toInt().toByte().toString()
            DataType.UINT16 -> (value.toInt() and 0xFFFF).toString()
            DataType.INT16 -> value.toInt().toShort().toString()
            DataType.UINT32 -> (value.toLong() and 0xFFFFFFFFL).toString()
            DataType.INT32 -> value.toInt().toString()
            DataType.FLOAT32 -> String.format("%.3f", value)
        }
    }

    /**
     * Parsuje wprowadzony przez użytkownika ciąg znaków HEX na wartość numeryczną w oparciu o specyfikację typu.
     */
    fun parseHexInput(hexString: String, dataType: DataType, endianness: Endianness): Double {
        val cleanHex = hexString.trim().replace("0x", "", ignoreCase = true)
        val byteLen = dataType.byteSize
        val paddedHex = cleanHex.padStart(byteLen * 2, '0').takeLast(byteLen * 2)

        val rawBytes = ByteArray(byteLen)
        for (i in 0 until byteLen) {
            val byteStr = paddedHex.substring(i * 2, i * 2 + 2)
            rawBytes[i] = byteStr.toInt(16).toByte()
        }

        // Jeżeli w HEX wprowadzono wartość w notacji Big Endian (HiLo), a bufor operuje w Little Endian:
        val alignedBytes = if (endianness == Endianness.LITTLE_ENDIAN) {
            rawBytes.reversedArray()
        } else {
            rawBytes
        }

        val bb = ByteBuffer.wrap(alignedBytes).order(
            if (endianness == Endianness.LITTLE_ENDIAN) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN
        )

        return when (dataType) {
            DataType.UINT8 -> (bb.get().toInt() and 0xFF).toDouble()
            DataType.INT8 -> bb.get().toDouble()
            DataType.UINT16 -> (bb.short.toInt() and 0xFFFF).toDouble()
            DataType.INT16 -> bb.short.toDouble()
            DataType.UINT32 -> (bb.int.toLong() and 0xFFFFFFFFL).toDouble()
            DataType.INT32 -> bb.int.toDouble()
            DataType.FLOAT32 -> bb.float.toDouble()
        }
    }
}