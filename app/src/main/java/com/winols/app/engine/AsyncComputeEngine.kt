package com.winols.app.engine

import com.winols.app.data.BinaryBufferManager
import com.winols.app.model.MapDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Silnik asynchronicznych obliczeń dla CPU-heavy zadań:
 * skanowania binarnego pamięci ECU, weryfikacji sum kontrolnych i korekcji bloków.
 */
class AsyncComputeEngine(
    private val mapFinderEngine: MapFinderEngine = MapFinderEngine(),
    private val checksumEngine: ChecksumEngine = ChecksumEngine()
) {

    suspend fun scanMapsAsync(
        buffer: ByteArray,
        minRows: Int = 4,
        maxRows: Int = 32,
        minCols: Int = 4,
        maxCols: Int = 32,
        onProgress: ((Float) -> Unit)? = null
    ): List<MapDefinition> = withContext(Dispatchers.Default) {
        // Głęboka kopia na potrzeby izolacji obliczeń jeśli bufor mógłby ulec mutacji
        val localCopy = buffer.copyOf()
        mapFinderEngine.scanForPotentialMaps(localCopy, minRows, maxRows, minCols, maxCols)
    }

    suspend fun verifyChecksumAsync(
        buffer: ByteArray,
        startAddress: Int,
        endAddress: Int,
        checksumLocation: Int,
        family: ChecksumFamily
    ): ChecksumResult = withContext(Dispatchers.Default) {
        checksumEngine.verifyBlock(buffer, startAddress, endAddress, checksumLocation, family)
    }

    suspend fun patchChecksumAsync(
        buffer: ByteArray,
        startAddress: Int,
        endAddress: Int,
        checksumLocation: Int,
        family: ChecksumFamily
    ): Boolean = withContext(Dispatchers.Default) {
        checksumEngine.patchChecksum(buffer, startAddress, endAddress, checksumLocation, family)
    }

    suspend fun batchRecalculateValuesAsync(
        bufferManager: BinaryBufferManager,
        map: MapDefinition,
        transform: (Double) -> Double
    ) = withContext(Dispatchers.Default) {
        var currentAddr = map.startAddress
        val step = map.dataType.byteSize.toLong()

        for (r in 0 until map.rows) {
            for (c in 0 until map.columns) {
                val rawVal = bufferManager.readValue(currentAddr, map.dataType)
                val engVal = (rawVal * map.factor) + map.offset
                val transformedEng = transform(engVal)
                val transformedRaw = if (map.factor != 0.0) {
                    (transformedEng - map.offset) / map.factor
                } else 0.0
                bufferManager.writeValue(currentAddr, map.dataType, transformedRaw)
                currentAddr += step
            }
        }
    }
}