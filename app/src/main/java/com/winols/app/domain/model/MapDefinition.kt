package com.winols.app.domain.model

enum class Endianness {
    LITTLE_ENDIAN, // LoHi
    BIG_ENDIAN     // HiLo (np. Bosch EDC15/16/17, ME7)
}

enum class DataType(val byteSize: Int) {
    UINT8(1),
    INT8(1),
    UINT16(2),
    INT16(2),
    UINT32(4),
    INT32(4)
}

data class AxisDefinition(
    val id: String,
    val name: String,
    val address: Int,
    val length: Int,
    val dataType: DataType,
    val endianness: Endianness,
    val factor: Double = 1.0,
    val offset: Double = 0.0,
    val unit: String = "",
    val values: List<Double> = emptyList()
)

data class MapDefinition(
    val id: String,
    val name: String,
    val address: Int,
    val rows: Int,
    val columns: Int,
    val dataType: DataType,
    val endianness: Endianness,
    val factor: Double = 1.0,
    val offset: Double = 0.0,
    val unit: String = "",
    val xAxis: AxisDefinition? = null,
    val yAxis: AxisDefinition? = null,
    val confidenceScore: Float = 0.0f
) {
    val totalBytes: Int
        get() = rows * columns * dataType.byteSize
}