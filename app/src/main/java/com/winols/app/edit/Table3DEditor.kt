package com.winols.app.edit

import com.winols.app.model.Table3DModel

/**
 * Operacje wsadowe i edycja precyzyjna komórek w mapach 3D.
 */
class Table3DEditor {

    fun applyAbsolute(table: Table3DModel, selection: Set<Pair<Int, Int>>, value: Double): Table3DModel {
        val updated = deepCopyZ(table.zValues)
        for ((row, col) in selection) {
            if (row in 0 until table.ySize && col in 0 until table.xSize) {
                updated[row][col] = (value - table.offset) / if (table.factor != 0.0) table.factor else 1.0
            }
        }
        return table.copy(zValues = updated)
    }

    fun applyRelativePercent(table: Table3DModel, selection: Set<Pair<Int, Int>>, percentDelta: Double): Table3DModel {
        val multiplier = 1.0 + (percentDelta / 100.0)
        val updated = deepCopyZ(table.zValues)
        for ((row, col) in selection) {
            if (row in 0 until table.ySize && col in 0 until table.xSize) {
                updated[row][col] = updated[row][col] * multiplier
            }
        }
        return table.copy(zValues = updated)
    }

    fun applyOffset(table: Table3DModel, selection: Set<Pair<Int, Int>>, delta: Double): Table3DModel {
        val rawDelta = if (table.factor != 0.0) delta / table.factor else delta
        val updated = deepCopyZ(table.zValues)
        for ((row, col) in selection) {
            if (row in 0 until table.ySize && col in 0 until table.xSize) {
                updated[row][col] += rawDelta
            }
        }
        return table.copy(zValues = updated)
    }

    fun interpolateSelection(
        table: Table3DModel,
        startRow: Int,
        startCol: Int,
        endRow: Int,
        endCol: Int
    ): Table3DModel {
        val updated = deepCopyZ(table.zValues)
        val minR = minOf(startRow, endRow)
        val maxR = maxOf(startRow, endRow)
        val minC = minOf(startCol, endCol)
        val maxC = maxOf(startCol, endCol)

        val q11 = updated[minR][minC]
        val q12 = updated[maxR][minC]
        val q21 = updated[minR][maxC]
        val q22 = updated[maxR][maxC]

        val rowSpan = (maxR - minR).toDouble()
        val colSpan = (maxC - minC).toDouble()

        for (r in minR..maxR) {
            val rWeight = if (rowSpan > 0) (r - minR) / rowSpan else 0.0
            for (c in minC..maxC) {
                val cWeight = if (colSpan > 0) (c - minC) / colSpan else 0.0
                // Bilinear interpolation
                val top = (1.0 - cWeight) * q11 + cWeight * q21
                val bottom = (1.0 - cWeight) * q12 + cWeight * q22
                updated[r][c] = (1.0 - rWeight) * top + rWeight * bottom
            }
        }

        return table.copy(zValues = updated)
    }

    private fun deepCopyZ(matrix: Array<DoubleArray>): Array<DoubleArray> {
        return Array(matrix.size) { r -> matrix[r].clone() }
    }
}