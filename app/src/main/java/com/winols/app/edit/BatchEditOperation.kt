package com.winols.app.edit

sealed class BatchEditOperation {
    data class AddOffset(val offset: Double) : BatchEditOperation()
    data class SubtractOffset(val offset: Double) : BatchEditOperation()
    data class PercentageChange(val percent: Double) : BatchEditOperation()
    data class SetExactValue(val value: Double) : BatchEditOperation()
    data class Smooth(val factor: Double = 0.5) : BatchEditOperation()
    data class InterpolateHorizontal(val row: Int, val startCol: Int, val endCol: Int) : BatchEditOperation()
    data class InterpolateVertical(val col: Int, val startRow: Int, val endRow: Int) : BatchEditOperation()
    data class Interpolate2D(val rStart: Int, val cStart: Int, val rEnd: Int, val cEnd: Int) : BatchEditOperation()
}