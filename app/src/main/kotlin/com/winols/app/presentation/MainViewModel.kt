package com.winols.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.winols.app.domain.model.EcuBinaryBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface EcuUiState {
    object Idle : EcuUiState
    object Loading : EcuUiState
    data class Loaded(
        val fileName: String,
        val buffer: EcuBinaryBuffer,
        val checksum: String
    ) : EcuUiState
    data class Error(val message: String) : EcuUiState
}

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<EcuUiState>(EcuUiState.Idle)
    val uiState: StateFlow<EcuUiState> = _uiState.asStateFlow()

    fun loadBinary(fileName: String, bytes: ByteArray) {
        viewModelScope.launch {
            _uiState.value = EcuUiState.Loading
            try {
                val buffer = withContext(Dispatchers.Default) {
                    EcuBinaryBuffer(bytes)
                }
                val checksumHex = String.format("0x%08X", buffer.calculateChecksum32())
                _uiState.value = EcuUiState.Loaded(fileName, buffer, checksumHex)
            } catch (e: Exception) {
                _uiState.value = EcuUiState.Error(e.localizedMessage ?: "Błąd parsowania wsadu.")
            }
        }
    }
}