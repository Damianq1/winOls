package com.winols.app.domain.model

import kotlin.math.roundToInt

data class CellCoordinate(val row: Int, val col: Int)

data class SelectionArea(
    val startRow: Int,
    val startCol: Int,
    val endRow: Int,
    val endCol: Int
) {
    val minRow: Int get() = minOf(startRow, endRow)
    val maxRow: Int get() = maxOf(startRow, endRow)
    val minCol: Int get() = minOf(startCol, endCol)
    val maxCol: Int get() = maxOf(startCol, endCol)

    fun contains(row: Int, col: Int): Boolean {
        return row in minRow..maxRow && col in minCol..maxCol
    }
}

class MapTable(
    val rows: Int,
    val cols: Int,
    initialData: IntArray? = null,
    val is16Bit: Boolean = false,
    val isSigned: Boolean = false
) {
    val data: IntArray = initialData?.copyOf() ?: IntArray(rows * cols)

    private val minLimit: Int
        get() = when {
            is16Bit && isSigned -> Short.MIN_VALUE.toInt()
            is16Bit && !isSigned -> 0
            !is16Bit && isSigned -> Byte.MIN_VALUE.toInt()
            else -> 0
        }

    private val maxLimit: Int
        get() = when {
            is16Bit && isSigned -> Short.MAX_VALUE.toInt()
            is16Bit && !isSigned -> 0xFFFF
            !is16Bit && isSigned -> Byte.MAX_VALUE.toInt()
            else -> 0xFF
        }

    fun get(row: Int, col: Int): Int {
        require(row in 0 until rows && col in 0 until cols)
        return data[row * cols + col]
    }

    fun set(row: Int, col: Int, value: Int) {
        require(row in 0 until rows && col in 0 until cols)
        data[row * cols + col] = value.coerceIn(minLimit, maxLimit)
    }

    fun applyOffset(selection: SelectionArea, delta: Int) {
        for (r in selection.minRow..selection.maxRow) {
            for (c in selection.minCol..selection.maxCol) {
                set(r, c, get(r, c) + delta)
            }
        }
    }

    fun applyPercentage(selection: SelectionArea, percentage: Double) {
        val factor = 1.0 + (percentage / 100.0)
        for (r in selection.minRow..selection.maxRow) {
            for (c in selection.minCol..selection.maxCol) {
                val current = get(r, c)
                val updated = (current * factor).roundToInt()
                set(r, c, updated)
            }
        }
    }

    fun setValue(selection: SelectionArea, value: Int) {
        for (r in selection.minRow..selection.maxRow) {
            for (c in selection.minCol..selection.maxCol) {
                set(r, c, value)
            }
        }
    }

    fun applySmoothing(selection: SelectionArea) {
        val snapshot = data.copyOf()
        fun readSnapshot(r: Int, c: Int): Int = snapshot[r * cols + c]

        for (r in selection.minRow..selection.maxRow) {
            for (c in selection.minCol..selection.maxCol) {
                var sum = 0
                var count = 0

                for (dr in -1..1) {
                    for (dc in -1..1) {
                        val nr = r + dr
                        val nc = c + dc
                        if (nr in 0 until rows && nc in 0 until cols) {
                            sum += readSnapshot(nr, nc)
                            count++
                        }
                    }
                }
                if (count > 0) {
                    set(r, c, (sum.toDouble() / count).roundToInt())
                }
            }
        }
    }

    fun copy(): MapTable {
        return MapTable(rows, cols, data.copyOf(), is16Bit, isSigned)
    }
}