package com.winols.app.edit

import com.winols.app.data.BinaryBufferManager
import com.winols.app.model.DataRepresentation
import com.winols.app.model.MapDefinition
import java.util.ArrayDeque
import kotlin.math.roundToInt

/**
 * Silnik edycji danych mapy z obsługą operacji masowych oraz historii Undo/Redo.
 */
class MapEditor(
    private val bufferManager: BinaryBufferManager
) {
    data class CellChange(
        val row: Int,
        val col: Int,
        val oldRawValue: Long,
        val newRawValue: Long
    )

    data class EditAction(
        val description: String,
        val map: MapDefinition,
        val changes: List<CellChange>
    )

    private val undoStack = ArrayDeque<EditAction>()
    private val redoStack = ArrayDeque<EditAction>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    enum class OperationType {
        ADD_OFFSET,       // Dodaj / Odejmij wartość fizyczną (+/-)
        PERCENTAGE,       // Zmiana procentowa (+/- X%)
        SET_CONSTANT,     // Ustaw stałą wartość fizyczną
        SMOOTHING         // Wygładzanie średnią sąsiadów (3x3 kernel)
    }

    /**
     * Wykonuje operację masową na zaznaczonych komórkach mapy.
     * @param selectedCells zbiór par (row, col)
     * @param param wartość parametru (np. +100.0, -5.0 dla %, 2500.0 dla stałej)
     */
    fun applyBulkOperation(
        map: MapDefinition,
        selectedCells: Set<Pair<Int, Int>>,
        operation: OperationType,
        param: Double = 0.0
    ): EditAction? {
        if (selectedCells.isEmpty()) return null

        val changes = mutableListOf<CellChange>()
        val representation = map.representation

        // Jeśli wygładzanie, potrzebujemy snapshotu aktualnych wartości fizycznych dla sąsiedztwa
        val currentPhysicalGrid = Array(map.rows) { r ->
            DoubleArray(map.cols) { c ->
                val raw = readRaw(map, r, c)
                rawToPhysical(raw, representation)
            }
        }

        for (r in 0 until map.rows) {
            for (c in 0 until map.cols) {
                if (!selectedCells.contains(Pair(r, c))) continue

                val oldRaw = readRaw(map, r, c)
                val oldPhys = currentPhysicalGrid[r][c]

                val newPhys = when (operation) {
                    OperationType.ADD_OFFSET -> oldPhys + param
                    OperationType.PERCENTAGE -> oldPhys * (1.0 + (param / 100.0))
                    OperationType.SET_CONSTANT -> param
                    OperationType.SMOOTHING -> calculateSmoothedValue(currentPhysicalGrid, r, c, map.rows, map.cols)
                }

                val newRaw = physicalToRaw(newPhys, representation)
                if (newRaw != oldRaw) {
                    changes.add(CellChange(r, c, oldRaw, newRaw))
                    writeRaw(map, r, c, newRaw)
                }
            }
        }

        if (changes.isNotEmpty()) {
            val action = EditAction(
                description = "${operation.name} (${changes.size} cells)",
                map = map,
                changes = changes
            )
            undoStack.push(action)
            redoStack.clear()
            return action
        }
        return null
    }

    fun undo(): EditAction? {
        if (undoStack.isEmpty()) return null
        val action = undoStack.pop()
        for (ch in action.changes) {
            writeRaw(action.map, ch.row, ch.col, ch.oldRawValue)
        }
        redoStack.push(action)
        return action
    }

    fun redo(): EditAction? {
        if (redoStack.isEmpty()) return null
        val action = redoStack.pop()
        for (ch in action.changes) {
            writeRaw(action.map, ch.row, ch.col, ch.newRawValue)
        }
        undoStack.push(action)
        return action
    }

    private fun calculateSmoothedValue(
        grid: Array<DoubleArray>,
        r: Int,
        c: Int,
        maxRows: Int,
        maxCols: Int
    ): Double {
        var sum = 0.0
        var count = 0
        for (dr in -1..1) {
            for (dc in -1..1) {
                val nr = r + dr
                val nc = c + dc
                if (nr in 0 until maxRows && nc in 0 until maxCols) {
                    sum += grid[nr][nc]
                    count++
                }
            }
        }
        return if (count > 0) sum / count else grid[r][c]
    }

    fun readRaw(map: MapDefinition, row: Int, col: Int): Long {
        val cellByteOffset = map.startAddress + (row * map.cols + col) * map.representation.byteSize
        return bufferManager.read(cellByteOffset, map.representation)
    }

    fun writeRaw(map: MapDefinition, row: Int, col: Int, rawValue: Long) {
        val cellByteOffset = map.startAddress + (row * map.cols + col) * map.representation.byteSize
        bufferManager.write(cellByteOffset, rawValue, map.representation)
    }

    fun rawToPhysical(raw: Long, rep: DataRepresentation): Double {
        return (raw * rep.factor) + rep.offset
    }

    fun physicalToRaw(physical: Double, rep: DataRepresentation): Long {
        val unscaled = (physical - rep.offset) / rep.factor
        val clamped = unscaled.roundToInt().toLong()
        return rep.clampRaw(clamped)
    }
}