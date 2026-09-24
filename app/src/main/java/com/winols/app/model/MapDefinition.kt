package com.winols.app.model

import java.nio.ByteOrder

enum class DataType(val byteSize: Int) {
    UBYTE(1),
    SBYTE(1),
    UWORD(2),
    SWORD(2),
    UDWORD(4),
    SDWORD(4)
}

data class AxisDefinition(
    val address: Int,
    val length: Int,
    val dataType: DataType = DataType.UWORD,
    val byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN,
    val factor: Double = 1.0,
    val offset: Double = 0.0,
    val unit: String = "",
    val name: String = "Axis"
)

data class MapDefinition(
    val id: String,
    val name: String,
    val startAddress: Int,
    val rows: Int,
    val columns: Int,
    val dataType: DataType = DataType.UWORD,
    val byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN,
    val factor: Double = 1.0,
    val offset: Double = 0.0,
    val unit: String = "",
    val xAxis: AxisDefinition? = null,
    val yAxis: AxisDefinition? = null,
    val confidence: Float = 1.0f
) {
    val totalBytes: Int
        get() = rows * columns * dataType.byteSize

    fun toPhysical(rawValue: Double): Double = (rawValue * factor) + offset

    fun toRaw(physicalValue: Double): Double = if (factor != 0.0) (physicalValue - offset) / factor else 0.0
}