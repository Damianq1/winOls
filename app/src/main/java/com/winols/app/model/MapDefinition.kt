package com.winols.app.model

enum class DataType(val byteSize: Int) {
    UBYTE(1),
    SBYTE(1),
    UWORD_LE(2),
    UWORD_BE(2),
    SWORD_LE(2),
    SWORD_BE(2),
    UDWORD_LE(4),
    UDWORD_BE(4),
    SDWORD_LE(4),
    SDWORD_BE(4),
    FLOAT_LE(4),
    FLOAT_BE(4)
}

data class AxisDefinition(
    val id: String,
    val name: String,
    val unit: String = "",
    val address: Int,
    val length: Int,
    val dataType: DataType,
    val factor: Double = 1.0,
    val offset: Double = 0.0
)

data class MapDefinition(
    val id: String,
    val name: String,
    val description: String = "",
    val startAddress: Int,
    val rows: Int,
    val columns: Int,
    val dataType: DataType,
    val factor: Double = 1.0,
    val offset: Double = 0.0,
    val unit: String = "",
    val xAxis: AxisDefinition? = null,
    val yAxis: AxisDefinition? = null
) {
    val totalElements: Int
        get() = rows * columns

    val byteLength: Int
        get() = totalElements * dataType.byteSize
}