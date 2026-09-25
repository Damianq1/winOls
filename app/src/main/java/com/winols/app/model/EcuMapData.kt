package com.winols.app.model

/**
 * Reprezentacja dwuwymiarowej mapy ECU wyciętej z pliku binarnego.
 */
data class EcuMapData(
    val name: String,
    val rows: Int,
    val cols: Int,
    val data: FloatArray,
    val xAxis: FloatArray = FloatArray(cols) { it.toFloat() },
    val yAxis: FloatArray = FloatArray(rows) { it.toFloat() }
) {
    val minValue: Float by lazy { data.minOrNull() ?: 0f }
    val maxValue: Float by lazy { data.maxOrNull() ?: 1f }

    fun getValue(row: Int, col: Int): Float {
        return data[row * cols + col]
    }
}