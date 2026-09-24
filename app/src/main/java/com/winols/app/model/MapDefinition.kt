package com.winols.app.model

import java.nio.ByteOrder

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
    FLOAT_BE(4);

    val byteOrder: ByteOrder
        get() = when (this) {
            UWORD_BE, SWORD_BE, UDWORD_BE, SDWORD_BE, FLOAT_BE -> ByteOrder.BIG_ENDIAN
            else -> ByteOrder.LITTLE_ENDIAN
        }

    val isSigned: Boolean
        get() = when (this) {
            SBYTE, SWORD_LE, SWORD_BE, SDWORD_LE, SDWORD_BE -> true
            else -> false
        }
}

data class AxisDefinition @JvmOverloads constructor(
    val id: String,
    val name: String,
    val unit: String = "",
    val address: Int,
    val length: Int,
    val dataType: DataType = DataType.UWORD_LE,
    val factor: Double = 1.0,
    val offset: Double = 0.0,
    val precision: Int = 2,
    val isHeaderPresent: Boolean = false
) {
    val totalBytes: Int get() = length * dataType.byteSize

    fun rawToPhysical(raw: Double): Double = (raw * factor) + offset
    fun physicalToRaw(physical: Double): Double = if (factor != 0.0) (physical - offset) / factor else 0.0
}

data class MapDefinition @JvmOverloads constructor(
    val id: String,
    val name: String,
    val category: String = "Engine",
    val description: String = "",
    val dataAddress: Int,
    val rows: Int,
    val cols: Int,
    val dataType: DataType = DataType.UWORD_LE,
    val factor: Double = 1.0,
    val offset: Double = 0.0,
    val precision: Int = 2,
    val unit: String = "",
    val xAxis: AxisDefinition? = null,
    val yAxis: AxisDefinition? = null
) {
    val totalElements: Int get() = rows * cols
    val totalBytes: Int get() = totalElements * dataType.byteSize
    val endAddress: Int get() = dataAddress + totalBytes

    fun rawToPhysical(raw: Double): Double = (raw * factor) + offset
    fun physicalToRaw(physical: Double): Double = if (factor != 0.0) (physical - offset) / factor else 0.0

    fun containsAddress(address: Int): Boolean = address in dataAddress until endAddress
}