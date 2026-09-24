package com.winols.app.engine

import com.winols.app.data.BinaryBufferManager
import com.winols.app.model.DataType
import java.util.zip.CRC32

data class ChecksumBlock(
    val name: String,
    val startAddress: Long,
    val endAddress: Long,
    val checksumAddress: Long,
    val algorithm: ChecksumAlgorithm
)

enum class ChecksumAlgorithm {
    ADD_16_LE,
    ADD_32_LE,
    CRC32_STANDARD
}

data class ChecksumResult(
    val block: ChecksumBlock,
    val storedValue: Long,
    val calculatedValue: Long,
    val isValid: Boolean
)

class ChecksumEngine(private val bufferManager: BinaryBufferManager) {

    fun verifyBlock(block: ChecksumBlock): ChecksumResult {
        val calculated = calculate(block)
        val stored = when (block.algorithm) {
            ChecksumAlgorithm.ADD_16_LE -> bufferManager.readValue(block.checksumAddress, DataType.UINT16_LE).toLong()
            ChecksumAlgorithm.ADD_32_LE, ChecksumAlgorithm.CRC32_STANDARD ->
                bufferManager.readValue(block.checksumAddress, DataType.UINT32_LE).toLong()
        }
        return ChecksumResult(block, stored, calculated, stored == calculated)
    }

    fun applyCorrection(block: ChecksumBlock) {
        val calculated = calculate(block)
        when (block.algorithm) {
            ChecksumAlgorithm.ADD_16_LE -> {
                bufferManager.writeValue(block.checksumAddress, calculated.toDouble(), DataType.UINT16_LE)
            }
            ChecksumAlgorithm.ADD_32_LE, ChecksumAlgorithm.CRC32_STANDARD -> {
                bufferManager.writeValue(block.checksumAddress, calculated.toDouble(), DataType.UINT32_LE)
            }
        }
    }

    private fun calculate(block: ChecksumBlock): Long {
        val raw = bufferManager.rawBuffer
        val start = block.startAddress.toInt()
        val end = block.endAddress.toInt()

        return when (block.algorithm) {
            ChecksumAlgorithm.ADD_16_LE -> {
                var sum = 0
                var i = start
                while (i < end) {
                    val b0 = raw[i].toInt() and 0xFF
                    val b1 = if (i + 1 < end) (raw[i + 1].toInt() and 0xFF) shl 8 else 0
                    sum = (sum + (b0 or b1)) and 0xFFFF
                    i += 2
                }
                sum.toLong()
            }
            ChecksumAlgorithm.ADD_32_LE -> {
                var sum = 0L
                var i = start
                while (i < end) {
                    val b0 = (raw[i].toLong() and 0xFF)
                    val b1 = if (i + 1 < end) (raw[i + 1].toLong() and 0xFF) shl 8 else 0L
                    val b2 = if (i + 2 < end) (raw[i + 2].toLong() and 0xFF) shl 16 else 0L
                    val b3 = if (i + 3 < end) (raw[i + 3].toLong() and 0xFF) shl 24 else 0L
                    sum = (sum + (b0 or b1 or b2 or b3)) and 0xFFFFFFFFL
                    i += 4
                }
                sum
            }
            ChecksumAlgorithm.CRC32_STANDARD -> {
                val crc = CRC32()
                crc.update(raw, start, end - start)
                crc.value
            }
        }
    }
}