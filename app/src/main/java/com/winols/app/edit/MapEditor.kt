package com.winols.app.edit

import com.winols.app.data.BinaryBufferManager
import com.winols.app.model.DataType
import com.winols.app.model.MapDefinition
import kotlin.math.roundToLong

class MapEditor(
    private val bufferManager: BinaryBufferManager,
    val mapDefinition: MapDefinition
) {

    /**
     * Zwraca całą macierz w przeliczeniu na jednostki fizyczne.
     */
    fun readPhysicalMatrix(): Array<DoubleArray> {
        val result = Array(mapDefinition.rows) { DoubleArray(mapDefinition.columns) }
        val step = mapDefinition.dataType.byteSize

        for (row in 0 until mapDefinition.rows) {
            for (col in 0 until mapDefinition.columns) {
                val elementIndex = row * mapDefinition.columns + col
                val address = mapDefinition.startAddress + elementIndex * step
                val rawVal = readElement(address, mapDefinition.dataType)
                result[row][col] = rawToPhysical(rawVal)
            }
        }
        return result
    }

    /**
     * Ustawia stałą wartość fizyczną dla pojedynczej komórki.
     */
    fun setConstantValue(row: Int, col: Int, constantPhysicalValue: Double) {
        validateCoordinates(row, col)
        bufferManager.pushSnapshot()
        writePhysicalInternal(row, col, constantPhysicalValue)
    }

    /**
     * Masowe ustawianie stałej wartości fizycznej dla zaznaczonego prostokątnego obszaru.
     */
    fun setConstantValueToRegion(
        startRow: Int,
        endRow: Int,
        startCol: Int,
        endCol: Int,
        constantPhysicalValue: Double
    ) {
        require(startRow in 0 until mapDefinition.rows && endRow in 0 until mapDefinition.rows) { "Zakres wierszy poza mapą" }
        require(startCol in 0 until mapDefinition.columns && endCol in 0 until mapDefinition.columns) { "Zakres kolumn poza mapą" }
        require(startRow <= endRow && startCol <= endCol) { "Nieprawidłowe indeksy początkowe i końcowe" }

        bufferManager.pushSnapshot()
        for (r in startRow..endRow) {
            for (c in startCol..endCol) {
                writePhysicalInternal(r, c, constantPhysicalValue)
            }
        }
    }

    /**
     * Masowe ustawianie stałej wartości fizycznej dla dowolnej listy wybranych komórek.
     */
    fun setConstantValueToSelection(cells: List<Pair<Int, Int>>, constantPhysicalValue: Double) {
        if (cells.isEmpty()) return
        bufferManager.pushSnapshot()
        for ((r, c) in cells) {
            validateCoordinates(r, c)
            writePhysicalInternal(r, c, constantPhysicalValue)
        }
    }

    /**
     * Ustawia całą mapę na stałą wartość fizyczną (np. zerowanie map EGR, limiterów).
     */
    fun fillEntireMapWithConstant(constantPhysicalValue: Double) {
        setConstantValueToRegion(
            startRow = 0,
            endRow = mapDefinition.rows - 1,
            startCol = 0,
            endCol = mapDefinition.columns - 1,
            constantPhysicalValue = constantPhysicalValue
        )
    }

    /**
     * Masowe ustawianie stałej wartości bezpośrednio w formacie RAW (z pominięciem przeliczników factor/offset).
     */
    fun setConstantRawToRegion(
        startRow: Int,
        endRow: Int,
        startCol: Int,
        endCol: Int,
        constantRawValue: Long
    ) {
        require(startRow <= endRow && startCol <= endCol) { "Nieprawidłowy zakres" }
        bufferManager.pushSnapshot()
        val clampedRaw = clampRawValue(constantRawValue, mapDefinition.dataType)

        for (r in startRow..endRow) {
            for (c in startCol..endCol) {
                validateCoordinates(r, c)
                val address = mapDefinition.startAddress + (r * mapDefinition.columns + c) * mapDefinition.dataType.byteSize
                writeElement(address, clampedRaw, mapDefinition.dataType)
            }
        }
    }

    private fun writePhysicalInternal(row: Int, col: Int, physicalValue: Double) {
        val raw = physicalToRaw(physicalValue)
        val clampedRaw = clampRawValue(raw, mapDefinition.dataType)
        val address = mapDefinition.startAddress + (row * mapDefinition.columns + col) * mapDefinition.dataType.byteSize
        writeElement(address, clampedRaw, mapDefinition.dataType)
    }

    private fun readElement(address: Int, type: DataType): Long {
        return when (type) {
            DataType.UINT8 -> bufferManager.readRawValue(address, 1)
            DataType.INT8 -> bufferManager.readSignedValue(address, 1)
            DataType.UINT16_LE -> bufferManager.readRawValue(address, 2, false)
            DataType.UINT16_BE -> bufferManager.readRawValue(address, 2, true)
            DataType.INT16_LE -> bufferManager.readSignedValue(address, 2, false)
            DataType.INT16_BE -> bufferManager.readSignedValue(address, 2, true)
            DataType.UINT32_LE -> bufferManager.readRawValue(address, 4, false)
            DataType.UINT32_BE -> bufferManager.readRawValue(address, 4, true)
            DataType.INT32_LE -> bufferManager.readSignedValue(address, 4, false)
            DataType.INT32_BE -> bufferManager.readSignedValue(address, 4, true)
        }
    }

    private fun writeElement(address: Int, value: Long, type: DataType) {
        when (type) {
            DataType.UINT8, DataType.INT8 -> bufferManager.writeRawValue(address, value, 1)
            DataType.UINT16_LE, DataType.INT16_LE -> bufferManager.writeRawValue(address, value, 2, false)
            DataType.UINT16_BE, DataType.INT16_BE -> bufferManager.writeRawValue(address, value, 2, true)
            DataType.UINT32_LE, DataType.INT32_LE -> bufferManager.writeRawValue(address, value, 4, false)
            DataType.UINT32_BE, DataType.INT32_BE -> bufferManager.writeRawValue(address, value, 4, true)
        }
    }

    private fun rawToPhysical(raw: Long): Double = (raw * mapDefinition.factor) + mapDefinition.additionOffset

    private fun physicalToRaw(physical: Double): Long =
        ((physical - mapDefinition.additionOffset) / mapDefinition.factor).roundToLong()

    private fun clampRawValue(value: Long, type: DataType): Long {
        return when (type) {
            DataType.UINT8 -> value.coerceIn(0L, 255L)
            DataType.INT8 -> value.coerceIn(-128L, 127L)
            DataType.UINT16_LE, DataType.UINT16_BE -> value.coerceIn(0L, 65535L)
            DataType.INT16_LE, DataType.INT16_BE -> value.coerceIn(-32768L, 32767L)
            DataType.UINT32_LE, DataType.UINT32_BE -> value.coerceIn(0L, 4294967295L)
            DataType.INT32_LE, DataType.INT32_BE -> value.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong())
        }
    }

    private fun validateCoordinates(row: Int, col: Int) {
        require(row in 0 until mapDefinition.rows) { "Nieprawidłowy wiersz: $row" }
        require(col in 0 until mapDefinition.columns) { "Nieprawidłowa kolumna: $col" }
    }
}