package com.winols.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.winols.app.data.buffer.BinaryMemoryBuffer
import com.winols.app.domain.model.EcuBinaryFile
import com.winols.app.domain.model.EcuMapDefinition
import com.winols.app.presentation.mvi.EditorEffect
import com.winols.app.presentation.mvi.EditorIntent
import com.winols.app.presentation.mvi.EditorState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EcuEditorViewModel : ViewModel() {

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private val _effect = Channel<EditorEffect>()
    val effect = _effect.receiveAsFlow()

    private var memoryBuffer: BinaryMemoryBuffer? = null

    fun handleIntent(intent: EditorIntent) {
        when (intent) {
            is EditorIntent.LoadBinary -> loadBinary(intent.fileName, intent.bytes)
            is EditorIntent.SelectMap -> selectMap(intent.mapDefinition)
            is EditorIntent.ModifyCell -> modifyCell(intent.address, intent.rawValue)
            is EditorIntent.ScanForMaps -> scanMaps()
        }
    }

    private fun loadBinary(name: String, bytes: ByteArray) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            memoryBuffer = BinaryMemoryBuffer(bytes)
            val binary = EcuBinaryFile(fileName = name, rawBytes = bytes)
            _state.update { 
                it.copy(
                    isLoading = false,
                    binaryFile = binary,
                    cursorOffset = 0,
                    errorMessage = null
                ) 
            }
        }
    }

    private fun selectMap(mapDef: EcuMapDefinition) {
        _state.update { it.copy(selectedMap = mapDef, cursorOffset = mapDef.startAddress) }
    }

    private fun modifyCell(address: Int, rawValue: Int) {
        val currentMap = _state.value.selectedMap ?: return
        val buffer = memoryBuffer ?: return

        buffer.writeValue(address, rawValue, currentMap.dataSize, currentMap.endianness)
        _state.update { it.copy(binaryFile = it.binaryFile?.copy(rawBytes = buffer.getRawBytes())) }
    }

    private fun scanMaps() {
        viewModelScope.launch {
            // Logika przeszukiwania nagłówków Bosch / VAG / Siemens (heurytyka osi X/Y)
            _effect.send(EditorEffect.ShowToast("Przeszukiwanie wsadów pod kątem map 2D/3D..."))
        }
    }
}