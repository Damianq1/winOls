package com.winols.app.presentation.editor

import com.winols.app.core.binary.model.EcuMapDefinition

sealed interface EditorIntent {
    data class LoadBinary(val bytes: ByteArray) : EditorIntent
    data class SelectMap(val mapDef: EcuMapDefinition) : EditorIntent
    data class ModifyCell(val offset: Int, val newValue: Short) : EditorIntent
    object ScanForMaps : EditorIntent
    object RecalculateChecksum : EditorIntent
}

data class EditorState(
    val isLoading: Boolean = false,
    val binarySize: Int = 0,
    val isLoaded: Boolean = false,
    val detectedMaps: List<EcuMapDefinition> = emptyList(),
    val selectedMap: EcuMapDefinition? = null,
    val isChecksumValid: Boolean? = null,
    val errorMessage: String? = null
)