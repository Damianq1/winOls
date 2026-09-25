package com.winols.app.domain.model

import java.util.Locale

/**
 * Typ danych wartości w pamięci ECU.
 */
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

/**
 * Definicja mapy ECU (tablicy kalibracyjnej).
 */
data class EcuMap(
    val id: String,
    val name: String,
    val startAddress: Long,
    val rows: Int,
    val columns: Int,
    val dataType: DataType,
    val factor: Double = 1.0,
    val offset: Double = 0.0,
    val unit: String = "",
    val description: String = ""
) {
    val totalBytes: Int
        get() = rows * columns * dataType.byteSize
}

/**
 * Model reprezentujący zmienioną zmienną / komórkę mapy.
 */
data class ModifiedVariable(
    val mapId: String?,
    val mapName: String?,
    val address: Long,
    val originalRawValue: Long,
    val modifiedRawValue: Long,
    val originalPhysicalValue: Double,
    val modifiedPhysicalValue: Double,
    val unit: String = ""
) {
    val deltaRaw: Long
        get() = modifiedRawValue - originalRawValue

    val deltaPhysical: Double
        get() = modifiedPhysicalValue - originalPhysicalValue

    val percentChange: Double
        get() = if (originalPhysicalValue != 0.0) {
            ((modifiedPhysicalValue - originalPhysicalValue) / originalPhysicalValue) * 100.0
        } else {
            0.0
        }

    fun formattedAddress(): String = String.format(Locale.US, "0x%06X", address)
}