package com.winols.app.engine

import com.winols.app.model.DataType
import kotlin.math.roundToLong

/**
 * Silnik konwersji i transformacji jednostek ECU.
 * Realizuje dwukierunkowe przeliczanie:
 *   Physical = (RAW * Factor) + Offset
 *   RAW = (Physical - Offset) / Factor
 * z zaokrąglaniem, nasyceniem (saturation/clamping) oraz obsługą typowych profili sterowników.
 */
object ScalingConversionEngine {

    data class ScalingFormula(
        val factor: Double = 1.0,
        val offset: Double = 0.0,
        val unit: String = "",
        val decimalPlaces: Int = 2
    )

    /**
     * Zestaw standardowych profili przeliczników znanych ze sterowników Bosch/Siemens/Marelli.
     */
    val STANDARD_PROFILES = mapOf(
        "RPM" to ScalingFormula(factor = 0.25, offset = 0.0, unit = "rpm", decimalPlaces = 0),
        "RPM_16BIT" to ScalingFormula(factor = 1.0, offset = 0.0, unit = "rpm", decimalPlaces = 0),
        "BOOST_PRESSURE_MBAR" to ScalingFormula(factor = 0.039063, offset = 0.0, unit = "mbar", decimalPlaces = 1),
        "PRESSURE_HPA" to ScalingFormula(factor = 1.0, offset = 0.0, unit = "hPa", decimalPlaces = 0),
        "TEMPERATURE_C_EDC15" to ScalingFormula(factor = 0.1, offset = -273.1, unit = "°C", decimalPlaces = 1),
        "TEMPERATURE_C_BOSCH" to ScalingFormula(factor = 0.75, offset = -48.0, unit = "°C", decimalPlaces = 1),
        "INJECTION_QUANTITY_MG" to ScalingFormula(factor = 0.01, offset = 0.0, unit = "mg/hub", decimalPlaces = 2),
        "TORQUE_NM" to ScalingFormula(factor = 0.1, offset = 0.0, unit = "Nm", decimalPlaces = 1),
        "LAMBDA" to ScalingFormula(factor = 0.0009765625, offset = 0.0, unit = "λ", decimalPlaces = 3),
        "RAIL_PRESSURE_BAR" to ScalingFormula(factor = 0.1, offset = 0.0, unit = "bar", decimalPlaces = 1),
        "PERCENTAGE" to ScalingFormula(factor = 0.0030517578, offset = 0.0, unit = "%", decimalPlaces = 2),
        "PEDAL_POSITION" to ScalingFormula(factor = 0.01, offset = 0.0, unit = "%", decimalPlaces = 1)
    )

    /**
     * Konwertuje wartość surową (RAW) na jednostkę fizyczną.
     */
    fun rawToPhysical(
        raw: Double,
        factor: Double = 1.0,
        offset: Double = 0.0
    ): Double {
        return (raw * factor) + offset
    }

    /**
     * Konwertuje wartość fizyczną na surową wartość binarną (RAW), uwzględniając zakres i typ danych docelowych.
     */
    fun physicalToRaw(
        physical: Double,
        factor: Double = 1.0,
        offset: Double = 0.0,
        dataType: DataType = DataType.UINT16
    ): Double {
        if (factor == 0.0) throw IllegalArgumentException("Współczynnik Factor nie może wynosić 0.0")

        val rawCalculated = (physical - offset) / factor

        return when (dataType) {
            DataType.FLOAT32 -> rawCalculated
            else -> {
                val (minVal, maxVal) = getDataTypeRange(dataType)
                val rounded = rawCalculated.roundToLong()
                rounded.coerceIn(minVal, maxVal).toDouble()
            }
        }
    }

    /**
     * Formatuje wartość fizyczną do czytelnego ciągu znaków z odpowiednią precyzją i jednostką.
     */
    fun formatPhysical(value: Double, formula: ScalingFormula): String {
        val formatString = "%.${formula.decimalPlaces}f"
        val formattedNumber = String.format(formatString, value)
        return if (formula.unit.isNotEmpty()) "$formattedNumber ${formula.unit}" else formattedNumber
    }

    /**
     * Zwraca dopuszczalny zakres wartości całkowitych dla danego typu danych w ECU.
     */
    fun getDataTypeRange(dataType: DataType): Pair<Long, Long> {
        return when (dataType) {
            DataType.UINT8 -> Pair(0L, 255L)
            DataType.INT8 -> Pair(-128L, 127L)
            DataType.UINT16 -> Pair(0L, 65535L)
            DataType.INT16 -> Pair(-32768L, 32767L)
            DataType.UINT32 -> Pair(0L, 4294967295L)
            DataType.INT32 -> Pair(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong())
            DataType.FLOAT32 -> Pair(Long.MIN_VALUE, Long.MAX_VALUE)
        }
    }

    /**
     * Przelicza całą macierz RAW na wartości fizyczne.
     */
    fun batchRawToPhysical(
        matrix: Array<DoubleArray>,
        factor: Double,
        offset: Double
    ): Array<DoubleArray> {
        return Array(matrix.size) { r ->
            DoubleArray(matrix[r].size) { c ->
                rawToPhysical(matrix[r][c], factor, offset)
            }
        }
    }

    /**
     * Przelicza całą macierz wartości fizycznych na RAW ze sprawdzaniem nasycenia (clamping).
     */
    fun batchPhysicalToRaw(
        matrix: Array<DoubleArray>,
        factor: Double,
        offset: Double,
        dataType: DataType
    ): Array<DoubleArray> {
        return Array(matrix.size) { r ->
            DoubleArray(matrix[r].size) { c ->
                physicalToRaw(matrix[r][c], factor, offset, dataType)
            }
        }
    }
}