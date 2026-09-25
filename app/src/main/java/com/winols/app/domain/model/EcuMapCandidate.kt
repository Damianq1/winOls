package com.winols.app.domain.model

data class EcuMapCandidate(
    val id: String,
    val startOffset: Int,
    val rows: Int,
    val cols: Int,
    val bitResolution: Int = 16,
    val isSigned: Boolean = false,
    val title: String = "Map_0x${startOffset.toString(16).uppercase()}"
)