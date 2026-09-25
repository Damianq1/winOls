package com.winols.app.domain.parser

import com.winols.app.domain.model.EcuMemoryBuffer
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.TreeMap

/**
 * Parser formatu Intel HEX (I8HEX, I16HEX, I32HEX).
 * Obsługuje ciągłe i fragmentaryczne mapy pamięci, wypełniając puste przestrzenie bajtem 0xFF.
 */
class IntelHexParser {

    fun parse(inputStream: InputStream, defaultPaddingByte: Byte = 0xFF.toByte()): EcuMemoryBuffer {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val memoryMap = TreeMap<Long, Byte>()

        var upperAddress = 0L

        reader.useLines { lines ->
            lines.forEachIndexed { index, rawLine ->
                val line = rawLine.trim()
                if (line.isEmpty()) return@forEachIndexed
                if (!line.startsWith(":")) {
                    throw IllegalArgumentException("Line ${index + 1}: Missing colon start code")
                }

                val byteCount = line.substring(1, 3).toInt(16)
                val offsetAddress = line.substring(3, 7).toInt(16).toLong()
                val recordType = line.substring(7, 9).toInt(16)
                val dataPayload = line.substring(9, 9 + byteCount * 2)
                val checksum = line.substring(9 + byteCount * 2, 9 + byteCount * 2 + 2).toInt(16)

                verifyChecksum(line.substring(1, 9 + byteCount * 2), checksum, index + 1)

                when (recordType) {
                    0x00 -> { // Data Record
                        val fullAddress = upperAddress or offsetAddress
                        for (i in 0 until byteCount) {
                            val byteVal = dataPayload.substring(i * 2, i * 2 + 2).toInt(16).toByte()
                            memoryMap[fullAddress + i] = byteVal
                        }
                    }
                    0x01 -> { // End of File Record
                        return@useLines
                    }
                    0x02 -> { // Extended Segment Address Record
                        upperAddress = (dataPayload.toInt(16).toLong() shl 4)
                    }
                    0x04 -> { // Extended Linear Address Record
                        upperAddress = (dataPayload.toInt(16).toLong() shl 16)
                    }
                    0x03, 0x05 -> {
                        // Start Segment/Linear Address Records (nie wpływają bezpośrednio na bufor pamięci ECU)
                    }
                    else -> throw UnsupportedOperationException("Line ${index + 1}: Unsupported record type 0x$recordType")
                }
            }
        }

        if (memoryMap.isEmpty()) {
            return EcuMemoryBuffer(ByteArray(0), 0L)
        }

        val minAddress = memoryMap.firstKey()
        val maxAddress = memoryMap.lastKey()
        val totalSize = (maxAddress - minAddress + 1).toInt()

        val fullByteArray = ByteArray(totalSize) { defaultPaddingByte }
        for ((address, byteValue) in memoryMap) {
            val targetOffset = (address - minAddress).toInt()
            fullByteArray[targetOffset] = byteValue
        }

        return EcuMemoryBuffer(fullByteArray, minAddress)
    }

    private fun verifyChecksum(payloadHex: String, expectedChecksum: Int, lineNumber: Int) {
        var sum = 0
        for (i in payloadHex.indices step 2) {
            sum += payloadHex.substring(i, i + 2).toInt(16)
        }
        val calculatedChecksum = ((-sum) and 0xFF)
        if (calculatedChecksum != expectedChecksum) {
            throw IllegalStateException("Line $lineNumber: Checksum mismatch. Calc: 0x${calculatedChecksum.toString(16)}, Expected: 0x${expectedChecksum.toString(16)}")
        }
    }
}