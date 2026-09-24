package com.winols.app.model

enum class DataType(val byteSize: Int) {
    UBYTE(1),
    SBYTE(1),
    UWORD_LE(2),
    SWORD_LE(2),
    UWORD_BE(2),
    SWORD_BE(2),
    ULONG_LE(4),
    SLONG_LE(4),
    ULONG_BE(4),
    SLONG_BE(4)
}

data class MapAxis(
    val name: String,
    val address: Int,
    val size: Int,
    val dataType: DataType = DataType.UWORD_LE,
    val factor: Double = 1.0,
    val offset: Double = 0.0,
    val unit: String = ""
)

data class MapDefinition(
    val id: String,
    val name: String,
    val address: Int,
    val rows: Int,
    val cols: Int,
    val dataType: DataType = DataType.UWORD_LE,
    val factor: Double = 1.0,
    val offset: Double = 0.0,
    val unit: String = "",
    val xAxis: MapAxis? = null,
    val yAxis: MapAxis? = null
) {
    val totalBytes: Int
        get() = rows * cols * dataType.byteSize
}