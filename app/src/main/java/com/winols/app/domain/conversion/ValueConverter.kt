package com.winols.app.domain.conversion

import com.winols.app.domain.model.DataFormat
import kotlin.math.roundToLong

/**
 * Model parametrów konwersji używanych w mapach ECU (WinOLS / A2L / DAMOS).
 *
 * Wzór wprost:    Fizyczna = (Surowa * factor) + offset
 * Wzór odwrotny: Surowa   = round((Fizyczna - offset) / factor)
 */
data class ConversionProfile(
    val factor: Double = 1.0,
    val offset: Double = 0.0,
    val unit: String = "",
    val precision: Int = 2
)

object ValueConverter {

    /**
     * Przelicza surową wartość liczbową (odczytaną z pliku binarnego) na jednostkę fizyczną.
     */
    fun rawToPhysical(rawValue: Number, profile: ConversionProfile): Double {
        return (rawValue.toDouble() * profile.factor) + profile.offset
    }

    /**
     * Konwertuje wartość fizyczną wprowadzoną przez użytkownika z powrotem na surową liczbę binarną,
     * zabezpieczając przed przekroczeniem zakresu wybranego typu binarnego (clamping).
     */
    fun physicalToRaw(physicalValue: Double, profile: ConversionProfile, format: DataFormat): Long {
        require(profile.factor != 0.0) { "Factor nie może być równy 0" }

        val unroundedRaw = (physicalValue - profile.offset) / profile.factor
        val rawCandidate = unroundedRaw.roundToLong()

        return rawCandidate.coerceIn(format.minValue, format.maxValue)
    }

    /**
     * Formatuje wartość fizyczną do stringa zgodnie z wymaganą liczbą miejsc po przecinku i jednostką.
     */
    fun formatPhysical(value: Double, profile: ConversionProfile): String {
        val formatted = String.format("%.${profile.precision}f", value)
        return if (profile.unit.isBlank()) formatted else "$formatted ${profile.unit}"
    }

    // Predefiniowane presety konwersji typowe dla ECU (Bosch ME7, EDC15/16/17)
    val PRESET_TEMPERATURE_COOLANT = ConversionProfile(factor = 0.75, offset = -48.0, unit = "°C", precision = 1)
    val PRESET_TEMPERATURE_INTAKE = ConversionProfile(factor = 0.25, offset = -40.0, unit = "°C", precision = 2) // np. x * 0.25 - 40
    val PRESET_PRESSURE_MBAR_16BIT = ConversionProfile(factor = 0.01, offset = 0.0, unit = "mbar", precision = 2) // np. x * 0.01
    val PRESET_PRESSURE_HPA = ConversionProfile(factor = 0.1, offset = 0.0, unit = "hPa", precision = 1)
    val PRESET_ENGINE_RPM = ConversionProfile(factor = 0.25, offset = 0.0, unit = "rpm", precision = 0)
    val PRESET_LAMBDA = ConversionProfile(factor = 0.001, offset = 0.0, unit = "λ", precision = 3)
    val PRESET_IGNITION_ANGLE = ConversionProfile(factor = 0.75, offset = 0.0, unit = "°KW", precision = 2)
}