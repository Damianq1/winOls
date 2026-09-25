package com.winols.app.model

data class EcuMap(
    val id: String,
    val name: String,
    val addressHex: String,
    val columns: Int, // X-Axis (np. Obroty RPM)
    val rows: Int,    // Y-Axis (np. Obciążenie / Ciśnienie)
    val rawValues: FloatArray,
    val factor: Float = 1.0f,
    val offset: Float = 0.0f
) {
    val minVal: Float by lazy { rawValues.minOrNull()?.times(factor)?.plus(offset) ?: 0f }
    val maxVal: Float by lazy { rawValues.maxOrNull()?.times(factor)?.plus(offset) ?: 1f }

    fun getValue(x: Int, y: Int): Float {
        val index = y * columns + x
        return if (index in rawValues.indices) (rawValues[index] * factor) + offset else 0f
    }
}