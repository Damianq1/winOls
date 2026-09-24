package com.winols.app.io

import java.io.BufferedReader
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.Arrays

/**
 * Parser oraz koder formatu Intel HEX (I8HEX, I16HEX, I32HEX).
 * Obsługuje rekordy:
 * 00 - Data
 * 01 - End Of File
 * 02 - Extended Segment Address
 * 04 - Extended Linear Address
 */
object IntelHex {

    class HexRecordException(message: String) : Exception(message)

    data class Record(
        val byteCount: Int,
        val address: Int,
        val recordType: Int,
        val data: ByteArray,
        val checksum: Int
    )

    /**
     * Parsuje strumień Intel HEX i zwraca tablicę bajtów wypełniającą obszar binarny.
     * Wypełnia puste/niezaadresowane luki wartością 0xFF (standard pamięci Flash/ROM).
     */
    fun parse(inputStream: InputStream, targetSize: Int? = null): ByteArray {
        val reader = BufferedReader(inputStream.reader())
        val sparseBlocks = mutableListOf<Pair<Int, ByteArray>>()
        var upperAddress = 0
        var maxAddress = 0

        reader.forEachLine { rawLine ->
            val line = rawLine.trim()
            if (line.isNotEmpty() && line.startsWith(":")) {
                val record = parseRecord(line)
                when (record.recordType) {
                    0x00 -> { // Data Record
                        val physicalAddress = upperAddress or record.address
                        sparseBlocks.add(Pair(physicalAddress, record.data))
                        val end = physicalAddress + record.byteCount
                        if (end > maxAddress) maxAddress = end
                    }
                    0x01 -> { // EOF
                        return@forEachLine
                    }
                    0x02 -> { // Extended Segment Address
                        val segment = ((record.data[0].toInt() and 0xFF) shl 8) or (record.data[1].toInt() and 0xFF)
                        upperAddress = segment shl 4
                    }
                    0x04 -> { // Extended Linear Address
                        val upper = ((record.data[0].toInt() and 0xFF) shl 8) or (record.data[1].toInt() and 0xFF)
                        upperAddress = upper shl 16
                    }
                }
            }
        }

        val totalSize = targetSize ?: maxAddress.coerceAtLeast(1)
        val flatBuffer = ByteArray(totalSize)
        Arrays.fill(flatBuffer, 0xFF.toByte())

        for ((addr, data) in sparseBlocks) {
            val copyLen = data.size.coerceAtMost(flatBuffer.size - addr)
            if (copyLen > 0 && addr < flatBuffer.size) {
                System.arraycopy(data, 0, flatBuffer, addr, copyLen)
            }
        }

        return flatBuffer
    }

    private fun parseRecord(line: String): Record {
        val clean = line.removePrefix(":")
        if (clean.length < 10) throw HexRecordException("Zbyt krótki rekord HEX: $line")

        val byteCount = clean.substring(0, 2).toInt(16)
        val address = clean.substring(2, 6).toInt(16)
        val recordType = clean.substring(6, 8).toInt(16)

        val expectedChars = 10 + (byteCount * 2)
        if (clean.length < expectedChars) {
            throw HexRecordException("Niekompletny rekord danych: oczekiwano $expectedChars znaków, otrzymano ${clean.length}")
        }

        val data = ByteArray(byteCount)
        var checksumAccumulator = byteCount + (address shr 8) + (address and 0xFF) + recordType

        for (i in 0 until byteCount) {
            val byteVal = clean.substring(8 + (i * 2), 10 + (i * 2)).toInt(16)
            data[i] = byteVal.toByte()
            checksumAccumulator += byteVal
        }

        val checksum = clean.substring(8 + (byteCount * 2), 10 + (byteCount * 2)).toInt(16)
        val calculatedChecksum = ((-checksumAccumulator) and 0xFF)

        if (checksum != calculatedChecksum) {
            throw HexRecordException("Błąd sumy kontrolnej rekordu HEX! Otrzymano 0x${checksum.toString(16)}, oczekiwano 0x${calculatedChecksum.toString(16)}")
        }

        return Record(byteCount, address, recordType, data, checksum)
    }

    /**
     * Eksportuje zawartość bufora bajtowego do formatu Intel HEX (rekordy 16-bajtowe z obsługą 32-bitowej przestrzeni).
     */
    fun export(buffer: ByteBuffer, outputStream: OutputStream, bytesPerLine: Int = 16) {
        val writer = outputStream.bufferedWriter()
        val originalPos = buffer.position()
        buffer.position(0)

        val totalLength = buffer.capacity()
        var currentUpper = -1

        var offset = 0
        val lineBuffer = ByteArray(bytesPerLine)

        while (offset < totalLength) {
            val upper = offset ushr 16
            if (upper != currentUpper) {
                currentUpper = upper
                val upperBytes = byteArrayOf((upper shr 8).toByte(), (upper and 0xFF).toByte())
                writer.write(formatRecord(0x0000, 0x04, upperBytes))
                writer.newLine()
            }

            val remaining = totalLength - offset
            val count = remaining.coerceAtMost(bytesPerLine)
            buffer.position(offset)
            buffer.get(lineBuffer, 0, count)

            val slice = if (count == bytesPerLine) lineBuffer else lineBuffer.copyOf(count)
            val lowerAddress = offset and 0xFFFF
            writer.write(formatRecord(lowerAddress, 0x00, slice))
            writer.newLine()

            offset += count
        }

        // EOF Record
        writer.write(":00000001FF")
        writer.newLine()
        writer.flush()
        buffer.position(originalPos)
    }

    private fun formatRecord(address: Int, recordType: Int, data: ByteArray): String {
        val count = data.size
        var checksumAccumulator = count + (address shr 8) + (address and 0xFF) + recordType

        val sb = StringBuilder(":")
        sb.append(String.format("%02X", count))
        sb.append(String.format("%04X", address))
        sb.append(String.format("%02X", recordType))

        for (b in data) {
            val v = b.toInt() and 0xFF
            checksumAccumulator += v
            sb.append(String.format("%02X", v))
        }

        val checksum = ((-checksumAccumulator) and 0xFF)
        sb.append(String.format("%02X", checksum))
        return sb.toString()
    }
}