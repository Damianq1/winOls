package com.winols.app.edit

import com.winols.app.data.BinaryBufferManager
import com.winols.app.model.AxisDefinition
import com.winols.app.model.MapDefinition

class MapEditor(
    private val bufferManager: BinaryBufferManager,
    private val map: MapDefinition
) {

    fun getCellValue(row: Int, col: Int): Double {
        checkIndices(row, col)
        val cellOffset = map.startAddress + (row * map.cols + col) * map.cellDataType.byteSize
        val raw = bufferManager.readValue(cellOffset, map.cellDataType)
        return raw * map.factor + map.offset
    }

    fun setCellValue(row: Int, col: Int, physicalValue: Double) {
        checkIndices(row, col)
        val raw = (physicalValue - map.offset) / map.factor
        val cellOffset = map.startAddress + (row * map.cols + col) * map.cellDataType.byteSize
        bufferManager.writeValue(cellOffset, raw, map.cellDataType)
    }

    fun getMatrix(): Array<DoubleArray> {
        val matrix = Array(map.rows) { DoubleArray(map.cols) }
        for (r in 0 until map.rows) {
            for (c in 0 until map.cols) {
                matrix[r][c] = getCellValue(r, c)
            }
        }
        return matrix
    }

    fun modifyRelativePercent(percentage: Double, selectedCells: List<Pair<Int, Int>>? = null) {
        val multiplier = 1.0 + (percentage / 100.0)
        val targets = selectedCells ?: (0 until map.rows).flatMap { r -> (0 until map.cols).map { c -> r to c } }

        for ((r, c) in targets) {
            val current = getCellValue(r, c)
            setCellValue(r, c, current * multiplier)
        }
    }

    fun modifyAbsoluteOffset(delta: Double, selectedCells: List<Pair<Int, Int>>? = null) {
        val targets = selectedCells ?: (0 until map.rows).flatMap { r -> (0 until map.cols).map { c -> r to c } }

        for ((r, c) in targets) {
            val current = getCellValue(r, c)
            setCellValue(r, c, current + delta)
        }
    }

    fun interpolate2D(
        rStart: Int, cStart: Int,
        rEnd: Int, cEnd: Int
    ) {
        val v00 = getCellValue(rStart, cStart)
        val v01 = getCellValue(rStart, cEnd)
        val v10 = getCellValue(rEnd, cStart)
        val v11 = getCellValue(rEnd, cEnd)

        val rowRange = (rEnd - rStart).toDouble()
        val colRange = (cEnd - cStart).toDouble()

        for (r in rStart..rEnd) {
            val rWeight = if (rowRange == 0.0) 0.0 else (r - rStart) / rowRange
            for (c in cStart..cEnd) {
                val cWeight = if (colRange == 0.0) 0.0 else (c - cStart) / colRange
                val top = v00 * (1.0 - cWeight) + v01 * cWeight
                val bottom = v10 * (1.0 - cWeight) + v11 * cWeight
                val value = top * (1.0 - rWeight) + bottom * rWeight
                setCellValue(r, c, value)
            }
        }
    }

    fun getAxisValues(axis: AxisDefinition): DoubleArray {
        val result = DoubleArray(axis.length)
        for (i in 0 until axis.length) {
            val offset = axis.startAddress + i * axis.dataType.byteSize
            val raw = bufferManager.readValue(offset, axis.dataType)
            result[i] = raw * axis.factor + axis.offset
        }
        return result
    }

    fun setAxisValue(axis: AxisDefinition, index: Int, physicalValue: Double) {
        require(index in 0 until axis.length) { "Indeks osi poza zakresem: $index" }
        val raw = (physicalValue - axis.offset) / axis.factor
        val offset = axis.startAddress + index * axis.dataType.byteSize
        bufferManager.writeValue(offset, raw, axis.dataType)
    }

    private fun checkIndices(row: Int, col: Int) {
        require(row in 0 until map.rows && col in 0 until map.cols) {
            "Indeksy poza granicami mapy: row=$row (max ${map.rows - 1}), col=$col (max ${map.cols - 1})"
        }
    }
}