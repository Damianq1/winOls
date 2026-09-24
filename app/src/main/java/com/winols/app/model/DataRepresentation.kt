package com.winols.app.model

import java.nio.ByteOrder

/**
 * Konfiguracja formatu danych binarnej reprezentacji (WinOLS style).
 */
data class DataFormatConfig(
    val bitWidth: BitWidth = BitWidth.BIT_16,
    val signedness: Signedness = Signedness.UNSIGNED,
    val byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN, // BIG_ENDIAN = HiLo, LITTLE_ENDIAN = LoHi
    val radix: DisplayRadix = DisplayRadix.HEX,
    val factor: Double = 1.0,
    val offset: Double = 0.0
)

enum class BitWidth(val bytes: Int) {
    BIT_8(1),
    BIT_16(2),
    BIT_32(4)
}

enum class Signedness {
    SIGNED,
    UNSIGNED
}

enum class DisplayRadix {
    HEX,
    DECIMAL,
    BINARY
}

enum class ViewMode {
    HEX_VIEW,
    GRAPH_2D,
    TABLE_3D
}

/**
 * Punkt na wykresie 2D (przebieg/fala binarna).
 */
data class Point2D(
    val offset: Int,
    val rawValue: Long,
    val physicalValue: Double
)

/**
 * Matryca danych dla widoku mapy 3D.
 */
data class MapData3D(
    val rows: Int,
    val cols: Int,
    val rowHeaders: List<Double>,
    val colHeaders: List<Double>,
    val data: Array<DoubleArray>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MapData3D
        if (rows != other.rows || cols != other.cols) return false
        if (rowHeaders != other.rowHeaders || colHeaders != other.colHeaders) return false
        return data.contentDeepEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = rows
        result = 31 * result + cols
        result = 31 * result + rowHeaders.hashCode()
        result = 31 * result + colHeaders.hashCode()
        result = 31 * result + data.contentDeepHashCode()
        return result
    }
}