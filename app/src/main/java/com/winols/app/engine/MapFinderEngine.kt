package com.winols.app.engine

import com.winols.app.data.BinaryBufferManager
import com.winols.app.model.AxisDefinition
import com.winols.app.model.DataType
import com.winols.app.model.MapDefinition
import com.winols.app.model.MapOrganization
import java.util.UUID

class MapFinderEngine(private val bufferManager: BinaryBufferManager) {

    /**
     * Wyszukuje potencjalne mapy 2D/3D z nagłówkami osi (heurystyka w stylu Bosch ME7/EDC15/EDC16).
     * Wykrywa sekwencje: [ID_X (1-2B)] [LEN_X (1-2B)] [OŚ_X ...] [ID_Y (1-2B)] [LEN_Y (1-2B)] [OŚ_Y ...] [DANE]
     */
    fun scanForMaps(minDim: Int = 4, maxDim: Int = 32): List<MapDefinition> {
        val detectedMaps = mutableListOf<MapDefinition>()
        val bufSize = bufferManager.size
        var offset = 0L

        while (offset < bufSize - 32) {
            val lenX = bufferManager.readValue(offset, DataType.UINT8).toInt()
            val lenY = bufferManager.readValue(offset + 1, DataType.UINT8).toInt()

            if (lenX in minDim..maxDim && lenY in minDim..maxDim) {
                val axisXSize = lenX * 2
                val axisYSize = lenY * 2
                val mapDataSize = lenX * lenY * 2
                val totalCandidateSize = 2 + axisXSize + axisYSize + mapDataSize

                if (offset + totalCandidateSize <= bufSize) {
                    val axisXOffset = offset + 2
                    val axisYOffset = axisXOffset + axisXSize
                    val dataOffset = axisYOffset + axisYSize

                    if (isMonotonicIncreasing(axisXOffset, lenX, DataType.UINT16_LE) &&
                        isMonotonicIncreasing(axisYOffset, lenY, DataType.UINT16_LE)
                    ) {
                        val map = MapDefinition(
                            id = UUID.randomUUID().toString(),
                            name = "AutoMap_0x${offset.toString(16).uppercase()}_${lenX}x${lenY}",
                            organization = MapOrganization.THREE_D,
                            startAddress = dataOffset,
                            rows = lenY,
                            cols = lenX,
                            cellDataType = DataType.UINT16_LE,
                            xAxis = AxisDefinition("X-Axis", "", axisXOffset, lenX, DataType.UINT16_LE),
                            yAxis = AxisDefinition("Y-Axis", "", axisYOffset, lenY, DataType.UINT16_LE)
                        )
                        detectedMaps.add(map)
                        offset += totalCandidateSize
                        continue
                    }
                }
            }
            offset += 2
        }

        return detectedMaps
    }

    private fun isMonotonicIncreasing(offset: Long, count: Int, type: DataType): Boolean {
        var prev = bufferManager.readValue(offset, type)
        for (i in 1 until count) {
            val cur = bufferManager.readValue(offset + i * type.byteSize, type)
            if (cur < prev) return false
            prev = cur
        }
        return true
    }
}