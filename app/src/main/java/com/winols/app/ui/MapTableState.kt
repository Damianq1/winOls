package com.winols.app.ui

enum class CellDisplayMode {
    PHYSICAL_VALUES,
    RAW_HEX,
    RAW_DECIMAL,
    ABSOLUTE_DIFFERENCE,
    PERCENTAGE_DIFFERENCE
}

data class SelectionRange(
    val startRow: Int,
    val startCol: Int,
    val endRow: Int,
    val endCol: Int
) {
    val rowIndices: IntProgression
        get() = if (startRow <= endRow) startRow..endRow else startRow downTo endRow

    val colIndices: IntProgression
        get() = if (startCol <= endCol) startCol..endCol else startCol downTo endCol

    fun toCellList(): List<Pair<Int, Int>> {
        val list = mutableListOf<Pair<Int, Int>>()
        val minR = minOf(startRow, endRow)
        val maxR = maxOf(startRow, endRow)
        val minC = minOf(startCol, endCol)
        val maxC = maxOf(startCol, endCol)
        for (r in minR..maxR) {
            for (c in minC..maxC) {
                list.add(Pair(r, c))
            }
        }
        return list
    }

    fun contains(row: Int, col: Int): Boolean {
        val minR = minOf(startRow, endRow)
        val maxR = maxOf(startRow, endRow)
        val minC = minOf(startCol, endCol)
        val maxC = maxOf(startCol, endCol)
        return row in minR..maxR && col in minC..maxC
    }
}