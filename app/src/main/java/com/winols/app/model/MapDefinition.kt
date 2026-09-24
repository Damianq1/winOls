package com.winols.app.model

import com.winols.app.data.DataFormat

data class AxisDefinition(
    val name: String,
    val address: Int,
    val length: Int,
    val format: DataFormat,
    val factor: Double = 1.0,
    val offset: Double = 0.0,
    val unit: String = ""
)

data class MapDefinition(
    val id: String,
    val name: String,
    val startAddress: Int,
    val rows: Int,
    val cols: Int,
    val cellFormat: DataFormat,
    val factor: Double = 1.0,
    val offset: Double = 0.0,
    val unit: String = "",
    val xAxis: AxisDefinition? = null,
    val yAxis: AxisDefinition? = null,
    val confidence: Float = 1.0f
) {
    val totalElements: Int get() = rows * cols
    val byteSize: Int get() = totalElements * cellFormat.bytesCount

    fun rawToPhysical(rawValue: Long): Double = (rawValue * factor) + offset

    fun physicalToRaw(physicalValue: Double): Long {
        val base = (physicalValue - offset) / factor
        return base.toLong()
    }
}