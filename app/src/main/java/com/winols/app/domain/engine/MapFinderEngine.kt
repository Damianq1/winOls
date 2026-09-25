package com.winols.app.domain.engine

import com.winols.app.domain.model.AxisDefinition
import com.winols.app.domain.model.DataType
import com.winols.app.domain.model.Endianness
import com.winols.app.domain.model.MapDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MapFinderEngine {

    suspend fun scanBinary(
        buffer: ByteArray,
        endianness: Endianness = Endianness.BIG_ENDIAN,
        minDimension: Int = 4,
        maxDimension: Int = 32
    ): List<MapDefinition> = withContext(Dispatchers.Default) {
        val detectedMaps = mutableListOf<MapDefinition>()
        if (buffer.size < 64) return@withContext detectedMaps

        var offset = 0
        while (offset < buffer.size - 64) {
            val potentialMap = inspectOffset(buffer, offset, endianness, minDimension, maxDimension)
            if (potentialMap != null) {
                detectedMaps.add(potentialMap)
                offset += potentialMap.totalBytes
            } else {
                offset += 2 // Wyrównanie do słowa 16-bit
            }
        }
        detectedMaps
    }

    private fun inspectOffset(
        buffer: ByteArray,
        offset: Int,
        endianness: Endianness,
        minDim: Int,
        maxDim: Int
    ): MapDefinition? {
        val order = if (endianness == Endianness.BIG_ENDIAN) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN
        val byteBuffer = ByteBuffer.wrap(buffer).order(order)

        // Wzorzec 1: Standardowy nagłówek wymiarów osi 16-bit [Rows (Y)] x [Columns (X)]
        val dimY = byteBuffer.getShort(offset).toInt() and 0xFFFF
        val dimX = byteBuffer.getShort(offset + 2).toInt() and 0xFFFF

        if (dimY in minDim..maxDim && dimX in minDim..maxDim) {
            val mapDataStart = offset + 4
            val mapBytes = dimX * dimY * DataType.UINT16.byteSize

            if (mapDataStart + mapBytes <= buffer.size) {
                val score = calculateMatrixPlausibility(byteBuffer, mapDataStart, dimY, dimX)
                if (score > 0.65f) {
                    val hexAddr = "0x" + offset.toString(16).uppercase().padStart(6, '0')
                    return MapDefinition(
                        id = "MAP_$hexAddr",
                        name = "Map 2D/3D ($dimY x $dimX)",
                        address = mapDataStart,
                        rows = dimY,
                        columns = dimX,
                        dataType = DataType.UINT16,
                        endianness = endianness,
                        xAxis = AxisDefinition(
                            id = "AXIS_X_$hexAddr",
                            name = "X Axis",
                            address = 0,
                            length = dimX,
                            dataType = DataType.UINT16,
                            endianness = endianness
                        ),
                        yAxis = AxisDefinition(
                            id = "AXIS_Y_$hexAddr",
                            name = "Y Axis",
                            address = 0,
                            length = dimY,
                            dataType = DataType.UINT16,
                            endianness = endianness
                        ),
                        confidenceScore = score
                    )
                }
            }
        }

        // Wzorzec 2: Wyszukiwanie wektora monotonicznego (oś RPM / ciśnienia) poprzedzającego tablicę
        val axisCandidate = checkMonotonicAxis(byteBuffer, offset, minDim, maxDim)
        if (axisCandidate != null) {
            val (axisLength, axisBytes) = axisCandidate
            val potentialGridStart = offset + axisBytes
            val potentialRows = axisLength
            val potentialCols = axisLength

            val totalGridBytes = potentialRows * potentialCols * DataType.UINT16.byteSize
            if (potentialGridStart + totalGridBytes <= buffer.size) {
                val score = calculateMatrixPlausibility(byteBuffer, potentialGridStart, potentialRows, potentialCols)
                if (score > 0.70f) {
                    val hexAddr = "0x" + potentialGridStart.toString(16).uppercase().padStart(6, '0')
                    return MapDefinition(
                        id = "MAP_AXIS_$hexAddr",
                        name = "Identified Axis Map ($potentialRows x $potentialCols)",
                        address = potentialGridStart,
                        rows = potentialRows,
                        columns = potentialCols,
                        dataType = DataType.UINT16,
                        endianness = endianness,
                        confidenceScore = score
                    )
                }
            }
        }

        return null
    }

    private fun checkMonotonicAxis(
        buffer: ByteBuffer,
        offset: Int,
        minDim: Int,
        maxDim: Int
    ): Pair<Int, Int>? {
        for (len in minDim..maxDim) {
            val byteCount = len * 2
            if (offset + byteCount > buffer.capacity()) break

            var strictlyIncreasing = true
            var prevVal = buffer.getShort(offset).toInt() and 0xFFFF

            for (i in 1 until len) {
                val currentVal = buffer.getShort(offset + i * 2).toInt() and 0xFFFF
                if (currentVal <= prevVal || currentVal > 65000) {
                    strictlyIncreasing = false
                    break
                }
                prevVal = currentVal
            }

            if (strictlyIncreasing) {
                return Pair(len, byteCount)
            }
        }
        return null
    }

    private fun calculateMatrixPlausibility(
        buffer: ByteBuffer,
        startOffset: Int,
        rows: Int,
        cols: Int
    ): Float {
        var validElements = 0
        var totalElements = rows * cols
        var nonZeroCount = 0

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val pos = startOffset + (r * cols + c) * 2
                val value = buffer.getShort(pos).toInt() and 0xFFFF

                if (value != 0 && value != 0xFFFF) {
                    nonZeroCount++
                }
                // Filtruje losowy szum i puste bloki
                if (value in 1..65500) {
                    validElements++
                }
            }
        }

        val nonZeroRatio = nonZeroCount.toFloat() / totalElements
        val validityRatio = validElements.toFloat() / totalElements

        if (nonZeroRatio < 0.4f || nonZeroRatio > 0.99f && validityRatio == 0.0f) {
            return 0.0f
        }

        return (nonZeroRatio * 0.4f) + (validityRatio * 0.6f)
    }
}