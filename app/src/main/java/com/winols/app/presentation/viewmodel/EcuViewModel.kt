package com.winols.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.winols.app.domain.model.EcuBinary
import com.winols.app.presentation.mvi.EcuContract.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class EcuViewModel : ViewModel() {

    private val _state = MutableStateFlow(EcuViewState())
    val state: StateFlow<EcuViewState> = _state.asStateFlow()

    private val _eventChannel = Channel<EcuSingleEvent>()
    val events = _eventChannel.receiveAsFlow()

    fun handleIntent(intent: EcuIntent) {
        when (intent) {
            is EcuIntent.LoadBinaryData -> processBinary(intent.name, intent.bytes)
            is EcuIntent.ClearBinary -> {
                _state.value = EcuViewState()
            }
        }
    }

    private fun processBinary(name: String, bytes: ByteArray) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            try {
                val ecu = EcuBinary(name, bytes)
                val hexDump = ecu.toHexPreview()
                _state.value = EcuViewState(
                    isLoading = false,
                    binary = ecu,
                    hexDump = hexDump,
                    errorMessage = null
                )
                _eventChannel.send(EcuSingleEvent.ShowToast("Załadowano poprawnie: $name"))
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Błąd parsowania pliku"
                )
            }
        }
    }
}