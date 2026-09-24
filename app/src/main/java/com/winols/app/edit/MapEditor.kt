package com.winols.app.edit

import com.winols.app.data.BinaryBufferManager
import com.winols.app.model.DataRepresentation
import com.winols.app.model.MapDefinition
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

class MapEditor(private val bufferManager: BinaryBufferManager) {

    enum class OperationType {
        ADD_OFFSET,
        PERCENT_MULTIPLY,
        SET_CONSTANT,
        SMOOTH
    }

    fun readMapValues(mapDef: MapDefinition): DoubleArray {
        val totalElements = mapDef.rows * mapDef.cols
        val byteStep = mapDef.dataRepresentation.byteSize
        val rawBytes = bufferManager.getBlock(mapDef.startAddress, totalElements * byteStep)
        val byteBuf = ByteBuffer.wrap(rawBytes).order(mapDef.dataRepresentation.byteOrder)
        val values = DoubleArray(totalElements)

        for (i in 0 until totalElements) {
            val raw = when (mapDef.dataRepresentation) {
                DataRepresentation.UBYTE -> (byteBuf.get().toInt() and 0xFF).toDouble()
                DataRepresentation.SBYTE -> byteBuf.get().toDouble()
                DataRepresentation.UWORD_LE, DataRepresentation.UWORD_BE -> (byteBuf.short.toInt() and 0xFFFF).toDouble()
                DataRepresentation.SWORD_LE, DataRepresentation.SWORD_BE -> byteBuf.short.toDouble()
                DataRepresentation.ULONG_LE, DataRepresentation.ULONG_BE -> (byteBuf.int.toLong() and 0xFFFFFFFFL).toDouble()
                DataRepresentation.SLONG_LE, DataRepresentation.SLONG_BE -> byteBuf.int.toDouble()
            }
            values[i] = (raw * mapDef.factor) + mapDef.offset
        }
        return values
    }

    fun applyOperation(
        mapDef: MapDefinition,
        opType: OperationType,
        operand: Double,
        selectionMask: BooleanArray? = null
    ) {
        val currentValues = readMapValues(mapDef)
        val totalElements = mapDef.rows * mapDef.cols
        val updatedValues = DoubleArray(totalElements)

        for (i in 0 until totalElements) {
            val isSelected = selectionMask?.getOrNull(i) ?: true
            if (!isSelected) {
                updatedValues[i] = currentValues[i]
                continue
            }

            updatedValues[i] = when (opType) {
                OperationType.ADD_OFFSET -> currentValues[i] + operand
                OperationType.PERCENT_MULTIPLY -> currentValues[i] * (1.0 + (operand / 100.0))
                OperationType.SET_CONSTANT -> operand
                OperationType.SMOOTH -> currentValues[i]
            }
        }

        if (opType == OperationType.SMOOTH) {
            apply3x3Smoothing(currentValues, updatedValues, mapDef.rows, mapDef.cols, operand.coerceIn(0.0, 1.0), selectionMask)
        }

        writeMapValues(mapDef, updatedValues)
    }

    private fun apply3x3Smoothing(
        src: DoubleArray,
        dst: DoubleArray,
        rows: Int,
        cols: Int,
        factor: Double,
        selectionMask: BooleanArray?
    ) {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val idx = r * cols + c
                val isSelected = selectionMask?.getOrNull(idx) ?: true
                if (!isSelected) continue

                var sum = 0.0
                var count = 0
                for (dr in -1..1) {
                    for (dc in -1..1) {
                        val nr = r + dr
                        val nc = c + dc
                        if (nr in 0 until rows && nc in 0 until cols) {
                            sum += src[nr * cols + nc]
                            count++
                        }
                    }
                }
                val avg = sum / count
                dst[idx] = src[idx] * (1.0 - factor) + (avg * factor)
            }
        }
    }

    private fun writeMapValues(mapDef: MapDefinition, values: DoubleArray) {
        val totalElements = mapDef.rows * mapDef.cols
        val byteStep = mapDef.dataRepresentation.byteSize
        val targetBytes = ByteArray(totalElements * byteStep)
        val byteBuf = ByteBuffer.wrap(targetBytes).order(mapDef.dataRepresentation.byteOrder)

        for (i in 0 until totalElements) {
            val unscaled = (values[i] - mapDef.offset) / mapDef.factor
            when (mapDef.dataRepresentation) {
                DataRepresentation.UBYTE -> {
                    val clamped = unscaled.roundToInt().coerceIn(0, 0xFF)
                    byteBuf.put(clamped.toByte())
                }
                DataRepresentation.SBYTE -> {
                    val clamped = unscaled.roundToInt().coerceIn(-128, 127)
                    byteBuf.put(clamped.toByte())
                }
                DataRepresentation.UWORD_LE, DataRepresentation.UWORD_BE -> {
                    val clamped = unscaled.roundToInt().coerceIn(0, 0xFFFF)
                    byteBuf.putShort(clamped.toShort())
                }
                DataRepresentation.SWORD_LE, DataRepresentation.SWORD_BE -> {
                    val clamped = unscaled.roundToInt().coerceIn(-32768, 32767)
                    byteBuf.putShort(clamped.toShort())
                }
                DataRepresentation.ULONG_LE, DataRepresentation.ULONG_BE -> {
                    val clamped = unscaled.toLong().coerceIn(0L, 0xFFFFFFFFL)
                    byteBuf.putInt(clamped.toInt())
                }
                DataRepresentation.SLONG_LE, DataRepresentation.SLONG_BE -> {
                    val clamped = unscaled.toLong().coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong())
                    byteBuf.putInt(clamped.toInt())
                }
            }
        }

        bufferManager.putBlock(mapDef.startAddress, targetBytes, trackUndo = true)
    }
}