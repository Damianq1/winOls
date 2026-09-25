package com.winols.app.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.winols.app.domain.detector.MapFinder
import com.winols.app.domain.model.EcuBinary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel : ViewModel() {

    private val mapFinder = MapFinder()
    private val _uiState = MutableStateFlow(MainState())
    val uiState: StateFlow<MainState> = _uiState.asStateFlow()

    fun handleIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.LoadBinary -> loadBinary(intent.name, intent.bytes)
            is MainIntent.ScanForMaps -> scanMaps()
            is MainIntent.SelectMap -> _uiState.update { it.copy(selectedMap = intent.map) }
            is MainIntent.ClearBinary -> _uiState.value = MainState()
        }
    }

    private fun loadBinary(fileName: String, data: ByteArray) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val binary = EcuBinary(fileName, data)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    binary = binary,
                    maps = emptyList(),
                    selectedMap = null
                )
            }
        }
    }

    private fun scanMaps() {
        val currentBinary = _uiState.value.binary ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val detectedMaps = withContext(Dispatchers.Default) {
                mapFinder.detectCandidates(currentBinary)
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    maps = detectedMaps,
                    selectedMap = detectedMaps.firstOrNull()
                )
            }
        }
    }
}