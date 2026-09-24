package com.winols.app.engine

import com.winols.app.data.BinaryBufferManager
import com.winols.app.model.DataType

class ChecksumEngine(private val bufferManager: BinaryBufferManager) {

    fun calculateSimple16BitSum(startAddress: Int, endAddress: Int): Int {
        var sum = 0
        var addr = startAddress
        while (addr + 1 <= endAddress && addr + 1 < bufferManager.size) {
            val word = bufferManager.readValue(addr, DataType.UWORD_LE).toInt()
            sum = (sum + word) and 0xFFFF
            addr += 2
        }
        return sum
    }

    fun verifyChecksum16Bit(startAddress: Int, endAddress: Int, checksumAddress: Int): Boolean {
        val calculated = calculateSimple16BitSum(startAddress, endAddress)
        val stored = bufferManager.readValue(checksumAddress, DataType.UWORD_LE).toInt()
        return calculated == stored
    }

    fun updateChecksum16Bit(startAddress: Int, endAddress: Int, checksumAddress: Int) {
        val calculated = calculateSimple16BitSum(startAddress, endAddress)
        bufferManager.writeValue(checksumAddress, DataType.UWORD_LE, calculated.toDouble())
    }

    fun calculateCrc32(startAddress: Int, length: Int): Long {
        val crc = java.util.zip.CRC32()
        val raw = bufferManager.getRawBytes()
        val safeLen = length.coerceAtMost(raw.size - startAddress)
        if (safeLen > 0 && startAddress >= 0) {
            crc.update(raw, startAddress, safeLen)
        }
        return crc.value
    }
}