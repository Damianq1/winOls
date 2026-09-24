package com.winols.app

import com.winols.app.data.DataFormatterEngine
import com.winols.app.model.ConversionProfile
import com.winols.app.model.DataRepresentation
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer

class DataRepresentationTest {

    private val engine = DataFormatterEngine()

    @Test
    fun testFactor0_01() {
        val profile = ConversionProfile(factor = 0.01, offset = 0.0, unit = "%")
        val raw = 1000L
        val physical = profile.rawToPhysical(raw)
        
        assertEquals(10.0, physical, 0.0001)
        assertEquals("10.00 %", profile.formatPhysical(physical))

        val convertedBack = profile.physicalToRaw(10.0, DataRepresentation.UWORD_LE)
        assertEquals(1000L, convertedBack)
    }

    @Test
    fun testBoschTemperatureFormula() {
        // Formuła: (RAW * 0.25) - 40
        val profile = ConversionProfile(factor = 0.25, offset = -40.0, unit = "°C", decimalPlaces = 1)
        
        // 0 RAW -> (0 * 0.25) - 40 = -40.0°C
        assertEquals(-40.0, profile.rawToPhysical(0), 0.001)

        // 160 RAW -> (160 * 0.25) - 40 = 0.0°C
        assertEquals(0.0, profile.rawToPhysical(160), 0.001)

        // 255 RAW -> (255 * 0.25) - 40 = 23.75°C
        assertEquals(23.75, profile.rawToPhysical(255), 0.001)

        // Odwrotna konwersja: 90°C (temperatura robocza silnika) -> (90 + 40) / 0.25 = 520 RAW
        val rawFor90C = profile.physicalToRaw(90.0, DataRepresentation.UWORD_LE)
        assertEquals(520L, rawFor90C)
    }

    @Test
    fun testBufferReadWriteClamping() {
        val buffer = ByteBuffer.allocate(4)
        val profile = ConversionProfile.TEMPERATURE_BOSCH

        // Zapisujemy 100°C do komórki 8-bitowej unsigned (zakres RAW: 0..255)
        // (100 + 40) / 0.25 = 560 RAW -> powinno zostać przycięte do 255
        engine.writePhysicalValue(buffer, 0, 100.0, DataRepresentation.UBYTE, profile)
        val readPhysical = engine.readPhysicalValue(buffer, 0, DataRepresentation.UBYTE, profile)

        // 255 RAW to 23.75°C
        assertEquals(23.75, readPhysical, 0.001)
    }

    @Test
    fun testParseFormulaString() {
        val parsedProfile = engine.parseFormula("(x * 0.25) - 40", unit = "°C", decimalPlaces = 1)
        assertEquals(0.25, parsedProfile.factor, 0.0001)
        assertEquals(-40.0, parsedProfile.offset, 0.0001)
        assertEquals("°C", parsedProfile.unit)
        assertEquals(1, parsedProfile.decimalPlaces)
    }
}