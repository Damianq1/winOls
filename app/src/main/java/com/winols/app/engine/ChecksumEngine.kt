package com.winols.app.engine

import com.winols.app.data.BinaryBufferManager
import java.util.zip.CRC32

class ChecksumEngine(private val bufferManager: BinaryBufferManager) {

    fun calculateSimpleSum16(startAddress: Int, length: Int): Int {
        val bytes = bufferManager.readBlock(startAddress, length)
        var sum = 0
        for (i in 0 until length - 1 step 2) {
            val high = bytes[i].toInt() and 0xFF
            val low = bytes[i + 1].toInt() and 0xFF
            sum = (sum + ((high shl 8) or low)) and 0xFFFF
        }
        return sum
    }

    fun calculateCRC32(startAddress: Int, length: Int): Long {
        val bytes = bufferManager.readBlock(startAddress, length)
        val crc = CRC32()
        crc.update(bytes)
        return crc.value
    }

    fun verifyChecksum(startAddress: Int, length: Int, expectedCrc: Long): Boolean {
        return calculateCRC32(startAddress, length) == expectedCrc
    }
}