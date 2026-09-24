package com.winols.app

import com.winols.app.data.DataWordSize
import java.nio.ByteOrder

/**
 * Model widoku i formatowania pojedynczej komórki binarnej do celów UI i Custom Views.
 */
data class BinCellModel(
    val address: Int,
    val rawValue: Long,
    val physicalValue: Double,
    val formattedPhysical: String
)

/**
 * Konfigurator przeliczania wartości surowych (RAW) na jednostki inżynieryjne.
 * Formuła: Physical = (RAW * factor) + offset
 */
data class UnitScaleConfig(
    val factor: Double = 1.0,
    val offset: Double = 0.0,
    val precision: Int = 2,
    val unit: String = ""
) {
    fun rawToPhysical(raw: Long): Double {
        return (raw * factor) + offset
    }

    fun physicalToRaw(phys: Double): Long {
        if (factor == 0.0) return 0L
        return Math.round((phys - offset) / factor)
    }

    fun format(raw: Long): String {
        val phys = rawToPhysical(raw)
        return "%.${precision}f %s".format(phys, unit).trim()
    }
}

/**
 * Opcje wizualizacji danych w podglądzie HEX oraz widoku Grid View.
 */
data class ViewDisplayOptions(
    val wordSize: DataWordSize = DataWordSize.WORD_16,
    val isSigned: Boolean = false,
    val byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN,
    val scaleConfig: UnitScaleConfig = UnitScaleConfig()
)