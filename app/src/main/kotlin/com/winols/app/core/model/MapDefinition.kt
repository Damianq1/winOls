package com.winols.app.core.model

import java.nio.ByteOrder

enum class DataType(val byteSize: Int) {
    UBYTE(1),
    SBYTE(1),
    UWORD_BE(2),
    UWORD_LE(2),
    SWORD_BE(2),
    SWORD_LE(2)
}

enum class MapDimension {
    ONE_D,   // 1D Single curve / axis
    TWO_D,   // 2D Line (X x Y)
    THREE_D  // 3D Surface (X x Y x Z)
}

data class AxisDefinition(
    val description: String,
    val offset: Int,
    val length: Int,
    val dataType: DataType,
    val factor: Double = 1.0,
    val offsetVal: Double = 0.0,
    val unit: String = ""
)

data class MapDefinition(
    val id: String,
    val name: String,
    val offset: Int,
    val rows: Int,
    val cols: Int,
    val dataType: DataType,
    val dimension: MapDimension = if (rows > 1 && cols > 1) MapDimension.THREE_D else MapDimension.TWO_D,
    val factor: Double = 1.0,
    val offsetVal: Double = 0.0,
    val unit: String = "",
    val xAxis: AxisDefinition? = null,
    val yAxis: AxisDefinition? = null
) {
    val totalElements: Int get() = rows * cols
    val byteSize: Int get() = totalElements * dataType.byteSize
}