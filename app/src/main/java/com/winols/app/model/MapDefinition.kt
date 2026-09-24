package com.winols.app.model

import java.nio.ByteOrder

enum class DataType(val byteSize: Int) {
    UBYTE(1),
    SBYTE(1),
    USHORT(2),
    SSHORT(2),
    UINT(4),
    SINT(4),
    FLOAT(4)
}

enum class MapDimension {
    ONE_D,
    TWO_D,
    THREE_D
}

data class AxisDefinition(
    val name: String,
    val unit: String = "",
    val startOffset: Int = 0,
    val length: Int = 0,
    val dataType: DataType = DataType.USHORT,
    val factor: Double = 1.0,
    val offset: Double = 0.0,
    val isExternal: Boolean = false,
    val byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN
)

data class MapDefinition(
    val id: String,
    val name: String,
    val description: String = "",
    val startOffset: Int,
    val columns: Int,
    val rows: Int = 1,
    val dimension: MapDimension = if (rows > 1) MapDimension.THREE_D else MapDimension.TWO_D,
    val dataType: DataType = DataType.USHORT,
    val byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN,
    val factor: Double = 1.0,
    val offset: Double = 0.0,
    val unit: String = "",
    val xAxis: AxisDefinition? = null,
    val yAxis: AxisDefinition? = null
) {
    val totalBytes: Int
        get() = columns * rows * dataType.byteSize
}