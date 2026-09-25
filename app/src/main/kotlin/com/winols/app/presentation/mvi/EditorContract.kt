package com.winols.app.presentation.mvi

import com.winols.app.domain.model.EcuBinaryFile
import com.winols.app.domain.model.EcuMapDefinition

sealed interface EditorIntent {
    data class LoadBinary(val fileName: String, val bytes: ByteArray) : EditorIntent
    data class SelectMap(val mapDefinition: EcuMapDefinition) : EditorIntent
    data class ModifyCell(val address: Int, val rawValue: Int) : EditorIntent
    object ScanForMaps : EditorIntent
}

data class EditorState(
    val isLoading: Boolean = false,
    val binaryFile: EcuBinaryFile? = null,
    val discoveredMaps: List<EcuMapDefinition> = emptyList(),
    val selectedMap: EcuMapDefinition? = null,
    val cursorOffset: Int = 0,
    val errorMessage: String? = null
)

sealed interface EditorEffect {
    data class ShowToast(val message: String) : EditorEffect
}