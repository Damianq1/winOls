package com.winols.app.edit

import com.winols.app.data.BinaryBufferManager
import com.winols.app.model.MapDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteOrder

/**
 * Obsługa kalkulacji, przeliczania RAW <-> Fizyczne oraz masowej edycji wartości mapy.
 */
class MapEditor(private val bufferManager: BinaryBufferManager) {

    data class EditDelta(
        val offset: Int,
        val previousRaw: ByteArray,
        val newRaw: ByteArray
    )

    enum class OperationType {
        ADD_OFFSET,
        PERCENTAGE,
        SET_EXACT,
        SMOOTH
    }

    /**
     * Pobiera macierz wartości fizycznych (X, Y) dla danej mapy.
     */
    suspend fun extractPhysicalValues(mapDef: MapDefinition): Array<DoubleArray> = withContext(Dispatchers.Default) {
        val rows = mapDef.rows
        val cols = mapDef.cols
        val result = Array(rows) { DoubleArray(cols) }
        val cellSizeBytes = mapDef.cellSizeBytes

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val cellOffset = mapDef.dataOffset + (r * cols + c) * cellSizeBytes
                val raw = readCellValue(cellOffset, mapDef)
                result[r][c] = rawToPhysical(raw, mapDef.factor, mapDef.offset)
            }
        }
        result
    }

    /**
     * Aplikuje masową modyfikację do wybranego obszaru (lub całej mapy) i zwraca delty do obsługi Undo.
     */
    suspend fun applyOperation(
        mapDef: MapDefinition,
        op: OperationType,
        operand: Double,
        selectionMask: Array<BooleanArray>? = null
    ): List<EditDelta> = withContext(Dispatchers.Default) {
        val deltas = mutableListOf<EditDelta>()
        val rows = mapDef.rows
        val cols = mapDef.cols
        val cellSizeBytes = mapDef.cellSizeBytes

        val currentValues = Array(rows) { r ->
            DoubleArray(cols) { c ->
                val cellOffset = mapDef.dataOffset + (r * cols + c) * cellSizeBytes
                val raw = readCellValue(cellOffset, mapDef)
                rawToPhysical(raw, mapDef.factor, mapDef.offset)
            }
        }

        val calculatedValues = Array(rows) { r -> currentValues[r].copyOf() }

        when (op) {
            OperationType.ADD_OFFSET -> {
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        if (selectionMask == null || selectionMask[r][c]) {
                            calculatedValues[r][c] += operand
                        }
                    }
                }
            }
            OperationType.PERCENTAGE -> {
                val factor = 1.0 + (operand / 100.0)
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        if (selectionMask == null || selectionMask[r][c]) {
                            calculatedValues[r][c] *= factor
                        }
                    }
                }
            }
            OperationType.SET_EXACT -> {
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        if (selectionMask == null || selectionMask[r][c]) {
                            calculatedValues[r][c] = operand
                        }
                    }
                }
            }
            OperationType.SMOOTH -> {
                // Prosty filtr Gaussa / sąsiedztwa 3x3 dla wygładzania
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        if (selectionMask == null || selectionMask[r][c]) {
                            var sum = 0.0
                            var count = 0
                            for (dr in -1..1) {
                                for (dc in -1..1) {
                                    val nr = r + dr
                                    val nc = c + dc
                                    if (nr in 0 until rows && nc in 0 until cols) {
                                        sum += currentValues[nr][nc]
                                        count++
                                    }
                                }
                            }
                            calculatedValues[r][c] = sum / count
                        }
                    }
                }
            }
        }

        // Zapis do bufora binarnego i generowanie delt
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (selectionMask == null || selectionMask[r][c]) {
                    val cellOffset = mapDef.dataOffset + (r * cols + c) * cellSizeBytes
                    val oldBytes = bufferManager.readBytes(cellOffset, cellSizeBytes)
                    val newRaw = physicalToRaw(calculatedValues[r][c], mapDef.factor, mapDef.offset, mapDef.isSigned, cellSizeBytes)

                    writeCellValue(cellOffset, newRaw, mapDef)
                    val newBytes = bufferManager.readBytes(cellOffset, cellSizeBytes)

                    deltas.add(EditDelta(cellOffset, oldBytes, newBytes))
                }
            }
        }

        deltas
    }

    private fun readCellValue(offset: Int, mapDef: MapDefinition): Double {
        val order = if (mapDef.isBigEndian) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN
        return when (mapDef.cellSizeBytes) {
            1 -> if (mapDef.isSigned) bufferManager.getByte(offset).toDouble() else bufferManager.getUByte(offset).toDouble()
            2 -> if (mapDef.isSigned) bufferManager.getShort(offset, order).toDouble() else bufferManager.getUShort(offset, order).toDouble()
            4 -> if (mapDef.isSigned) bufferManager.getInt(offset, order).toDouble() else bufferManager.getUInt(offset, order).toDouble()
            else -> throw IllegalArgumentException("Nieobsługiwany rozmiar komórki: ${mapDef.cellSizeBytes}")
        }
    }

    private fun writeCellValue(offset: Int, rawValue: Long, mapDef: MapDefinition) {
        val order = if (mapDef.isBigEndian) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN
        when (mapDef.cellSizeBytes) {
            1 -> bufferManager.putByte(offset, rawValue.toByte())
            2 -> bufferManager.putShort(offset, rawValue.toShort(), order)
            4 -> bufferManager.putInt(offset, rawValue.toInt(), order)
        }
    }

    private fun rawToPhysical(raw: Double, factor: Double, offset: Double): Double {
        return (raw * factor) + offset
    }

    private fun physicalToRaw(physical: Double, factor: Double, offset: Double, isSigned: Boolean, bytes: Int): Long {
        val unscaled = if (factor != 0.0) (physical - offset) / factor else 0.0
        val clamped = unscaled.coerceIn(getMinValue(isSigned, bytes), getMaxValue(isSigned, bytes))
        return Math.round(clamped)
    }

    private fun getMinValue(signed: Boolean, bytes: Int): Double = when {
        !signed -> 0.0
        bytes == 1 -> Byte.MIN_VALUE.toDouble()
        bytes == 2 -> Short.MIN_VALUE.toDouble()
        else -> Int.MIN_VALUE.toDouble()
    }

    private fun getMaxValue(signed: Boolean, bytes: Int): Double = when {
        !signed && bytes == 1 -> 255.0
        !signed && bytes == 2 -> 65535.0
        !signed && bytes == 4 -> 4294967295.0
        bytes == 1 -> Byte.MAX_VALUE.toDouble()
        bytes == 2 -> Short.MAX_VALUE.toDouble()
        else -> Int.MAX_VALUE.toDouble()
    }
}