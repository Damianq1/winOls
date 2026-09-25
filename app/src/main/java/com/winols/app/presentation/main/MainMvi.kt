package com.winols.app.presentation.main

import com.winols.app.domain.model.EcuBinary
import com.winols.app.domain.model.EcuMapCandidate

sealed interface MainIntent {
    data class LoadBinary(val name: String, val bytes: ByteArray) : MainIntent
    data class SelectMap(val map: EcuMapCandidate) : MainIntent
    data object ScanForMaps : MainIntent
    data object ClearBinary : MainIntent
}

data class MainState(
    val isLoading: Boolean = false,
    val binary: EcuBinary? = null,
    val maps: List<EcuMapCandidate> = emptyList(),
    val selectedMap: EcuMapCandidate? = null,
    val error: String? = null
)