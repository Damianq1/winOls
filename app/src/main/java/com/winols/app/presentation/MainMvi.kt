package com.winols.app.presentation

import com.winols.app.domain.model.EcuBinary

sealed interface MainIntent {
    data class LoadBinary(val bytes: ByteArray, val name: String) : MainIntent
}

sealed interface MainState {
    data object Idle : MainState
    data object Loading : MainState
    data class Loaded(val binary: EcuBinary, val previewHex: String) : MainState
    data class Error(val message: String) : MainState
}