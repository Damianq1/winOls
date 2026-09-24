package com.winols.app.edit

import com.winols.app.data.BinaryBufferManager
import com.winols.app.model.MapDefinition

class MapEditor(
    private val bufferManager: BinaryBufferManager,
    private val batchEngine: MapBatchEngine = MapBatchEngine()
) {
    private val undoStack = ArrayDeque<ByteArray>()
    private val redoStack = ArrayDeque<ByteArray>()

    fun loadMapGrid(mapDef: MapDefinition): Array<DoubleArray> {
        val rows = mapDef.yAxisDimension
        val cols = mapDef.xAxisDimension
        val grid = Array(rows) { DoubleArray(cols) }

        var offset = mapDef.startAddress
        val elementSize = mapDef.elementBitSize / 8

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val rawValue = bufferManager.readNumeric(offset, elementSize, mapDef.isSigned, mapDef.isLittleEndian)
                grid[r][c] = (rawValue * mapDef.scaleFactor) + mapDef.offset
                offset += elementSize
            }
        }
        return grid
    }

    fun applyBatchEdit(
        mapDef: MapDefinition,
        currentGrid: Array<DoubleArray>,
        selection: CellSelection,
        operation: MapBatchOperation
    ): Array<DoubleArray> {
        saveSnapshot()

        val updatedGrid = batchEngine.applyOperation(
            grid = currentGrid,
            selection = selection,
            operation = operation,
            clampMin = mapDef.minValue ?: -Double.MAX_VALUE,
            clampMax = mapDef.maxValue ?: Double.MAX_VALUE
        )

        commitToBinary(mapDef, updatedGrid)
        return updatedGrid
    }

    private fun commitToBinary(mapDef: MapDefinition, grid: Array<DoubleArray>) {
        val rows = mapDef.yAxisDimension
        val cols = mapDef.xAxisDimension
        val elementSize = mapDef.elementBitSize / 8
        var offset = mapDef.startAddress

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val physicalValue = grid[r][c]
                val rawValue = ((physicalValue - mapDef.offset) / mapDef.scaleFactor).toLong()
                bufferManager.writeNumeric(offset, rawValue, elementSize, mapDef.isLittleEndian)
                offset += elementSize
            }
        }
    }

    private fun saveSnapshot() {
        undoStack.addLast(bufferManager.getSnapshot())
        redoStack.clear()
        if (undoStack.size > 50) {
            undoStack.removeFirst()
        }
    }

    fun undo(): Boolean {
        if (undoStack.isEmpty()) return false
        redoStack.addLast(bufferManager.getSnapshot())
        val previousState = undoStack.removeLast()
        bufferManager.restoreSnapshot(previousState)
        return true
    }

    fun redo(): Boolean {
        if (redoStack.isEmpty()) return false
        undoStack.addLast(bufferManager.getSnapshot())
        val nextState = redoStack.removeLast()
        bufferManager.restoreSnapshot(nextState)
        return true
    }
}