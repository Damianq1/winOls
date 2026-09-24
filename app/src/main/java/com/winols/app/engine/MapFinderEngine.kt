package com.winols.app.engine

import com.winols.app.data.BinaryBufferManager
import com.winols.app.model.AxisDefinition
import com.winols.app.model.DataType
import com.winols.app.model.MapDefinition
import java.nio.ByteOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MapFinderEngine(private val bufferManager: BinaryBufferManager = BinaryBufferManager.instance) {

    suspend fun findPotentialMaps(
        minRows: Int = 4,
        maxRows: Int = 32,
        minCols: Int = 4,
        maxCols: Int = 32
    ): List<MapDefinition> = withContext(Dispatchers.Default) {
        val detected = mutableListOf<MapDefinition>()
        val totalBytes = bufferManager.currentFileSize
        if (totalBytes < 128) return@withContext detected

        var offset = 0
        while (offset < totalBytes - 64) {
            // Heurystyka wykrywania nagłówków osi typu Bosch: [Length X][Length Y]
            val dimX = bufferManager.readValue(offset, DataType.UBYTE, ByteOrder.BIG_ENDIAN).toInt()
            val dimY = bufferManager.readValue(offset + 1, DataType.UBYTE, ByteOrder.BIG_ENDIAN).toInt()

            if (dimX in minCols..maxCols && dimY in minRows..maxRows) {
                val candidateStart = offset + 2
                val mapDataSize = dimX * dimY * DataType.UWORD.byteSize

                if (candidateStart + mapDataSize <= totalBytes) {
                    val variance = calculateVariance(candidateStart, dimX * dimY, DataType.UWORD)
                    // Filtrujemy płaskie bloki wypełnione 0x00 lub 0xFF
                    if (variance > 25.0) {
                        detected.add(
                            MapDefinition(
                                id = "MAP_${Integer.toHexString(candidateStart).uppercase()}",
                                name = "Potential Map ${dimY}x${dimX} @ 0x${Integer.toHexString(candidateStart).uppercase()}",
                                startAddress = candidateStart,
                                rows = dimY,
                                columns = dimX,
                                dataType = DataType.UWORD,
                                byteOrder = ByteOrder.BIG_ENDIAN,
                                xAxis = AxisDefinition(address = offset, length = dimX, name = "X"),
                                yAxis = AxisDefinition(address = offset + 1, length = dimY, name = "Y"),
                                confidence = 0.75f
                            )
                        )
                        offset += 2 + mapDataSize
                        continue
                    }
                }
            }
            offset += 2
        }
        detected
    }

    private fun calculateVariance(startAddr: Int, count: Int, type: DataType): Double {
        var sum = 0.0
        var sqSum = 0.0
        val sampleSize = count.coerceAtMost(128)

        for (i in 0 until sampleSize) {
            val v = bufferManager.readValue(startAddr + (i * type.byteSize), type, ByteOrder.BIG_ENDIAN)
            sum += v
            sqSum += v * v
        }
        val mean = sum / sampleSize
        return (sqSum / sampleSize) - (mean * mean)
    }
}