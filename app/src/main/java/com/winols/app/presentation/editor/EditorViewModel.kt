package com.winols.app.presentation.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.winols.app.core.binary.EcuBinaryBuffer
import com.winols.app.core.binary.finder.MapFinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditorViewModel(
    private val mapFinder: MapFinder = MapFinder()
) : ViewModel() {

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private var ecuBuffer: EcuBinaryBuffer? = null

    fun processIntent(intent: EditorIntent) {
        when (intent) {
            is EditorIntent.LoadBinary -> loadBinaryData(intent.bytes)
            is EditorIntent.ScanForMaps -> scanMaps()
            is EditorIntent.SelectMap -> _state.update { it.copy(selectedMap = intent.mapDef) }
            is EditorIntent.ModifyCell -> modifyCell(intent.offset, intent.newValue)
            is EditorIntent.RecalculateChecksum -> calculateChecksum()
        }
    }

    private fun loadBinaryData(bytes: ByteArray) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                ecuBuffer = withContext(Dispatchers.Default) { EcuBinaryBuffer(bytes) }
                _state.update {
                    it.copy(
                        isLoading = false,
                        isLoaded = true,
                        binarySize = bytes.size
                    )
                }
                processIntent(EditorIntent.ScanForMaps)
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    private fun scanMaps() {
        val buffer = ecuBuffer ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val maps = withContext(Dispatchers.Default) {
                mapFinder.findPotential16BitMaps(buffer)
            }
            _state.update { it.copy(isLoading = false, detectedMaps = maps) }
        }
    }

    private fun modifyCell(offset: Int, value: Short) {
        ecuBuffer?.write16Bit(offset, value)
    }

    private fun calculateChecksum() {
        // Logika rekalkulacji sumy kontrolnej w Dispatchers.Default
    }
}