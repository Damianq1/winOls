package com.winols.app.data

import com.winols.app.model.DataType
import com.winols.app.model.Endianness

/**
 * Stan konfiguracji widoku danych w edytorze heksadecymalnym i tabelarycznym.
 */
data class ViewConfiguration(
    var dataType: DataType = DataType.UINT16,
    var endianness: Endianness = Endianness.LITTLE_ENDIAN,
    var displayMode: DataRepresentationFormatter.DisplayMode = DataRepresentationFormatter.DisplayMode.HEX,
    var columnsCount: Int = 16
) {
    fun toggleEndianness() {
        endianness = if (endianness == Endianness.LITTLE_ENDIAN) {
            Endianness.BIG_ENDIAN
        } else {
            Endianness.LITTLE_ENDIAN
        }
    }

    fun toggleSign() {
        dataType = when (dataType) {
            DataType.UINT8 -> DataType.INT8
            DataType.INT8 -> DataType.UINT8
            DataType.UINT16 -> DataType.INT16
            DataType.INT16 -> DataType.UINT16
            DataType.UINT32 -> DataType.INT32
            DataType.INT32 -> DataType.UINT32
            DataType.FLOAT32 -> DataType.FLOAT32
        }
    }

    fun setBitDepth(bits: Int) {
        dataType = when (bits) {
            8 -> if (isSigned()) DataType.INT8 else DataType.UINT8
            16 -> if (isSigned()) DataType.INT16 else DataType.UINT16
            32 -> if (isSigned()) DataType.INT32 else DataType.UINT32
            else -> dataType
        }
    }

    fun isSigned(): Boolean = dataType in listOf(DataType.INT8, DataType.INT16, DataType.INT32)
}