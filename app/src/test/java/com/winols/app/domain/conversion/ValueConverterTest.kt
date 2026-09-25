package com.winols.app.domain.conversion

import com.winols.app.domain.model.DataFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class ValueConverterTest {

    @Test
    fun testRawToPhysical_withFactorAndOffset() {
        // Test dla wzoru: x * 0.25 - 40 (temperatura dolotu/oleju w VAG EDC/ME7)
        val profile = ValueConverter.PRESET_TEMPERATURE_INTAKE
        val rawInput = 200 // 200 * 0.25 - 40 = 50 - 40 = 10.0 °C

        val physical = ValueConverter.rawToPhysical(rawInput, profile)
        assertEquals(10.0, physical, 0.0001)
    }

    @Test
    fun testRawToPhysical_pressureFactor() {
        // Test dla wzoru: x * 0.01
        val profile = ValueConverter.PRESET_PRESSURE_MBAR_16BIT
        val rawInput = 25000 // 25000 * 0.01 = 250.00 mbar

        val physical = ValueConverter.rawToPhysical(rawInput, profile)
        assertEquals(250.0, physical, 0.0001)
    }

    @Test
    fun testPhysicalToRaw_reverseConversion() {
        val profile = ValueConverter.PRESET_TEMPERATURE_INTAKE // factor 0.25, offset -40.0
        val physicalValue = 10.0 // (10.0 - (-40.0)) / 0.25 = 50.0 / 0.25 = 200

        val raw = ValueConverter.physicalToRaw(physicalValue, profile, DataFormat.UBYTE)
        assertEquals(200L, raw)
    }

    @Test
    fun testPhysicalToRaw_clampsOnOverflow() {
        val profile = ValueConverter.PRESET_TEMPERATURE_INTAKE // 8-bit unsigned (0..255)
        // Maksymalna wartość dla raw 255 to: 255 * 0.25 - 40 = 23.75 °C
        // Próba zapisu 100 °C: (100 - (-40)) / 0.25 = 560 -> przekracza UBYTE (max 255)
        val excessivePhysical = 100.0

        val clampedRaw = ValueConverter.physicalToRaw(excessivePhysical, profile, DataFormat.UBYTE)
        assertEquals(255L, clampedRaw)
    }

    @Test
    fun testPhysicalToRaw_clampsOnUnderflow() {
        val profile = ValueConverter.PRESET_TEMPERATURE_INTAKE
        // Wartość poniżej zakresu 0: (-50 - (-40)) / 0.25 = -40 -> UBYTE nie pozwala na wartości ujemne
        val negativePhysical = -50.0

        val clampedRaw = ValueConverter.physicalToRaw(negativePhysical, profile, DataFormat.UBYTE)
        assertEquals(0L, clampedRaw)
    }

    @Test
    fun testFormatPhysical() {
        val profile = ValueConverter.PRESET_TEMPERATURE_COOLANT
        val formatted = ValueConverter.formatPhysical(89.25, profile)
        assertEquals("89.3 °C", formatted.replace(',', '.'))
    }
}