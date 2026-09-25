package com.winols.app.core.binary

import java.util.zip.CRC32

object ChecksumCalculator {

    fun calculateCRC32(data: ByteArray, startOffset: Int = 0, length: Int = data.size): Long {
        val crc = CRC32()
        crc.update(data, startOffset, length)
        return crc.value
    }

    fun calculateAdditive16Bit(data: ByteArray, startOffset: Int = 0, length: Int = data.size): Int {
        var sum = 0
        val end = (startOffset + length).coerceAtMost(data.size)
        for (i in startOffset until end) {
            sum = (sum + (data[i].toInt() and 0xFF)) and 0xFFFF
        }
        return sum
    }
}