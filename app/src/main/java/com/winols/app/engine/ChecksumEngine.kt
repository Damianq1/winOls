package com.winols.app.engine

import com.winols.app.data.BinaryBufferManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class ChecksumEngine(private val bufferManager: BinaryBufferManager) {

    suspend fun calculateSum16(
        startAddress: Int,
        endAddress: Int,
        onProgress: ((Int) -> Unit)? = null
    ): Int = withContext(Dispatchers.Default) {
        require(startAddress in 0..endAddress) { "Invalid address range" }
        val length = endAddress - startAddress
        val chunkSize = 4096
        var currentSum = 0

        var offset = startAddress
        var processed = 0

        while (offset < endAddress) {
            ensureActive()

            val currentChunkLength = minOf(chunkSize, endAddress - offset)
            val chunk = bufferManager.getBytes(offset, currentChunkLength)

            for (i in 0 until currentChunkLength - 1 step 2) {
                val byteLow = chunk[i].toInt() and 0xFF
                val byteHigh = chunk[i + 1].toInt() and 0xFF
                val word = byteLow or (byteHigh shl 8)
                currentSum = (currentSum + word) and 0xFFFF
            }

            offset += currentChunkLength
            processed += currentChunkLength

            onProgress?.invoke(((processed.toFloat() / length) * 100).toInt())
        }

        currentSum
    }

    suspend fun verifyAndPatch(
        startAddress: Int,
        endAddress: Int,
        checksumAddress: Int
    ): Boolean = withContext(Dispatchers.Default) {
        val calculatedSum = calculateSum16(startAddress, endAddress)

        val patchBytes = byteArrayOf(
            (calculatedSum and 0xFF).toByte(),
            ((calculatedSum shr 8) and 0xFF).toByte()
        )
        bufferManager.writeBytes(checksumAddress, patchBytes)
        true
    }
}