package com.winols.app.engine

import com.winols.app.data.BinaryBufferManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteOrder

class ChecksumEngine(private val bufferManager: BinaryBufferManager) {

    enum class Algorithm {
        ADD8,
        ADD16_LE,
        ADD16_BE,
        CRC16_CCITT,
        CRC32
    }

    data class ChecksumResult(
        val calculated: Long,
        val stored: Long,
        val isValid: Boolean
    )

    suspend fun computeChecksum(
        startAddress: Int,
        endAddress: Int,
        algorithm: Algorithm
    ): Long = withContext(Dispatchers.Default) {
        val length = endAddress - startAddress
        if (length <= 0) return@withContext 0L
        val data = bufferManager.readBlock(startAddress, length)

        when (algorithm) {
            Algorithm.ADD8 -> {
                var sum = 0
                for (b in data) {
                    sum = (sum + (b.toInt() and 0xFF)) and 0xFF
                }
                sum.toLong()
            }
            Algorithm.ADD16_LE -> {
                var sum = 0
                var i = 0
                while (i < data.size - 1) {
                    val low = data[i].toInt() and 0xFF
                    val high = data[i + 1].toInt() and 0xFF
                    sum = (sum + (low or (high shl 8))) and 0xFFFF
                    i += 2
                }
                sum.toLong()
            }
            Algorithm.ADD16_BE -> {
                var sum = 0
                var i = 0
                while (i < data.size - 1) {
                    val high = data[i].toInt() and 0xFF
                    val low = data[i + 1].toInt() and 0xFF
                    sum = (sum + ((high shl 8) or low)) and 0xFFFF
                    i += 2
                }
                sum.toLong()
            }
            Algorithm.CRC16_CCITT -> {
                var crc = 0xFFFF
                for (b in data) {
                    crc = ((crc ushr 8) or (crc shl 8)) and 0xFFFF
                    crc = crc xor (b.toInt() and 0xFF)
                    crc = crc xor ((crc and 0xFF) shr 4)
                    crc = crc xor ((crc shl 12) and 0xFFFF)
                    crc = crc xor (((crc and 0xFF) shl 5) and 0xFFFF)
                }
                crc.toLong() and 0xFFFF
            }
            Algorithm.CRC32 -> {
                var crc = 0xFFFFFFFF.toInt()
                for (b in data) {
                    var byteVal = (b.toInt() xor crc) and 0xFF
                    for (j in 0 until 8) {
                        val mask = -(byteVal and 1)
                        byteVal = (byteVal ushr 1) xor (0xEDB88320.toInt() and mask)
                    }
                    crc = (crc ushr 8) xor byteVal
                }
                (crc.inv().toLong()) and 0xFFFFFFFFL
            }
        }
    }

    suspend fun verifyAndPatch(
        startAddress: Int,
        endAddress: Int,
        checksumAddress: Int,
        algorithm: Algorithm,
        patch: Boolean = false
    ): ChecksumResult = withContext(Dispatchers.Default) {
        val calculated = computeChecksum(startAddress, endAddress, algorithm)
        val bytesNeeded = when (algorithm) {
            Algorithm.ADD8 -> 1
            Algorithm.CRC32 -> 4
            else -> 2
        }

        val storedData = bufferManager.readBlock(checksumAddress, bytesNeeded)
        var stored = 0L
        for (i in 0 until bytesNeeded) {
            stored = stored or ((storedData[i].toLong() and 0xFF) shl (i * 8))
        }

        val isValid = (calculated == stored)

        if (patch && !isValid) {
            val patchData = ByteArray(bytesNeeded)
            for (i in 0 until bytesNeeded) {
                patchData[i] = ((calculated ushr (i * 8)) and 0xFF).toByte()
            }
            bufferManager.writeBlock(checksumAddress, patchData)
        }

        ChecksumResult(calculated, stored, isValid)
    }
}