package com.winols.app.model

enum class DataType(val byteSize: Int) {
    UINT8(1),
    INT8(1),
    UINT16_LE(2),
    UINT16_BE(2),
    INT16_LE(2),
    INT16_BE(2),
    UINT32_LE(4),
    UINT32_BE(4)
}

enum class MapOrganization {
    ONE_D,
    TWO_D,
    THREE_D
}

data class AxisDefinition(
    val name: String,
    val unit: String = "",
    val startAddress: Long,
    val length: Int,
    val dataType: DataType = DataType.UINT16_LE,
    val factor: Double = 1.0,
    val offset: Double = 0.0,
    val isShared: Boolean = false
)

data class MapDefinition(
    val id: String,
    val name: String,
    val description: String = "",
    val organization: MapOrganization,
    val startAddress: Long,
    val rows: Int,
    val cols: Int,
    val cellDataType: DataType = DataType.UINT16_LE,
    val factor: Double = 1.0,
    val offset: Double = 0.0,
    val unit: String = "",
    val xAxis: AxisDefinition? = null,
    val yAxis: AxisDefinition? = null
) {
    val totalBytes: Int
        get() = rows * cols * cellDataType.byteSize
}