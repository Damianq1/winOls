package com.winols.app

import com.winols.app.data.BinaryBufferManager
import com.winols.app.edit.MapEditor
import com.winols.app.model.DataRepresentation
import com.winols.app.model.MapDefinition
import com.winols.app.model.PhysicalConversion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer

class SmoothingTest {

    private lateinit var bufferManager: BinaryBufferManager
    private lateinit var editor: MapEditor

    @Before
    fun setup() {
        val initialBytes = ByteArray(1024)
        bufferManager = BinaryBufferManager(ByteBuffer.wrap(initialBytes))
        editor = MapEditor(bufferManager)
    }

    @Test
    fun testSmoothingRemovesSpikeIn3x3Map() {
        // Przygotujmy mapę 3x3 z nagłym pikiem w środku (komórka 1,1)
        // [ 10, 10, 10 ]
        // [ 10, 90, 10 ]
        // [ 10, 10, 10 ]
        val rows = 3
        val cols = 3
        val map = MapDefinition(
            name = "Test Spike Map",
            startAddress = 0x00,
            rows = rows,
            cols = cols,
            dataRepresentation = DataRepresentation.UINT8,
            physicalConversion = PhysicalConversion(factor = 1.0, offset = 0.0)
        )

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val value = if (r == 1 && c == 1) 90L else 10L
                val addr = map.startAddress + (r * cols + c)
                bufferManager.writeTypedValue(addr, value, DataRepresentation.UINT8)
            }
        }

        // Wygładzamy tylko komórkę środkową o 80% siły
        val spikeCell = setOf(MapEditor.EditCell(1, 1))
        editor.smoothMap(map, strength = 0.8f, selectedCells = spikeCell)

        val centerValue = bufferManager.readTypedValue(map.startAddress + 4, DataRepresentation.UINT8)

        // Sąsiedzi to same 10. Średnia = 10.
        // Blended = 90 * (1 - 0.8) + 10 * 0.8 = 18 + 8 = 26
        assertEquals(26L, centerValue)
    }

    @Test
    fun testSmoothing1DVector() {
        // Wektor 1x3: [ 100, 200, 100 ]
        val map = MapDefinition(
            name = "1D Curve",
            startAddress = 0x10,
            rows = 1,
            cols = 3,
            dataRepresentation = DataRepresentation.UINT16_LE,
            physicalConversion = PhysicalConversion(factor = 1.0, offset = 0.0)
        )

        bufferManager.writeTypedValue(0x10, 100L, DataRepresentation.UINT16_LE)
        bufferManager.writeTypedValue(0x12, 200L, DataRepresentation.UINT16_LE)
        bufferManager.writeTypedValue(0x14, 100L, DataRepresentation.UINT16_LE)

        // Wygładzanie 50%
        editor.smoothMap(map, strength = 0.5f)

        // Komórka środkowa (0,1): sąsiedzi to (100 + 100) / 2 = 100
        // Nowa wartość = 200 * 0.5 + 100 * 0.5 = 150
        val centerVal = bufferManager.readTypedValue(0x12, DataRepresentation.UINT16_LE)
        assertEquals(150L, centerVal)
    }

    @Test
    fun testSmoothingUndoSupport() {
        val map = MapDefinition(
            name = "Undo Smooth Test",
            startAddress = 0x20,
            rows = 2,
            cols = 2,
            dataRepresentation = DataRepresentation.UINT8,
            physicalConversion = PhysicalConversion.IDENTITY
        )

        bufferManager.writeTypedValue(0x20, 10L, DataRepresentation.UINT8)
        bufferManager.writeTypedValue(0x21, 50L, DataRepresentation.UINT8)
        bufferManager.writeTypedValue(0x22, 10L, DataRepresentation.UINT8)
        bufferManager.writeTypedValue(0x23, 10L, DataRepresentation.UINT8)

        editor.smoothMap(map, strength = 1.0f)
        assertTrue(bufferManager.canUndo())

        bufferManager.undo()
        assertEquals(50L, bufferManager.readTypedValue(0x21, DataRepresentation.UINT8))
    }
}