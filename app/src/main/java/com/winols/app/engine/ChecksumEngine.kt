package com.winols.app.engine

import com.winols.app.data.BinaryBufferManager

enum class ChecksumType {
    ADD_16,
    CRC32
}

class ChecksumEngine(private val bufferManager: BinaryBufferManager) {

    fun calculateSum16(startOffset: Int, endOffset: Int): Int {
        var sum = 0
        var i = startOffset
        while (i < endOffset) {
            sum = (sum + bufferManager.getUShort(i)) and 0xFFFF
            i += 2
        }
        return sum
    }

    fun calculateCRC32(startOffset: Int, endOffset: Int): Long {
        val crc = java.util.zip.CRC32()
        val data = bufferManager.getSubArray(startOffset, endOffset - startOffset)
        crc.update(data)
        return crc.value
    }

    fun verifyChecksum16(startOffset: Int, endOffset: Int, expectedChecksumOffset: Int): Boolean {
        val actual = calculateSum16(startOffset, endOffset)
        val expected = bufferManager.getUShort(expectedChecksumOffset)
        return actual == expected
    }

    fun patchChecksum16(startOffset: Int, endOffset: Int, targetChecksumOffset: Int) {
        val calculated = calculateSum16(startOffset, endOffset)
        bufferManager.setShort(targetChecksumOffset, calculated.toShort())
    }
}