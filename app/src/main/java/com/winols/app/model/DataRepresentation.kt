package com.winols.app.model

import java.nio.ByteOrder

enum class BitDepth(val bytesPerElement: Int) {
    BITS_8(1),
    BITS_16(2),
    BITS_32(4)
}

enum class ValueType {
    SIGNED,
    UNSIGNED,
    HEX
}

data class DataRepresentation(
    val bitDepth: BitDepth = BitDepth.BITS_16,
    val valueType: ValueType = ValueType.UNSIGNED,
    val byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN,
    val factor: Double = 1.0,
    val offset: Double = 0.0,
    val decimalPlaces: Int = 2
) {
    fun rawToPhysical(rawValue: Long): Double {
        return (rawValue * factor) + offset
    }

    fun physicalToRaw(physicalValue: Double): Long {
        if (factor == 0.0) return 0L
        return Math.round((physicalValue - offset) / factor)
    }

    fun formatPhysical(physicalValue: Double): String {
        return if (decimalPlaces <= 0) {
            Math.round(physicalValue).toString()
        } else {
            String.format("%.${decimalPlaces}f", physicalValue)
        }
    }
}