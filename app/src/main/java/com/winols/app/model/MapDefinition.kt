package com.winols.app.model

import com.winols.app.data.DataFormatterEngine

/**
 * Reprezentacja osi mapy (X, Y).
 */
data class AxisDefinition(
    var name: String = "",
    var unit: String = "",
    var address: Long = 0L,
    var length: Int = 0,
    var bitWidth: Int = 16,
    var isSigned: Boolean = false,
    var factor: Double = 1.0,
    var offset: Double = 0.0,
    var decimals: Int = 0
) {
    fun toPhysical(raw: Long): Double = DataFormatterEngine.rawToPhysical(raw, factor, offset)
    fun toRaw(physical: Double): Long = DataFormatterEngine.physicalToRaw(physical, factor, offset, bitWidth, isSigned)
}

/**
 * Pełna definicja mapy 1D, 2D lub 3D w pamięci Flash.
 */
data class MapDefinition(
    var id: String = "",
    var name: String = "Untitled Map",
    var address: Long = 0L,
    var columns: Int = 1,
    var rows: Int = 1,
    var bitWidth: Int = 16,
    var isSigned: Boolean = false,
    var isBigEndian: Boolean = false,
    var factor: Double = 1.0,
    var offset: Double = 0.0,
    var unit: String = "",
    var decimals: Int = 2,
    var xAxis: AxisDefinition = AxisDefinition("X", "", 0L, 0),
    var yAxis: AxisDefinition = AxisDefinition("Y", "", 0L, 0)
) {
    val totalElements: Int
        get() = columns * rows

    val byteSize: Int
        get() = totalElements * (bitWidth / 8)

    /**
     * Przelicza komórkę RAW na jednostkę fizyczną według wzoru: (RAW * factor) + offset
     */
    fun cellRawToPhysical(raw: Long): Double {
        return DataFormatterEngine.rawToPhysical(raw, factor, offset)
    }

    /**
     * Przelicza jednostkę fizyczną na RAW z dopasowaniem do szerokości bitowej komórki mapy.
     */
    fun cellPhysicalToRaw(physical: Double): Long {
        return DataFormatterEngine.physicalToRaw(physical, factor, offset, bitWidth, isSigned)
    }

    /**
     * Zwraca sformatowaną reprezentację tekstową komórki fizycznej wraz z jednostką.
     */
    fun formatCellValue(raw: Long, includeUnit: Boolean = false): String {
        val physical = cellRawToPhysical(raw)
        val formatted = DataFormatterEngine.formatPhysical(physical, decimals)
        return if (includeUnit && unit.isNotBlank()) "$formatted $unit" else formatted
    }
}