package com.winols.app

import com.winols.app.data.BinaryBufferManager
import com.winols.app.edit.MapEditor
import com.winols.app.model.DataRepresentation
import com.winols.app.model.MapDefinition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmoothingTest {

    @Test
    fun testSmoothing2DSharpPeak() {
        val manager = BinaryBufferManager(ByteArray(9))
        val editor = MapEditor(manager)

        // Siatka 3x3 z nagłym pikiem w środku
        val grid = doubleArrayOf(
            10.0, 10.0, 10.0,
            10.0, 100.0, 10.0,
            10.0, 10.0, 10.0
        )

        // Wykonaj wygładzenie z siłą 1.0
        val result = editor.smoothMapData(grid, rows = 3, cols = 3, strength = 1.0)

        // Środkowy element powinien spaść z 100 w stronę wartości otoczenia
        assertTrue(result[4] < 100.0)
        // Rogi i krawędzie powinny nieco wzrosnąć
        assertTrue(result[0] > 10.0)
        assertTrue(result[1] > 10.0)
    }

    @Test
    fun testSmoothingWithMapDefinition() {
        val rawData = byteArrayOf(
            10, 10, 10,
            10, 50, 10,
            10, 10, 10
        )
        val manager = BinaryBufferManager(rawData)
        val editor = MapEditor(manager)

        val mapDef = MapDefinition(
            name = "TestMap",
            startAddress = 0,
            rows = 3,
            columns = 3,
            representation = DataRepresentation.UINT8,
            factor = 1.0,
            offset = 0.0
        )

        editor.applyOperation(mapDef, MapEditor.Operation.SMOOTH, param = 0.8)

        val centerValue = manager.readValue(4, DataRepresentation.UINT8)
        assertTrue("Wartość środkowa powinna zostać wygładzona w dół", centerValue < 50)
    }
}