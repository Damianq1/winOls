package com.winols.app.domain.detector

import com.winols.app.domain.model.DataType
import com.winols.app.domain.model.EcuMap
import com.winols.app.domain.model.MapAxis
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MapScanner(
    private val minCols: Int = 4,
    private val maxCols: Int = 32,
    private val minRows: Int = 2,
    private val maxRows: Int = 32
) {

    /**
     * Skanuje bufor binarny ECU w poszukiwaniu potencjalnych map 2D i 3D
     */
    fun scanBinary(binary: ByteArray, byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN): List<EcuMap> {
        val detectedMaps = mutableListOf<EcuMap>()
        val buffer = ByteBuffer.wrap(binary).order(byteOrder)
        val limit = binary.size - (minCols * minRows * 2)

        var offset = 0
        while (offset < limit) {
            // 1. Sprawdzenie nagłówków wymiarów w stylu Bosch (16-bitowe znaczniki długości)
            val candidate = evaluateHeaderAt(buffer, offset, byteOrder)
            if (candidate != null) {
                detectedMaps.add(candidate)
                // Przesunięcie o rozmiar wykrytej mapy, aby uniknąć nakładających się duplikatów
                offset = candidate.dataAddress + (candidate.rows * candidate.cols * candidate.dataType.byteSize)
                continue
            }

            // 2. Weryfikacja bezpośrednich osi monotonicznych (dla map bez jawnego nagłówka)
            val heuristicCandidate = evaluateHeuristicMapAt(buffer, offset, byteOrder)
            if (heuristicCandidate != null) {
                detectedMaps.add(heuristicCandidate)
                offset = heuristicCandidate.dataAddress + (heuristicCandidate.rows * heuristicCandidate.cols * heuristicCandidate.dataType.byteSize)
                continue
            }

            offset += 2 // Wyrównanie do słów 16-bitowych
        }

        return detectedMaps
    }

    private fun evaluateHeaderAt(buffer: ByteBuffer, offset: Int, byteOrder: ByteOrder): EcuMap? {
        if (offset + 4 > buffer.capacity()) return null

        val rowsCandidate = buffer.getShort(offset).toInt() and 0xFFFF
        val colsCandidate = buffer.getShort(offset + 2).toInt() and 0xFFFF

        if (rowsCandidate in minRows..maxRows && colsCandidate in minCols..maxCols) {
            val axisXOffset = offset + 4
            val axisX = extractAxis(buffer, axisXOffset, colsCandidate, DataType.UINT16_LE)
            
            if (axisX != null && axisX.isMonotonic) {
                val axisYOffset = axisXOffset + (colsCandidate * 2)
                val axisY = extractAxis(buffer, axisYOffset, rowsCandidate, DataType.UINT16_LE)

                if (axisY != null && axisY.isMonotonic) {
                    val dataOffset = axisYOffset + (rowsCandidate * 2)
                    val mapData = extractMatrix(buffer, dataOffset, rowsCandidate, colsCandidate, DataType.UINT16_LE)

                    if (mapData != null && isValidDataBlock(mapData)) {
                        return EcuMap(
                            id = "MAP_0x${offset.toString(16).uppercase()}",
                            dataAddress = dataOffset,
                            rows = rowsCandidate,
                            cols = colsCandidate,
                            dataType = DataType.UINT16_LE,
                            xAxis = axisX,
                            yAxis = axisY,
                            data = mapData,
                            confidenceScore = 0.95f
                        )
                    }
                }
            }
        }
        return null
    }

    private fun evaluateHeuristicMapAt(buffer: ByteBuffer, offset: Int, byteOrder: ByteOrder): EcuMap? {
        // Detekcja heurystyczna dla wariantów 16x16, 16x1, 12x12
        val standardCols = listOf(16, 12, 8)
        val standardRows = listOf(16, 12, 8, 1)

        for (cols in standardCols) {
            val axis = extractAxis(buffer, offset, cols, DataType.UINT16_LE) ?: continue
            if (axis.isMonotonic) {
                for (rows in standardRows) {
                    val dataOffset = offset + (cols * 2)
                    val data = extractMatrix(buffer, dataOffset, rows, cols, DataType.UINT16_LE) ?: continue
                    
                    if (isValidDataBlock(data)) {
                        return EcuMap(
                            id = "MAP_HEUR_0x${offset.toString(16).uppercase()}",
                            dataAddress = dataOffset,
                            rows = rows,
                            cols = cols,
                            dataType = DataType.UINT16_LE,
                            xAxis = axis,
                            yAxis = null,
                            data = data,
                            confidenceScore = 0.70f
                        )
                    }
                }
            }
        }
        return null
    }

    private fun extractAxis(buffer: ByteBuffer, offset: Int, length: Int, dataType: DataType): MapAxis? {
        if (offset + (length * dataType.byteSize) > buffer.capacity()) return null
        
        val values = mutableListOf<Double>()
        for (i in 0 until length) {
            val v = buffer.getShort(offset + i * 2).toInt() and 0xFFFF
            values.add(v.toDouble())
        }

        val monotonic = isStrictlyMonotonic(values)
        return MapAxis(
            address = offset,
            length = length,
            dataType = dataType,
            values = values,
            isMonotonic = monotonic
        )
    }

    private fun extractMatrix(
        buffer: ByteBuffer,
        offset: Int,
        rows: Int,
        cols: Int,
        dataType: DataType
    ): List<List<Double>>? {
        val totalBytes = rows * cols * dataType.byteSize
        if (offset + totalBytes > buffer.capacity()) return null

        val matrix = ArrayList<List<Double>>(rows)
        var currentOffset = offset
        for (r in 0 until rows) {
            val rowValues = ArrayList<Double>(cols)
            for (c in 0 until cols) {
                val value = buffer.getShort(currentOffset).toInt() and 0xFFFF
                rowValues.add(value.toDouble())
                currentOffset += dataType.byteSize
            }
            matrix.add(rowValues)
        }
        return matrix
    }

    private fun isStrictlyMonotonic(values: List<Double>): Boolean {
        if (values.size < 3) return false
        var increasing = true
        var decreasing = true

        for (i in 0 until values.size - 1) {
            if (values[i] >= values[i + 1]) increasing = false
            if (values[i] <= values[i + 1]) decreasing = false
        }
        return increasing || decreasing
    }

    private fun isValidDataBlock(data: List<List<Double>>): Boolean {
        val flat = data.flatten()
        if (flat.isEmpty()) return false

        val first = flat.first()
        val allSame = flat.all { it == first }
        if (allSame) return false // Wyklucza bloki wypełnienia paddingiem (np. 0x0000 lub 0xFFFF)

        val zeroCount = flat.count { it == 0.0 }
        if (zeroCount.toDouble() / flat.size > 0.85) return false

        return true
    }
}