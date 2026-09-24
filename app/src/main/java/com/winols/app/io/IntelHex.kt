package com.winols.app.io

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Parser oraz koder formatu Intel HEX (I8HEX, I16HEX, I32HEX).
 * Umożliwia konwersję pomiędzy strumieniem tekstowym HEX a buforem binarnym.
 */
object IntelHex {

    private const val RECORD_DATA = 0x00
    private const val RECORD_EOF = 0x01
    private const val RECORD_EXT_SEGMENT_ADDR = 0x02
    private const val RECORD_START_SEGMENT_ADDR = 0x03
    private const val RECORD_EXT_LINEAR_ADDR = 0x04
    private const val RECORD_START_LINEAR_ADDR = 0x05

    data class HexRecord(
        val byteCount: Int,
        val address: Int,
        val recordType: Int,
        val data: ByteArray,
        val checksum: Int
    )

    /**
     * Wczytuje dane ze strumienia Intel HEX bezpośrednio do bufora docelowego lub alokuje nowy.
     */
    fun parse(inputStream: InputStream, estimatedSize: Int = 2 * 1024 * 1024): ByteBuffer {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.US_ASCII))
        var upperAddress = 0
        val segments = mutableListOf<Pair<Int, ByteArray>>()
        var maxAddress = 0

        reader.forEachLine { rawLine ->
            val line = rawLine.trim()
            if (line.isNotEmpty() && line.startsWith(":")) {
                val record = parseLine(line)
                when (record.recordType) {
                    RECORD_DATA -> {
                        val fullAddress = upperAddress or record.address
                        segments.add(fullAddress to record.data)
                        val endOffset = fullAddress + record.byteCount
                        if (endOffset > maxAddress) {
                            maxAddress = endOffset
                        }
                    }
                    RECORD_EXT_LINEAR_ADDR -> {
                        if (record.byteCount == 2) {
                            val high = (record.data[0].toInt() and 0xFF) shl 8
                            val low = record.data[1].toInt() and 0xFF
                            upperAddress = (high or low) shl 16
                        }
                    }
                    RECORD_EXT_SEGMENT_ADDR -> {
                        if (record.byteCount == 2) {
                            val high = (record.data[0].toInt() and 0xFF) shl 8
                            val low = record.data[1].toInt() and 0xFF
                            upperAddress = (high or low) shl 4
                        }
                    }
                    RECORD_EOF -> return@forEachLine
                }
            }
        }

        val finalSize = maxOf(maxAddress, estimatedSize)
        val buffer = ByteBuffer.allocateDirect(finalSize).order(ByteOrder.LITTLE_ENDIAN)
        
        // Wypełnienie wartościami 0xFF (domyślny stan czystej pamięci Flash/EEPROM)
        val fillChunk = ByteArray(4096) { 0xFF.toByte() }
        var written = 0
        while (written < finalSize) {
            val toWrite = minOf(fillChunk.size, finalSize - written)
            buffer.put(fillChunk, 0, toWrite)
            written += toWrite
        }
        buffer.clear()

        // Zaaplikowanie segmentów danych
        for ((address, data) in segments) {
            if (address + data.size <= buffer.capacity()) {
                buffer.position(address)
                buffer.put(data)
            }
        }
        buffer.clear()
        return buffer
    }

    /**
     * Zapisuje zawartość bufora do formatu Intel HEX (I32HEX)
     */
    fun write(buffer: ByteBuffer, outputStream: OutputStream, bytesPerRecord: Int = 16) {
        val writer = outputStream.bufferedWriter(Charsets.US_ASCII)
        val totalBytes = buffer.capacity()
        var currentUpper = -1

        val chunk = ByteArray(bytesPerRecord)
        var offset = 0

        while (offset < totalBytes) {
            val highAddress = (offset ushr 16) and 0xFFFF
            if (highAddress != currentUpper) {
                currentUpper = highAddress
                // Emit Extended Linear Address Record (0x04)
                val extData = byteArrayOf(((currentUpper shr 8) and 0xFF).toByte(), (currentUpper and 0xFF).toByte())
                writeRecord(writer, 2, 0x0000, RECORD_EXT_LINEAR_ADDR, extData)
            }

            val count = minOf(bytesPerRecord, totalBytes - offset)
            buffer.position(offset)
            buffer.get(chunk, 0, count)

            val recordData = if (count == bytesPerRecord) chunk else chunk.copyOf(count)
            writeRecord(writer, count, offset and 0xFFFF, RECORD_DATA, recordData)

            offset += count
        }

        // EOF Record
        writeRecord(writer, 0, 0x0000, RECORD_EOF, ByteArray(0))
        writer.flush()
        buffer.clear()
    }

    private fun parseLine(line: String): HexRecord {
        val count = line.substring(1, 3).toInt(16)
        val address = line.substring(3, 7).toInt(16)
        val type = line.substring(7, 9).toInt(16)
        val data = ByteArray(count)

        for (i in 0 until count) {
            val idx = 9 + (i * 2)
            data[i] = line.substring(idx, idx + 2).toInt(16).toByte()
        }

        val checksum = line.substring(9 + (count * 2), 11 + (count * 2)).toInt(16)
        return HexRecord(count, address, type, data, checksum)
    }

    private fun writeRecord(writer: java.io.BufferedWriter, count: Int, address: Int, type: Int, data: ByteArray) {
        var sum = count + ((address shr 8) and 0xFF) + (address and 0xFF) + type
        val sb = StringBuilder()
        sb.append(String.format(":%02X%04X%02X", count, address, type))

        for (b in data) {
            val byteVal = b.toInt() and 0xFF
            sum += byteVal
            sb.append(String.format("%02X", byteVal))
        }

        val checksum = ((sum xor 0xFF) + 1) and 0xFF
        sb.append(String.format("%02X\r\n", checksum))
        writer.write(sb.toString())
    }
}