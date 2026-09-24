package com.winols.app.engine

import com.winols.app.data.BinaryBufferManager
import com.winols.app.model.DataType
import java.util.zip.CRC32

class ChecksumEngine(private val bufferManager: BinaryBufferManager) {

    enum class Algorithm {
        ADD8,
        ADD16_LE,
        ADD16_BE,
        ADD32_LE,
        ADD32_BE,
        XOR8,
        CRC16_CCITT,
        CRC32_IEEE
    }

    data class ChecksumBlock(
        val id: String,
        val description: String,
        val startAddress: Int,
        val endAddress: Int,
        val checksumAddress: Int,
        val algorithm: Algorithm
    )

    fun calculate(block: ChecksumBlock): Long {
        require(block.startAddress >= 0 && block.endAddress >= block.startAddress) { "Niepoprawny zakres bloku" }
        require(block.endAddress < bufferManager.size) { "Zakres wykracza poza bufor" }

        val length = block.endAddress - block.startAddress + 1
        val rawBytes = ByteArray(length)
        for (i in 0 until length) {
            rawBytes[i] = bufferManager.readRawValue(block.startAddress + i, 1).toByte()
        }

        return when (block.algorithm) {
            Algorithm.ADD8 -> {
                var sum = 0L
                for (b in rawBytes) {
                    sum = (sum + (b.toLong() and 0xFFL)) and 0xFFL
                }
                sum
            }
            Algorithm.ADD16_LE -> {
                var sum = 0L
                for (i in 0 until length - 1 step 2) {
                    val w = (rawBytes[i].toLong() and 0xFFL) or ((rawBytes[i + 1].toLong() and 0xFFL) shl 8)
                    sum = (sum + w) and 0xFFFFL
                }
                sum
            }
            Algorithm.ADD16_BE -> {
                var sum = 0L
                for (i in 0 until length - 1 step 2) {
                    val w = ((rawBytes[i].toLong() and 0xFFL) shl 8) or (rawBytes[i + 1].toLong() and 0xFFL)
                    sum = (sum + w) and 0xFFFFL
                }
                sum
            }
            Algorithm.ADD32_LE -> {
                var sum = 0L
                for (i in 0 until length - 3 step 4) {
                    val dw = (rawBytes[i].toLong() and 0xFFL) or
                            ((rawBytes[i + 1].toLong() and 0xFFL) shl 8) or
                            ((rawBytes[i + 2].toLong() and 0xFFL) shl 16) or
                            ((rawBytes[i + 3].toLong() and 0xFFL) shl 24)
                    sum = (sum + dw) and 0xFFFFFFFFL
                }
                sum
            }
            Algorithm.ADD32_BE -> {
                var sum = 0L
                for (i in 0 until length - 3 step 4) {
                    val dw = ((rawBytes[i].toLong() and 0xFFL) shl 24) or
                            ((rawBytes[i + 1].toLong() and 0xFFL) shl 16) or
                            ((rawBytes[i + 2].toLong() and 0xFFL) shl 8) or
                            (rawBytes[i + 3].toLong() and 0xFFL)
                    sum = (sum + dw) and 0xFFFFFFFFL
                }
                sum
            }
            Algorithm.XOR8 -> {
                var xorVal = 0L
                for (b in rawBytes) {
                    xorVal = xorVal xor (b.toLong() and 0xFFL)
                }
                xorVal and 0xFFL
            }
            Algorithm.CRC16_CCITT -> {
                var crc = 0xFFFF
                for (b in rawBytes) {
                    val cur = b.toInt() and 0xFF
                    crc = ((crc ushr 8) or (crc shl 8)) and 0xFFFF
                    crc = crc xor cur
                    crc = crc xor ((crc and 0xFF) ushr 4)
                    crc = crc xor ((crc shl 12) and 0xFFFF)
                    crc = crc xor (((crc and 0xFF) shl 5) and 0xFFFF)
                }
                crc.toLong() and 0xFFFFL
            }
            Algorithm.CRC32_IEEE -> {
                val crc32 = CRC32()
                crc32.update(rawBytes)
                crc32.value and 0xFFFFFFFFL
            }
        }
    }

    fun verify(block: ChecksumBlock): Boolean {
        val calculated = calculate(block)
        val stored = when (block.algorithm) {
            Algorithm.ADD8, Algorithm.XOR8 -> bufferManager.readRawValue(block.checksumAddress, 1)
            Algorithm.ADD16_LE, Algorithm.CRC16_CCITT -> bufferManager.readRawValue(block.checksumAddress, 2, false)
            Algorithm.ADD16_BE -> bufferManager.readRawValue(block.checksumAddress, 2, true)
            Algorithm.ADD32_LE, Algorithm.CRC32_IEEE -> bufferManager.readRawValue(block.checksumAddress, 4, false)
            Algorithm.ADD32_BE -> bufferManager.readRawValue(block.checksumAddress, 4, true)
        }
        return calculated == stored
    }

    fun patch(block: ChecksumBlock) {
        val calculated = calculate(block)
        bufferManager.pushSnapshot()
        when (block.algorithm) {
            Algorithm.ADD8, Algorithm.XOR8 -> bufferManager.writeRawValue(block.checksumAddress, calculated, 1)
            Algorithm.ADD16_LE, Algorithm.CRC16_CCITT -> bufferManager.writeRawValue(block.checksumAddress, calculated, 2, false)
            Algorithm.ADD16_BE -> bufferManager.writeRawValue(block.checksumAddress, calculated, 2, true)
            Algorithm.ADD32_LE, Algorithm.CRC32_IEEE -> bufferManager.writeRawValue(block.checksumAddress, calculated, 4, false)
            Algorithm.ADD32_BE -> bufferManager.writeRawValue(block.checksumAddress, calculated, 4, true)
        }
    }
}