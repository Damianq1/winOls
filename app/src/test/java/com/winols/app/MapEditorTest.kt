package com.winols.app

import com.winols.app.data.BinaryBufferManager
import com.winols.app.edit.MapEditor
import com.winols.app.model.ConversionFormula
import com.winols.app.model.DataType
import com.winols.app.model.MapDefinition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapEditorTest {

    @Test
    fun testCellModificationsAndDeltas() {
        val rawData = ByteArray(64) { 0x00 }
        val buffer = BinaryBufferManager.fromByteArray(rawData)

        val mapDef = MapDefinition(
            id = "IGNITION_MAP",
            name = "Ignition Advance",
            dataAddress = 0,
            rows = 4,
            cols = 4,
            dataType = DataType.UBYTE,
            formula = ConversionFormula(factor = 0.5, offset = -10.0, precision = 1),
            unit = "deg"
        )

        val editor = MapEditor(buffer, mapDef)

        // Raw 0 -> (0 * 0.5) - 10.0 = -10.0 deg
        assertEquals(-10.0, editor.getPhysicalValue(0, 0), 0.001)
        assertFalse(editor.isCellModified(0, 0))

        // Ustawienie kąta wyprzedzenia na 15.0 deg -> raw = (15.0 - (-10.0)) / 0.5 = 50
        editor.setPhysicalValue(0, 0, 15.0)
        assertEquals(15.0, editor.getPhysicalValue(0, 0), 0.001)
        assertEquals(50.0, editor.getRawValue(0, 0), 0.001)
        assertTrue(editor.isCellModified(0, 0))
        assertEquals(25.0, editor.getPhysicalDelta(0, 0), 0.001)

        // Zmiana procentowa o 10%
        editor.applyPercentageChange(10.0, listOf(Pair(0, 0)))
        assertEquals(16.5, editor.getPhysicalValue(0, 0), 0.001)
    }

    @Test
    fun testBilinearInterpolation() {
        val rawData = ByteArray(16) { 0x00 }
        val buffer = BinaryBufferManager.fromByteArray(rawData)

        val mapDef = MapDefinition(
            id = "TARGET_MAP",
            name = "Target Lambda",
            dataAddress = 0,
            rows = 3,
            cols = 3,
            dataType = DataType.UWORD_LE,
            formula = ConversionFormula(factor = 1.0, offset = 0.0, precision = 2)
        )

        val editor = MapEditor(buffer, mapDef)

        editor.setPhysicalValue(0, 0, 100.0)
        editor.setPhysicalValue(0, 2, 200.0)
        editor.setPhysicalValue(2, 0, 100.0)
        editor.setPhysicalValue(2, 2, 200.0)

        editor.interpolateBilinear2D(0, 0, 2, 2)

        assertEquals(150.0, editor.getPhysicalValue(1, 1), 0.01)
        assertEquals(100.0, editor.getPhysicalValue(1, 0), 0.01)
        assertEquals(200.0, editor.getPhysicalValue(1, 2), 0.01)
    }

    @Test
    fun testMapSmoothing() {
        val rawData = ByteArray(9) { 0x00 }
        val buffer = BinaryBufferManager.fromByteArray(rawData)

        val mapDef = MapDefinition(
            id = "SMOOTH_MAP",
            name = "Peak Smoothing",
            dataAddress = 0,
            rows = 3,
            cols = 3,
            dataType = DataType.UBYTE,
            formula = ConversionFormula(factor = 1.0, offset = 0.0, precision = 1)
        )

        val editor = MapEditor(buffer, mapDef)
        // Utwórz pik w środku: 100, otoczenie: 0
        editor.setPhysicalValue(1, 1, 100.0)

        editor.smooth(factor = 0.5)

        // Średnia dla środka z otoczeniem 3x3: (100 + 8 * 0) / 9 = 11.11
        // Wygładzona wartość środka: 100 + 0.5 * (11.11 - 100) = 55.55 -> zaokrąglona do UBYTE
        val centerVal = editor.getPhysicalValue(1, 1)
        assertTrue(centerVal < 100.0 && centerVal > 50.0)
    }
}