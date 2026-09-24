package com.winols.app.engine

import com.winols.app.data.BinaryBufferManager
import com.winols.app.data.DataFormat
import com.winols.app.model.AxisDefinition
import com.winols.app.model.MapDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

class MapFinderEngine {

    suspend fun scanBuffer(
        bufferManager: BinaryBufferManager,
        minDim: Int = 4,
        maxDim: Int = 20
    ): List<MapDefinition> = withContext(Dispatchers.Default) {
        val detected = mutableListOf<MapDefinition>()
        val totalSize = bufferManager.size
        var offset = 0

        // Skanowanie 16-bitowych sekwencji Bosch/Siemens: [Liczba kolumn (X)] [Liczba wierszy (Y)]
        while (offset < totalSize - 64) {
            val colCandidate = bufferManager.readRawValue(offset, DataFormat.UINT16_BE).toInt()
            val rowCandidate = bufferManager.readRawValue(offset + 2, DataFormat.UINT16_BE).toInt()

            if (colCandidate in minDim..maxDim && rowCandidate in minDim..maxDim) {
                val dataStart = offset + 4
                val dataLength = colCandidate * rowCandidate * 2

                if (dataStart + dataLength <= totalSize) {
                    val score = evaluateMatrixEntropy(bufferManager, dataStart, rowCandidate, colCandidate)
                    if (score > 0.65f) {
                        val map = MapDefinition(
                            id = "MAP_0x${Integer.toHexString(dataStart).uppercase()}",
                            name = "Map_${rowCandidate}x${colCandidate}_0x${Integer.toHexString(dataStart).uppercase()}",
                            startAddress = dataStart,
                            rows = rowCandidate,
                            cols = colCandidate,
                            cellFormat = DataFormat.INT16_BE,
                            factor = 1.0,
                            offset = 0.0,
                            confidence = score,
                            xAxis = AxisDefinition("X-Axis", offset - (colCandidate * 2), colCandidate, DataFormat.INT16_BE),
                            yAxis = AxisDefinition("Y-Axis", offset - (colCandidate * 2) - (rowCandidate * 2), rowCandidate, DataFormat.INT16_BE)
                        )
                        detected.add(map)
                        offset += dataLength + 4
                        continue
                    }
                }
            }
            offset += 2
        }

        detected
    }

    private fun evaluateMatrixEntropy(
        bufferManager: BinaryBufferManager,
        startAddress: Int,
        rows: Int,
        cols: Int
    ): Float {
        var monotonicRows = 0
        var smoothGradients = 0
        var lastVal = -1L

        for (r in 0 until rows) {
            var rowMonotonic = true
            for (c in 0 until cols) {
                val addr = startAddress + (r * cols + c) * 2
                val current = bufferManager.readRawValue(addr, DataFormat.INT16_BE)

                if (c > 0 && current < lastVal) {
                    rowMonotonic = false
                }
                if (c > 0 && abs(current - lastVal) < 8000) {
                    smoothGradients++
                }
                lastVal = current
            }
            if (rowMonotonic) monotonicRows++
        }

        val totalPairs = rows * (cols - 1)
        val gradientScore = smoothGradients.toFloat() / totalPairs.coerceAtLeast(1)
        val monotonicityScore = monotonicRows.toFloat() / rows.coerceAtLeast(1)

        return (gradientScore * 0.6f) + (monotonicityScore * 0.4f)
    }
}