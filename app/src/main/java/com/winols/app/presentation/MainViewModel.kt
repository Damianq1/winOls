package com.winols.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.winols.app.domain.model.EcuBinary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel : ViewModel() {

    private val _state = MutableStateFlow<MainState>(MainState.Idle)
    val state: StateFlow<MainState> = _state.asStateFlow()

    fun processIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.LoadBinary -> loadBinaryData(intent.bytes, intent.name)
        }
    }

    private fun loadBinaryData(bytes: ByteArray, name: String) {
        viewModelScope.launch {
            _state.value = MainState.Loading
            try {
                val hexPreview = withContext(Dispatchers.Default) {
                    formatHexView(bytes, previewLimit = 512)
                }
                val binary = EcuBinary(name, bytes.size.toLong(), bytes)
                _state.value = MainState.Loaded(binary, hexPreview)
            } catch (e: Exception) {
                _state.value = MainState.Error(e.message ?: "Unknown error loading binary")
            }
        }
    }

    private fun formatHexView(bytes: ByteArray, previewLimit: Int): String {
        val sb = StringBuilder()
        val limit = minOf(bytes.size, previewLimit)
        for (i in 0 until limit step 16) {
            sb.append(String.format("%08X: ", i))
            val lineBytes = bytes.sliceArray(i until minOf(i + 16, limit))
            for (b in lineBytes) {
                sb.append(String.format("%02X ", b))
            }
            if (lineBytes.size < 16) {
                for (k in 0 until (16 - lineBytes.size)) {
                    sb.append("   ")
                }
            }
            sb.append(" | ")
            for (b in lineBytes) {
                val c = b.toInt().toChar()
                if (c in ' '..'~') sb.append(c) else sb.append('.')
            }
            sb.append("\n")
        }
        return sb.toString()
    }
}