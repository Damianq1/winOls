package com.winols.app.engine

import com.winols.app.data.BinaryBufferManager
import com.winols.app.edit.MapEditor
import com.winols.app.model.MapDefinition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Stack

/**
 * Koordynator sesji edycyjnej projektu.
 * Zarządza stanem bufora, historią Undo/Redo oraz aktualnie załadowaną listą map.
 */
class ProjectSessionManager {

    private var bufferManager: BinaryBufferManager? = null
    private var mapEditor: MapEditor? = null

    private val undoStack = Stack<List<MapEditor.EditDelta>>()
    private val redoStack = Stack<List<MapEditor.EditDelta>>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _detectedMaps = MutableStateFlow<List<MapDefinition>>(emptyList())
    val detectedMaps: StateFlow<List<MapDefinition>> = _detectedMaps.asStateFlow()

    fun attachBuffer(manager: BinaryBufferManager) {
        this.bufferManager = manager
        this.mapEditor = MapEditor(manager)
        clearHistory()
    }

    fun getBufferManager(): BinaryBufferManager =
        bufferManager ?: throw IllegalStateException("Bufor binarny nie został zainicjalizowany.")

    fun getMapEditor(): MapEditor =
        mapEditor ?: throw IllegalStateException("Edytor map nie został zainicjalizowany.")

    fun setMaps(maps: List<MapDefinition>) {
        _detectedMaps.value = maps
    }

    suspend fun executeOperation(
        mapDef: MapDefinition,
        op: MapEditor.OperationType,
        operand: Double,
        selectionMask: Array<BooleanArray>? = null
    ) {
        val editor = getMapEditor()
        val deltas = editor.applyOperation(mapDef, op, operand, selectionMask)
        if (deltas.isNotEmpty()) {
            undoStack.push(deltas)
            redoStack.clear()
            updateHistoryState()
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val lastAction = undoStack.pop()
        val buffer = getBufferManager()

        val invertedDeltas = mutableListOf<MapEditor.EditDelta>()
        for (delta in lastAction) {
            val currentBytes = buffer.readBytes(delta.offset, delta.previousRaw.size)
            buffer.writeBytes(delta.offset, delta.previousRaw)
            invertedDeltas.add(MapEditor.EditDelta(delta.offset, currentBytes, delta.previousRaw))
        }

        redoStack.push(invertedDeltas)
        updateHistoryState()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val nextAction = redoStack.pop()
        val buffer = getBufferManager()

        val reAppliedDeltas = mutableListOf<MapEditor.EditDelta>()
        for (delta in nextAction) {
            val currentBytes = buffer.readBytes(delta.offset, delta.newRaw.size)
            buffer.writeBytes(delta.offset, delta.newRaw)
            reAppliedDeltas.add(MapEditor.EditDelta(delta.offset, currentBytes, delta.newRaw))
        }

        undoStack.push(reAppliedDeltas)
        updateHistoryState()
    }

    suspend fun saveCurrentFile(destination: File? = null) {
        val buffer = getBufferManager()
        if (destination != null) {
            buffer.saveToFile(destination)
        } else {
            buffer.saveToFile()
        }
    }

    private fun clearHistory() {
        undoStack.clear()
        redoStack.clear()
        updateHistoryState()
    }

    private fun updateHistoryState() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }
}