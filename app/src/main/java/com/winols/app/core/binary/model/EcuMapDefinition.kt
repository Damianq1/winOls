package com.winols.app.core.binary.model

data class EcuMapDefinition(
    val id: String,
    val name: String,
    val startOffset: Int,
    val rows: Int,
    val cols: Int,
    val is16Bit: Boolean = true,
    val factor: Double = 1.0,
    val offsetFactor: Double = 0.0,
    val xAxisAddress: Int? = null,
    val yAxisAddress: Int? = null
) {
    val totalElements: Int get() = rows * cols
    val totalByteSize: Int get() = totalElements * (if (is16Bit) 2 else 1)
}