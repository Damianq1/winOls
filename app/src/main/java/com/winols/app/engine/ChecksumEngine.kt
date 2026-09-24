package com.winols.app.engine

import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class ChecksumFamily {
    GENERIC_SUM_16_LE,
    GENERIC_SUM_32_LE,
    BOSCH_EDC15,
    BOSCH_EDC16,
    SIEMENS_SIMOS
}

data class ChecksumResult(
    val family: ChecksumFamily,
    val calculatedChecksum: Long,
    val storedChecksum: Long,
    val isValid: Boolean,
    val patchedAddress: Long? = null
)

class ChecksumEngine {

    fun calculate16BitSum(buffer: ByteArray, startAddress: Int, endAddress: Int): Int {
        var sum = 0
        val boundedEnd = minOf(endAddress, buffer.size - 1)
        for (i in startAddress..boundedEnd step 2) {
            if (i + 1 < buffer.size) {
                val b1 = buffer[i].toInt() and 0xFF
                val b2 = buffer[i + 1].toInt() and 0xFF
                val word = (b2 shl 8) or b1
                sum = (sum + word) and 0xFFFF
            }
        }
        return sum
    }

    fun calculate32BitSum(buffer: ByteArray, startAddress: Int, endAddress: Int): Long {
        var sum = 0L
        val boundedEnd = minOf(endAddress, buffer.size - 3)
        for (i in startAddress..boundedEnd step 4) {
            val bb = ByteBuffer.wrap(buffer, i, 4).order(ByteOrder.LITTLE_ENDIAN)
            val dword = bb.int.toLong() and 0xFFFFFFFFL
            sum = (sum + dword) and 0xFFFFFFFFL
        }
        return sum
    }

    fun verifyBlock(
        buffer: ByteArray,
        startAddress: Int,
        endAddress: Int,
        checksumLocation: Int,
        family: ChecksumFamily
    ): ChecksumResult {
        return when (family) {
            ChecksumFamily.GENERIC_SUM_16_LE, ChecksumFamily.BOSCH_EDC15 -> {
                val calc = calculate16BitSum(buffer, startAddress, endAddress).toLong()
                val stored = if (checksumLocation + 1 < buffer.size) {
                    ((buffer[checksumLocation + 1].toInt() and 0xFF) shl 8 or (buffer[checksumLocation].toInt() and 0xFF)).toLong()
                } else 0L
                ChecksumResult(family, calc, stored, calc == stored, checksumLocation.toLong())
            }
            ChecksumFamily.GENERIC_SUM_32_LE, ChecksumFamily.BOSCH_EDC16, ChecksumFamily.SIEMENS_SIMOS -> {
                val calc = calculate32BitSum(buffer, startAddress, endAddress)
                val stored = if (checksumLocation + 3 < buffer.size) {
                    val bb = ByteBuffer.wrap(buffer, checksumLocation, 4).order(ByteOrder.LITTLE_ENDIAN)
                    bb.int.toLong() and 0xFFFFFFFFL
                } else 0L
                ChecksumResult(family, calc, stored, calc == stored, checksumLocation.toLong())
            }
        }
    }

    fun patchChecksum(
        buffer: ByteArray,
        startAddress: Int,
        endAddress: Int,
        checksumLocation: Int,
        family: ChecksumFamily
    ): Boolean {
        when (family) {
            ChecksumFamily.GENERIC_SUM_16_LE, ChecksumFamily.BOSCH_EDC15 -> {
                val calc = calculate16BitSum(buffer, startAddress, endAddress)
                if (checksumLocation + 1 < buffer.size) {
                    buffer[checksumLocation] = (calc and 0xFF).toByte()
                    buffer[checksumLocation + 1] = ((calc shr 8) and 0xFF).toByte()
                    return true
                }
            }
            ChecksumFamily.GENERIC_SUM_32_LE, ChecksumFamily.BOSCH_EDC16, ChecksumFamily.SIEMENS_SIMOS -> {
                val calc = calculate32BitSum(buffer, startAddress, endAddress)
                if (checksumLocation + 3 < buffer.size) {
                    val bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(calc.toInt())
                    System.arraycopy(bb.array(), 0, buffer, checksumLocation, 4)
                    return true
                }
            }
        }
        return false
    }
}