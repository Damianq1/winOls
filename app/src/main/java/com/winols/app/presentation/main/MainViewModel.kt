package com.winols.app.presentation.main

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.winols.app.domain.model.EcuBinary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.zip.CRC32

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<MainState>(MainState.Idle)
    val state: StateFlow<MainState> = _state.asStateFlow()

    fun handleIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.LoadBinary -> processBinary(intent.uri)
            is MainIntent.ClearBinary -> _state.value = MainState.Idle
        }
    }

    private fun processBinary(uri: Uri) {
        viewModelScope.launch {
            _state.value = MainState.Loading
            try {
                val binary = withContext(Dispatchers.IO) {
                    val context = getApplication<Application>().applicationContext
                    var fileName = "unknown.bin"
                    
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            fileName = cursor.getString(nameIndex)
                        }
                    }

                    val bytes = context.contentResolver.openInputStream(uri)?.use { inputStream: InputStream ->
                        inputStream.readBytes()
                    } ?: throw IllegalStateException("Nie można odczytać strumienia pliku")

                    val crc32 = CRC32().apply { update(bytes) }.value
                    EcuBinary(
                        fileName = fileName,
                        sizeBytes = bytes.size,
                        rawBytes = bytes,
                        checksum = crc32
                    )
                }

                val preview = generateHexPreview(binary.rawBytes, limitBytes = 256)
                _state.value = MainState.Loaded(ecuBinary = binary, previewHex = preview)
            } catch (e: Exception) {
                _state.value = MainState.Error(e.localizedMessage ?: "Błąd parsowania pliku binarnego")
            }
        }
    }

    private fun generateHexPreview(bytes: ByteArray, limitBytes: Int): String {
        val sb = StringBuilder()
        val limit = minOf(bytes.size, limitBytes)
        for (i in 0 until limit step 16) {
            sb.append(String.format("%08X  ", i))
            for (j in 0 until 16) {
                if (i + j < limit) {
                    sb.append(String.format("%02X ", bytes[i + j]))
                } else {
                    sb.append("   ")
                }
            }
            sb.append(" |")
            for (j in 0 until 16) {
                if (i + j < limit) {
                    val b = bytes[i + j].toInt().toChar()
                    sb.append(if (b in ' '..'~') b else '.')
                }
            }
            sb.append("|\n")
        }
        return sb.toString()
    }
}