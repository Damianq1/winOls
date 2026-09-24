package com.winols.app

import com.winols.app.data.DataFormatterEngine
import com.winols.app.model.CellDataType
import com.winols.app.model.MapDefinition
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DataRepresentationTest {

    @Test
    fun testBoostPressureScaling() {
        val map = MapDefinition(
            id = "MAP_BOOST",
            name = "Turbo Boost",
            startAddress = 0x00,
            rows = 1,
            columns = 1,
            dataType = CellDataType.UWORD,
            factor = 0.01,
            offset = 0.0,
            decimals = 2
        )

        // RAW 250 -> 2.50 bar
        val raw = 250
        val physical = map.rawToPhysical(raw)
        assertEquals(2.5, physical, 0.0001)

        // 2.50 bar -> RAW 250
        val rawCalculated = map.physicalToRaw(2.5)
        assertEquals(250L, rawCalculated)
    }

    @Test
    fun testTemperatureEcuScaling() {
        val map = MapDefinition(
            id = "MAP_COOLANT_TEMP",
            name = "ECT Sensor",
            startAddress = 0x00,
            rows = 1,
            columns = 1,
            dataType = CellDataType.UBYTE,
            factor = 0.25,
            offset = -40.0,
            decimals = 1
        )

        // RAW 0 -> (0 * 0.25) - 40 = -40.0°C
        assertEquals(-40.0, map.rawToPhysical(0), 0.0001)

        // RAW 160 -> (160 * 0.25) - 40 = 0.0°C
        assertEquals(0.0, map.rawToPhysical(160), 0.0001)

        // RAW 520 -> (520 * 0.25) - 40 = 90.0°C
        assertEquals(90.0, map.rawToPhysical(520), 0.0001)

        // 90.0°C -> RAW: (90 - (-40)) / 0.25 = 130 / 0.25 = 520
        // Dla UBYTE zostanie obcięte (clamped) do 255
        val raw90 = map.physicalToRaw(90.0)
        assertEquals(255L, raw90)

        // 10.0°C -> (10 - (-40)) / 0.25 = 50 / 0.25 = 200 RAW
        val raw10 = map.physicalToRaw(10.0)
        assertEquals(200L, raw10)
    }

    @Test
    fun testBufferRoundTrip() {
        val buffer = ByteBuffer.allocateDirect(16)
        val map = MapDefinition(
            id = "MAP_INJ",
            name = "Injection",
            startAddress = 0x02,
            rows = 2,
            columns = 2,
            dataType = CellDataType.UWORD,
            byteOrder = ByteOrder.LITTLE_ENDIAN,
            factor = 0.01,
            offset = 0.0
        )

        // Macierz wejściowa w jednostkach fizycznych
        val originalPhysical = arrayOf(
            doubleArrayOf(12.50, 25.00),
            doubleArrayOf(37.50, 50.00)
        )

        DataFormatterEngine.applyPhysicalMatrix(buffer, map, originalPhysical)
        val extractedPhysical = DataFormatterEngine.extractPhysicalMatrix(buffer, map)

        assertEquals(12.50, extractedPhysical[0][0], 0.001)
        assertEquals(25.00, extractedPhysical[0][1], 0.001)
        assertEquals(37.50, extractedPhysical[1][0], 0.001)
        assertEquals(50.00, extractedPhysical[1][1], 0.001)
    }
}