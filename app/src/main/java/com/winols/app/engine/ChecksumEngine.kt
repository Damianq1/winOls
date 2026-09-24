package com.winols.app.engine

import com.winols.app.data.BinaryBufferManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.zip.CRC32

object ChecksumEngine {

    suspend fun calculateAdd8(
        bufferManager: BinaryBufferManager,
        startAddress: Int,
        endAddress: Int
    ): Int = withContext(Dispatchers.Default) {
        val data = bufferManager.toByteArray()
        var sum = 0
        for (i in startAddress..endAddress.coerceAtMost(data.size - 1)) {
            sum = (sum + (data[i].toInt() and 0xFF)) and 0xFF
        }
        sum
    }

    suspend fun calculateAdd16(
        bufferManager: BinaryBufferManager,
        startAddress: Int,
        endAddress: Int,
        littleEndian: Boolean = false
    ): Int = withContext(Dispatchers.Default) {
        val data = bufferManager.toByteArray()
        var sum = 0
        var i = startAddress
        while (i < endAddress.coerceAtMost(data.size - 1)) {
            val b1 = data[i].toInt() and 0xFF
            val b2 = data[i + 1].toInt() and 0xFF
            val word = if (littleEndian) (b2 shl 8) or b1 else (b1 shl 8) or b2
            sum = (sum + word) and 0xFFFF
            i += 2
        }
        sum
    }

    suspend fun calculateCRC16CCITT(
        bufferManager: BinaryBufferManager,
        startAddress: Int,
        endAddress: Int
    ): Int = withContext(Dispatchers.Default) {
        val data = bufferManager.toByteArray()
        var crc = 0xFFFF
        val polynomial = 0x1021

        for (i in startAddress..endAddress.coerceAtMost(data.size - 1)) {
            val b = data[i].toInt() and 0xFF
            for (bit in 0 until 8) {
                val bitVal = ((b shr (7 - bit)) and 1) == 1
                val c15 = ((crc shr 15) and 1) == 1
                crc = crc shl 1
                if (c15 xor bitVal) crc = crc xor polynomial
            }
        }
        crc and 0xFFFF
    }

    suspend fun calculateCRC32(
        bufferManager: BinaryBufferManager,
        startAddress: Int,
        endAddress: Int
    ): Long = withContext(Dispatchers.Default) {
        val data = bufferManager.toByteArray()
        val crc = CRC32()
        val len = (endAddress - startAddress + 1).coerceAtMost(data.size - startAddress)
        crc.update(data, startAddress, len)
        crc.value
    }
}