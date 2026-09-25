package com.winols.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.winols.app.core.dispatcher.CoroutineDispatchers
import com.winols.app.core.dispatcher.DefaultCoroutineDispatchers
import com.winols.app.data.binary.BinaryFileManager
import com.winols.app.domain.processor.EcuBinaryProcessor
import com.winols.app.domain.processor.MapCandidate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class EcuUiState(
    val isLoading: Boolean = false,
    val progress: Float = 0f,
    val fileName: String? = null,
    val checksum: Long? = null,
    val detectedMaps: List<MapCandidate> = emptyList(),
    val errorMessage: String? = null
)

class MainViewModel(
    private val dispatchers: CoroutineDispatchers = DefaultCoroutineDispatchers(),
    private val fileManager: BinaryFileManager = BinaryFileManager(dispatchers),
    private val binaryProcessor: EcuBinaryProcessor = EcuBinaryProcessor(dispatchers)
) : ViewModel() {

    private val _uiState = MutableStateFlow(EcuUiState())
    val uiState: StateFlow<EcuUiState> = _uiState.asStateFlow()

    fun loadAndAnalyzeEcuFile(file: File) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, progress = 0f, fileName = file.name) }
            try {
                // 1. Asynchroniczny odczyt I/O
                val binaryData = fileManager.loadBinary(file)

                // 2. Asynchroniczne przeliczanie CPU
                val checksum = binaryProcessor.calculateChecksum(binaryData)
                _uiState.update { it.copy(checksum = checksum) }

                // 3. Reaktywny streaming postępu skanowania
                binaryProcessor.scanMaps(binaryData).collect { (progress, maps) ->
                    _uiState.update { 
                        it.copy(
                            progress = progress,
                            detectedMaps = maps
                        ) 
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.localizedMessage) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}