package com.winols.app.presentation.mvi

import com.winols.app.domain.model.EcuBinary

sealed interface EcuIntent {
    data class LoadBinaryData(val name: String, val bytes: ByteArray) : EcuIntent
    object ClearBinary : EcuIntent
}

data class EcuViewState(
    val isLoading: Boolean = false,
    val binary: EcuBinary? = null,
    val hexDump: String = "",
    val errorMessage: String? = null
)

sealed interface EcuSingleEvent {
    data class ShowToast(val message: String) : EcuSingleEvent
}