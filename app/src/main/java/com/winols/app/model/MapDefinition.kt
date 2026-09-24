package com.winols.app.model

import java.io.Serializable

enum class Endianness {
    LITTLE_ENDIAN,
    BIG_ENDIAN
}

enum class DataType(val byteSize: Int) {
    UINT8(1),
    INT8(1),
    UINT16(2),
    INT16(2),
    UINT32(4),
    INT32(4),
    FLOAT32(4)
}

enum class MapCategory {
    INJECTION,
    IGNITION_BOOST,
    TORQUE_LIMITER,
    RAIL_PRESSURE,
    LAMBDA_AFR,
    SENSOR_CALIBRATION,
    UNKNOWN
}

data class AxisDefinition(
    val name: String,
    val unit: String = "",
    val address: Int,
    val length: Int,
    val dataType: DataType = DataType.UINT16,
    val endianness: Endianness = Endianness.LITTLE_ENDIAN,
    val factor: Double = 1.0,
    val offset: Double = 0.0,
    val values: DoubleArray = DoubleArray(0)
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AxisDefinition
        return address == other.address && length == other.length && name == other.name
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + address
        result = 31 * result + length
        return result
    }
}

data class MapDefinition(
    val id: String,
    val name: String,
    val startAddress: Int,
    val rows: Int,
    val cols: Int,
    val dataType: DataType = DataType.UINT16,
    val endianness: Endianness = Endianness.LITTLE_ENDIAN,
    val factor: Double = 1.0,
    val offset: Double = 0.0,
    val unit: String = "",
    val category: MapCategory = MapCategory.UNKNOWN,
    val confidenceScore: Double = 0.0,
    val xAxis: AxisDefinition? = null,
    val yAxis: AxisDefinition? = null
) : Serializable {
    val totalBytes: Int
        get() = rows * cols * dataType.byteSize

    val is3D: Boolean
        get() = rows > 1 && cols > 1
}