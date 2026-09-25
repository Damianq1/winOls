package com.winols.app.presentation.main

import android.net.Uri
import com.winols.app.domain.model.EcuBinary

sealed interface MainIntent {
    data class LoadBinary(val uri: Uri) : MainIntent
    data object ClearBinary : MainIntent
}

sealed interface MainState {
    data object Idle : MainState
    data object Loading : MainState
    data class Loaded(val ecuBinary: EcuBinary, val previewHex: String) : MainState
    data class Error(val message: String) : MainState
}