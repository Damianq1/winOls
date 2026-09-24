package com.winols.app.engine

import com.winols.app.data.BinaryBufferManager
import java.nio.ByteOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class ChecksumResult {
    data class Success(val calculated: Long, val original: Long, val matched: Boolean) : ChecksumResult()
    data class Error(val message: String) : ChecksumResult()
}

class ChecksumEngine(private val bufferManager: BinaryBufferManager = BinaryBufferManager.instance) {

    suspend fun verifyAdd8(startAddr: Int, endAddr: Int, storedAddr: Int): ChecksumResult =
        withContext(Dispatchers.Default) {
            val length = endAddr - startAddr
            if (length <= 0) return@withContext ChecksumResult.Error("Nieprawidłowy zakres pamięci")

            val slice = bufferManager.getBufferSlice(startAddr, length)
            var sum = 0
            for (b in slice) {
                sum = (sum + (b.toInt() and 0xFF)) and 0xFF
            }

            val stored = bufferManager.readValue(storedAddr, com.winols.app.model.DataType.UBYTE, ByteOrder.LITTLE_ENDIAN).toLong()
            ChecksumResult.Success(sum.toLong(), stored, sum.toLong() == stored)
        }

    suspend fun verifyAdd16(startAddr: Int, endAddr: Int, storedAddr: Int, order: ByteOrder): ChecksumResult =
        withContext(Dispatchers.Default) {
            val length = endAddr - startAddr
            if (length <= 0 || length % 2 != 0) {
                return@withContext ChecksumResult.Error("Zakres musi mieć długość parzystą dla 16-bit sumy")
            }

            val slice = bufferManager.getBufferSlice(startAddr, length)
            var sum = 0
            for (i in 0 until length step 2) {
                val b1 = slice[i].toInt() and 0xFF
                val b2 = slice[i + 1].toInt() and 0xFF
                val word = if (order == ByteOrder.LITTLE_ENDIAN) (b2 shl 8) or b1 else (b1 shl 8) or b2
                sum = (sum + word) and 0xFFFF
            }

            val stored = bufferManager.readValue(storedAddr, com.winols.app.model.DataType.UWORD, order).toLong()
            ChecksumResult.Success(sum.toLong(), stored, sum.toLong() == stored)
        }

    suspend fun patchChecksum(address: Int, value: Long, type: com.winols.app.model.DataType, order: ByteOrder) =
        withContext(Dispatchers.IO) {
            bufferManager.writeValue(address, value.toDouble(), type, order)
        }
}