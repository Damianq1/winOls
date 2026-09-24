package com.winols.app.data

import com.winols.app.model.CellDataType
import com.winols.app.model.MapDefinition
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Wysokowydajny silnik formatowania i dwukierunkowej konwersji wartości
 * RAW (bajtowych) oraz fizycznych (skalowanych) bezpośrednio na buforach ByteBuffer.
 */
object DataFormatterEngine {

    /**
     * Wyciąga surową liczbę całkowitą (uwzględniając znak i kolejność bajtów) z bufora pod danym adresem.
     */
    fun readRawValue(buffer: ByteBuffer, address: Int, dataType: CellDataType, order: ByteOrder): Long {
        buffer.order(order)
        return when (dataType) {
            CellDataType.UBYTE -> (buffer.get(address).toInt() and 0xFF).toLong()
            CellDataType.SBYTE -> buffer.get(address).toLong()
            CellDataType.UWORD -> (buffer.getShort(address).toInt() and 0xFFFF).toLong()
            CellDataType.SWORD -> buffer.getShort(address).toLong()
            CellDataType.UDWORD -> buffer.getInt(address).toLong() and 0xFFFFFFFFL
            CellDataType.SDWORD -> buffer.getInt(address).toLong()
        }
    }

    /**
     * Zapisuje wyliczoną wartość surową (RAW) bezpośrednio do bufora pod dany adres.
     */
    fun writeRawValue(buffer: ByteBuffer, address: Int, rawValue: Long, dataType: CellDataType, order: ByteOrder) {
        buffer.order(order)
        when (dataType) {
            CellDataType.UBYTE -> buffer.put(address, (rawValue and 0xFF).toByte())
            CellDataType.SBYTE -> buffer.put(address, rawValue.toByte())
            CellDataType.UWORD -> buffer.putShort(address, (rawValue and 0xFFFF).toShort())
            CellDataType.SWORD -> buffer.putShort(address, rawValue.toShort())
            CellDataType.UDWORD -> buffer.putInt(address, (rawValue and 0xFFFFFFFFL).toInt())
            CellDataType.SDWORD -> buffer.putInt(address, rawValue.toInt())
        }
    }

    /**
     * Wczytuje całą mapę do dwuwymiarowej tablicy wartości fizycznych:
     * Physical = (RAW * Factor) + Offset
     */
    fun extractPhysicalMatrix(buffer: ByteBuffer, mapDef: MapDefinition): Array<DoubleArray> {
        val matrix = Array(mapDef.rows) { DoubleArray(mapDef.columns) }
        var currentAddress = mapDef.startAddress
        val step = mapDef.dataType.byteSize

        for (r in 0 until mapDef.rows) {
            for (c in 0 until mapDef.columns) {
                val raw = readRawValue(buffer, currentAddress, mapDef.dataType, mapDef.byteOrder)
                matrix[r][c] = mapDef.rawToPhysical(raw)
                currentAddress += step
            }
        }
        return matrix
    }

    /**
     * Zapisuje zmodyfikowaną macierz wartości fizycznych z powrotem do bufora binarnego jako RAW.
     */
    fun applyPhysicalMatrix(buffer: ByteBuffer, mapDef: MapDefinition, matrix: Array<DoubleArray>) {
        var currentAddress = mapDef.startAddress
        val step = mapDef.dataType.byteSize

        for (r in 0 until mapDef.rows) {
            for (c in 0 until mapDef.columns) {
                val physicalVal = matrix[r][c]
                val rawVal = mapDef.physicalToRaw(physicalVal)
                writeRawValue(buffer, currentAddress, rawVal, mapDef.dataType, mapDef.byteOrder)
                currentAddress += step
            }
        }
    }

    /**
     * Predefiniowane profile przeliczników często spotykanych w sterownikach Bosch (EDC15/EDC16/EDC17/MED9).
     */
    object WellKnownConversions {
        fun boostPressureMbar(def: MapDefinition) = def.copy(factor = 1.0, offset = 0.0, unit = "mbar", decimals = 0)
        fun boostPressureBar(def: MapDefinition) = def.copy(factor = 0.01, offset = 0.0, unit = "bar", decimals = 2)
        fun temperatureEcuStandard(def: MapDefinition) = def.copy(factor = 0.25, offset = -40.0, unit = "°C", decimals = 1)
        fun injectionQuantityMg(def: MapDefinition) = def.copy(factor = 0.01, offset = 0.0, unit = "mg/hub", decimals = 2)
        fun engineSpeedRpm(def: MapDefinition) = def.copy(factor = 0.5, offset = 0.0, unit = "RPM", decimals = 0)
        fun throttlePercentage(def: MapDefinition) = def.copy(factor = 0.0030517578, offset = 0.0, unit = "%", decimals = 1) // 100/32768
    }
}