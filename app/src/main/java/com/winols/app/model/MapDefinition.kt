package com.winols.app.model

enum class DataType(val byteSize: Int) {
    UBYTE(1),
    SBYTE(1),
    UWORD_LE(2),
    SWORD_LE(2),
    UWORD_BE(2),
    SWORD_BE(2),
    UDWORD_LE(4),
    SDWORD_LE(4),
    UDWORD_BE(4),
    SDWORD_BE(4)
}

data class AxisDefinition(
    val name: String,
    val unit: String = "",
    val address: Long = 0L,
    val length: Int = 1,
    val dataType: DataType = DataType.UWORD_LE,
    val factor: Double = 1.0,
    val offset: Double = 0.0,
    val manualValues: List<Double>? = null
)

data class MapDefinition(
    val id: String,
    val name: String,
    val startAddress: Long,
    val rows: Int,
    val columns: Int,
    val dataType: DataType = DataType.UWORD_LE,
    val factor: Double = 1.0,
    val offset: Double = 0.0,
    val unit: String = "",
    val xAxis: AxisDefinition? = null,
    val yAxis: AxisDefinition? = null,
    val category: String = "Engine"
) {
    val sizeInBytes: Int
        get() = rows * columns * dataType.byteSize
}