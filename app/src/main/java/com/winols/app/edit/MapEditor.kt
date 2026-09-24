package com.winols.app.edit

import com.winols.app.data.BinaryBufferManager
import com.winols.app.model.MapDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.ArrayDeque

class MapEditor(private val bufferManager: BinaryBufferManager) {

    private data class UndoStep(val startAddress: Int, val oldBytes: ByteArray)
    private val undoStack = ArrayDeque<UndoStep>()

    enum class OperationType {
        ADD_OFFSET,
        MULTIPLY_PERCENT,
        SET_CONSTANT,
        SMOOTH
    }

    suspend fun applyOperation(
        mapDef: MapDefinition,
        selectedCells: List<Pair<Int, Int>>? = null,
        type: OperationType,
        value: Double
    ) = withContext(Dispatchers.Default) {
        val bytesPerCell = mapDef.representation.bitDepth.bytesPerElement
        val totalSize = mapDef.sizeInBytes
        val originalData = bufferManager.readBlock(mapDef.startAddress, totalSize)
        undoStack.push(UndoStep(mapDef.startAddress, originalData))

        val matrix = Array(mapDef.rows) { DoubleArray(mapDef.columns) }
        val targetIndices = selectedCells ?: (0 until mapDef.rows).flatMap { r ->
            (0 until mapDef.columns).map { c -> Pair(r, c) }
        }

        for (r in 0 until mapDef.rows) {
            for (c in 0 until mapDef.columns) {
                val offset = (r * mapDef.columns + c) * bytesPerCell
                val raw = bufferManager.readRawValue(mapDef.startAddress + offset, mapDef.representation)
                matrix[r][c] = mapDef.representation.rawToPhysical(raw)
            }
        }

        when (type) {
            OperationType.ADD_OFFSET -> {
                for ((r, c) in targetIndices) {
                    matrix[r][c] += value
                }
            }
            OperationType.MULTIPLY_PERCENT -> {
                val factor = 1.0 + (value / 100.0)
                for ((r, c) in targetIndices) {
                    matrix[r][c] *= factor
                }
            }
            OperationType.SET_CONSTANT -> {
                for ((r, c) in targetIndices) {
                    matrix[r][c] = value
                }
            }
            OperationType.SMOOTH -> {
                val temp = Array(mapDef.rows) { r -> matrix[r].clone() }
                val targetSet = targetIndices.toSet()
                val weight = Math.max(0.0, Math.min(1.0, value))
                
                for ((r, c) in targetIndices) {
                    var sum = 0.0
                    var count = 0
                    for (dr in -1..1) {
                        for (dc in -1..1) {
                            val nr = r + dr
                            val nc = c + dc
                            if (nr in 0 until mapDef.rows && nc in 0 until mapDef.columns && targetSet.contains(Pair(nr, nc))) {
                                sum += temp[nr][nc]
                                count++
                            }
                        }
                    }
                    if (count > 0) {
                        val average = sum / count
                        matrix[r][c] = (temp[r][c] * (1.0 - weight)) + (average * weight)
                    }
                }
            }
        }

        for (r in 0 until mapDef.rows) {
            for (c in 0 until mapDef.columns) {
                val offset = (r * mapDef.columns + c) * bytesPerCell
                val newRaw = mapDef.representation.physicalToRaw(matrix[r][c])
                bufferManager.writeRawValue(mapDef.startAddress + offset, newRaw, mapDef.representation)
            }
        }
    }

    suspend fun undo(): Boolean = withContext(Dispatchers.Default) {
        if (undoStack.isEmpty()) return@withContext false
        val step = undoStack.pop()
        bufferManager.writeBlock(step.startAddress, step.oldBytes)
        true
    }

    val canUndo: Boolean
        get() = undoStack.isNotEmpty()
}