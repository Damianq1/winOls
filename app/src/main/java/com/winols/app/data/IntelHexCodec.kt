package com.winols.app.data

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.util.Locale

/**
 * Obsługa standardu Intel HEX (.hex / .ihex / .mcs).
 * Obsługuje rekordy:
 * 00 - Data Record
 * 01 - End of File (EOF)
 * 02 - Extended Segment Address (16-bit shift)
 * 03 - Start Segment Address
 * 04 - Extended Linear Address (górne 16 bitów 32-bitowego adresu)
 * 05 - Start Linear Address
 */
class IntelHexCodec {

    data class HexParseResult(
        val binaryData: ByteArray,
        val baseAddress: Long,
        val highestAddress: Long,
        val totalBytes: Int
    )

    /**
     * Parsuje strumień tekstowy Intel HEX i zwraca ciągły bufor binarny.
     */
    fun decodeHex(inputStream: InputStream, defaultFillByte: Byte = 0xFF.toByte()): HexParseResult {
        val records = mutableListOf<HexRecord>()
        var extendedAddress = 0L
        var minAddr = Long.MAX_VALUE
        var maxAddr = 0L

        BufferedReader(InputStreamReader(inputStream, Charsets.US_ASCII)).useLines { lines ->
            for (line in lines) {
                val trimmed = line.trim()
                if (!trimmed.startsWith(":") || trimmed.length < 11) continue

                val byteCount = trimmed.substring(1, 3).toInt(16)
                val addressField = trimmed.substring(3, 7).toInt(16)
                val recordType = trimmed.substring(7, 9).toInt(16)

                // Weryfikacja sumy kontrolnej rekordu (Two's complement)
                val totalLengthInChars = 9 + (byteCount * 2) + 2
                if (trimmed.length < totalLengthInChars) continue

                var checksumCalc = 0
                for (i in 1 until totalLengthInChars - 2 step 2) {
                    checksumCalc += trimmed.substring(i, i + 2).toInt(16)
                }
                checksumCalc = (-checksumCalc) and 0xFF
                val fileChecksum = trimmed.substring(totalLengthInChars - 2, totalLengthInChars).toInt(16)
                if (checksumCalc != fileChecksum) {
                    continue // Uszkodzony rekord, pomijamy
                }

                when (recordType) {
                    0x00 -> { // Data Record
                        val fullAddress = extendedAddress + addressField
                        val data = ByteArray(byteCount)
                        for (b in 0 until byteCount) {
                            val byteIndex = 9 + (b * 2)
                            data[b] = trimmed.substring(byteIndex, byteIndex + 2).toInt(16).toByte()
                        }
                        records.add(HexRecord(fullAddress, data))
                        if (fullAddress < minAddr) minAddr = fullAddress
                        val endAddr = fullAddress + byteCount
                        if (endAddr > maxAddr) maxAddr = endAddr
                    }
                    0x01 -> { // EOF
                        break
                    }
                    0x02 -> { // Extended Segment Address
                        val seg = trimmed.substring(9, 13).toInt(16)
                        extendedAddress = (seg shl 4).toLong()
                    }
                    0x04 -> { // Extended Linear Address
                        val upper = trimmed.substring(9, 13).toInt(16)
                        extendedAddress = (upper.toLong() shl 16)
                    }
                }
            }
        }

        if (records.isEmpty() || minAddr >= maxAddr) {
            return HexParseResult(ByteArray(0), 0L, 0L, 0)
        }

        val totalSize = (maxAddr - minAddr).toInt()
        val binaryBuffer = ByteArray(totalSize) { defaultFillByte }

        for (rec in records) {
            val offset = (rec.address - minAddr).toInt()
            System.arraycopy(rec.data, 0, binaryBuffer, offset, rec.data.size)
        }

        return HexParseResult(binaryBuffer, minAddr, maxAddr, totalSize)
    }

    /**
     * Koduje zawartość bufora binarnego do formatu Intel HEX z podziałem na bloki 16-bajtowe (rekordy 00 i 04).
     */
    fun encodeToHex(data: ByteArray, startAddress: Long = 0L, bytesPerLine: Int = 16): String {
        val sb = StringBuilder()
        var currentUpperAddress = -1L

        var offset = 0
        while (offset < data.size) {
            val currentAddr = startAddress + offset
            val upperAddr = (currentAddr shr 16) and 0xFFFFL

            // Jeśli zmieniono segment 64KB, emituj rekord typu 04 (Extended Linear Address)
            if (upperAddr != currentUpperAddress) {
                currentUpperAddress = upperAddr
                val highByte = ((upperAddr shr 8) and 0xFF).toInt()
                val lowByte = (upperAddr and 0xFF).toInt()
                val sum = 0x02 + 0x00 + 0x00 + 0x04 + highByte + lowByte
                val chk = (-sum) and 0xFF
                sb.append(String.format(Locale.US, ":02000004%04X%02X\r\n", upperAddr, chk))
            }

            val chunkLen = minOf(bytesPerLine, data.size - offset)
            val lowerAddr = (currentAddr and 0xFFFF).toInt()

            var chkSum = chunkLen + ((lowerAddr shr 8) and 0xFF) + (lowerAddr and 0xFF) + 0x00
            val lineSb = StringBuilder()
            lineSb.append(String.format(Locale.US, ":%02X%04X00", chunkLen, lowerAddr))

            for (i in 0 until chunkLen) {
                val b = data[offset + i].toInt() and 0xFF
                chkSum += b
                lineSb.append(String.format(Locale.US, "%02X", b))
            }

            val finalChecksum = (-chkSum) and 0xFF
            lineSb.append(String.format(Locale.US, "%02X\r\n", finalChecksum))
            sb.append(lineSb.toString())

            offset += chunkLen
        }

        // EOF Record
        sb.append(":00000001FF\r\n")
        return sb.toString()
    }

    private data class HexRecord(val address: Long, val data: ByteArray)
}