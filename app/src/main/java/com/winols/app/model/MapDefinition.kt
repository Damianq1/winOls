package com.winols.app.model

data class AxisDefinition(
    val startAddress: Int = 0,
    val length: Int = 0,
    val isInline: Boolean = false,
    val representation: DataRepresentation = DataRepresentation(),
    val id: String = "",
    val unit: String = ""
)

data class MapDefinition(
    val id: String,
    val name: String,
    val startAddress: Int,
    val rows: Int,
    val columns: Int,
    val representation: DataRepresentation = DataRepresentation(),
    val xAxis: AxisDefinition? = null,
    val yAxis: AxisDefinition? = null,
    val confidence: Double = 1.0
) {
    val totalElements: Int
        get() = rows * columns

    val sizeInBytes: Int
        get() = totalElements * representation.bitDepth.bytesPerElement
}