package com.winols.app.edit

import com.winols.app.BinModel
import com.winols.app.model.MapDefinition

class MapEditor(private val binModel: BinModel) {

    fun modifySingleCell(map: MapDefinition, row: Int, col: Int, newValue: Double) {
        binModel.setCellValue(map, row, col, newValue)
    }

    fun applyOffsetToSelection(
        map: MapDefinition,
        selectedCells: List<Pair<Int, Int>>,
        offset: Double
    ) {
        for ((r, c) in selectedCells) {
            val current = binModel.getCellValue(map, r, c)
            binModel.setCellValue(map, r, c, current + offset)
        }
    }

    fun applyPercentageToSelection(
        map: MapDefinition,
        selectedCells: List<Pair<Int, Int>>,
        percentage: Double
    ) {
        for ((r, c) in selectedCells) {
            binModel.applyPercentageChange(map, r, c, percentage)
        }
    }

    fun interpolate2D(
        map: MapDefinition,
        startRow: Int,
        startCol: Int,
        endRow: Int,
        endCol: Int
    ) {
        val v00 = binModel.getCellValue(map, startRow, startCol)
        val v01 = binModel.getCellValue(map, startRow, endCol)
        val v10 = binModel.getCellValue(map, endRow, startCol)
        val v11 = binModel.getCellValue(map, endRow, endCol)

        val rowSpan = (endRow - startRow).coerceAtLeast(1)
        val colSpan = (endCol - startCol).coerceAtLeast(1)

        for (r in startRow..endRow) {
            val ry = (r - startRow).toDouble() / rowSpan
            for (c in startCol..endCol) {
                val cx = (c - startCol).toDouble() / colSpan
                val interp = (1 - ry) * ((1 - cx) * v00 + cx * v01) +
                        ry * ((1 - cx) * v10 + cx * v11)
                binModel.setCellValue(map, r, c, interp)
            }
        }
    }
}