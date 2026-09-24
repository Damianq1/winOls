package com.winols.app.model

import java.nio.ByteOrder

/**
 * Format kodowania binarnego w pamięci Flash/EEPROM.
 */
enum class DataType(val byteSize: Int, val isSigned: Boolean) {
    UINT8(1, false),
    INT8(1, true),
    UINT16(2, false),
    INT16(2, true),
    UINT32(4, false),
    INT32(4, true);

    val minValueRaw: Long
        get() = when (this) {
            UINT8 -> 0L
            INT8 -> Byte.MIN_VALUE.toLong()
            UINT16 -> 0L
            INT16 -> Short.MIN_VALUE.toLong()
            UINT32 -> 0L
            INT32 -> Int.MIN_VALUE.toLong()
        }

    val maxValueRaw: Long
        get() = when (this) {
            UINT8 -> 0xFFL
            INT8 -> Byte.MAX_VALUE.toLong()
            UINT16 -> 0xFFFFL
            INT16 -> Short.MAX_VALUE.toLong()
            UINT32 -> 0xFFFFFFFFL
            INT32 -> Int.MAX_VALUE.toLong()
        }
}

/**
 * Konfiguracja konwersji RAW <-> Physical dla danej mapy lub osi.
 */
data class ValueConversion(
    val factor: Double = 1.0,
    val offset: Double = 0.0,
    val decimals: Int = 2,
    val unit: String = ""
) {
    /**
     * Konwertuje RAW na wartość fizyczną.
     */
    fun rawToPhysical(raw: Number): Double {
        return (raw.toDouble() * factor) + offset
    }

    /**
     * Konwertuje wartość fizyczną na zaokrąglony RAW z ograniczeniem do limitów danego DataType.
     */
    fun physicalToRaw(physical: Double, dataType: DataType): Long {
        if (factor == 0.0) return 0L
        val calculated = kotlin.math.round((physical - offset) / factor).toLong()
        return calculated.coerceIn(dataType.minValueRaw, dataType.maxValueRaw)
    }

    /**
     * Zwraca sformatowany ciąg znaków, np. "1.25 bar" lub "-40.0 °C".
     */
    fun formatPhysical(physical: Double): String {
        val formatted = "%.${decimals}f".format(java.util.Locale.US, physical)
        return if (unit.isNotBlank()) "$formatted $unit" else formatted
    }
}