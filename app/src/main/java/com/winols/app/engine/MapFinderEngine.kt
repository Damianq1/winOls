package com.winols.app.engine

import com.winols.app.data.BinaryBufferManager
import com.winols.app.model.MapDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class MapFinderEngine(private val bufferManager: BinaryBufferManager) {

    suspend fun scanForPotentialMaps(
        minRows: Int = 4,
        maxRows: Int = 32,
        minCols: Int = 4,
        maxCols: Int = 32,
        onProgress: ((progressPercent: Int) -> Unit)? = null
    ): List<MapDefinition> = withContext(Dispatchers.Default) {
        val totalSize = bufferManager.size.toInt()
        if (totalSize <= 0) return@withContext emptyList()

        val foundMaps = mutableListOf<MapDefinition>()
        val step = 16
        val totalSteps = totalSize / step

        for (i in 0 until totalSteps) {
            ensureActive()

            val offset = i * step
            if (offset + (maxRows * maxCols * 2) > totalSize) break

            if (isPlausibleMapHeader(offset)) {
                val detectedRows = 16
                val detectedCols = 16
                foundMaps.add(
                    MapDefinition(
                        name = "Detected_Map_0x${offset.toString(16).uppercase()}",
                        startAddress = offset,
                        rows = detectedRows,
                        columns = detectedCols,
                        is16Bit = true,
                        isSigned = false
                    )
                )
            }

            if (i % 256 == 0 && onProgress != null) {
                val progress = ((i.toFloat() / totalSteps) * 100).toInt()
                onProgress(progress)
            }
        }

        onProgress?.invoke(100)
        foundMaps
    }

    private suspend fun isPlausibleMapHeader(offset: Int): Boolean = withContext(Dispatchers.Default) {
        val sample = bufferManager.getBytes(offset, 8)
        val nonZeroCount = sample.count { it.toInt() != 0 }
        nonZeroCount in 2..6
    }
}