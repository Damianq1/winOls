package com.winols.app.core.model

/**
 * Reprezentacja pojedynczej mapy ECU (np. Dawka paliwa, Doładowanie).
 */
data class EcuMap(
    val id: String,
    val name: String,
    val startAddress: Int,
    val rows: Int,
    val columns: Int,
    val cellSizeInBytes: Int = 2,
    val factor: Double = 1.0,
    val offset: Double = 0.0,
    val unit: String = ""
) {
    val totalSizeBytes: Int get() = rows * columns * cellSizeInBytes
    val endAddress: Int get() = startAddress + totalSizeBytes
}