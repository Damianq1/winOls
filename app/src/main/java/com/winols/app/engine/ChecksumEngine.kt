package com.winols.app.engine

import com.winols.app.data.BinaryBufferManager
import java.util.zip.CRC32

/**
 * Silnik weryfikacji i kalkulacji sum kontrolnych typowych dla ECU.
 */
object ChecksumEngine {

    /**
     * Oblicza prostą sumę 16-bitową (modulo 0x10000).
     */
    @JvmStatic
    fun calculateSum16(bufferManager: BinaryBufferManager, startAddress: Int, length: Int): Int {
        var sum = 0
        for (i in 0 until length) {
            val byteVal = bufferManager.getRawByte(startAddress + i).toInt() and 0xFF
            sum = (sum + byteVal) and 0xFFFF
        }
        return sum
    }

    /**
     * Oblicza sumę 32-bitową.
     */
    @JvmStatic
    fun calculateSum32(bufferManager: BinaryBufferManager, startAddress: Int, length: Int): Long {
        var sum = 0L
        for (i in 0 until length) {
            val byteVal = (bufferManager.getRawByte(startAddress + i).toInt() and 0xFF).toLong()
            sum = (sum + byteVal) and 0xFFFFFFFFL
        }
        return sum
    }

    /**
     * Standardowe CRC32 dla zadanego bloku pamięci.
     */
    @JvmStatic
    fun calculateCRC32(bufferManager: BinaryBufferManager, startAddress: Int, length: Int): Long {
        val crc = CRC32()
        val data = ByteArray(length)
        for (i in 0 until length) {
            data[i] = bufferManager.getRawByte(startAddress + i)
        }
        crc.update(data)
        return crc.value
    }
}