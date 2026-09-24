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
import java.nio.ByteOrder

class MapEditorPercentTest {

    private lateinit var bufferManager: BinaryBufferManager
    private lateinit var mapEditor: MapEditor

    @Before
    fun setup() {
        // Bufor 1 KB wypełniony zerami
        val buffer = ByteBuffer.allocateDirect(1024).order(ByteOrder.LITTLE_ENDIAN)
        bufferManager = BinaryBufferManager(buffer)
        mapEditor = MapEditor(bufferManager)
    }

    @Test
    fun testPercentageIncreaseOnRawUint16() {
        val map = MapDefinition(
            startAddress = 0x00,
            rows = 2,
            columns = 2,
            dataRepresentation = DataRepresentation.UINT16_LE
        )

        // Wpisujemy wartości początkowe: 1000, 2000, 3000, 4000
        bufferManager.writeData(0x00, 1000L, DataRepresentation.UINT16_LE)
        bufferManager.writeData(0x02, 2000L, DataRepresentation.UINT16_LE)
        bufferManager.writeData(0x04, 3000L, DataRepresentation.UINT16_LE)
        bufferManager.writeData(0x06, 4000L, DataRepresentation.UINT16_LE)

        // Zwiększenie o +10%
        val deltas = mapEditor.applyPercentageChange(
            map = map,
            percentDelta = 10.0,
            applyOnPhysical = false
        )

        assertEquals(4, deltas.size)
        assertEquals(1100L, bufferManager.readData(0x00, DataRepresentation.UINT16_LE))
        assertEquals(2200L, bufferManager.readData(0x02, DataRepresentation.UINT16_LE))
        assertEquals(3300L, bufferManager.readData(0x04, DataRepresentation.UINT16_LE))
        assertEquals(4400L, bufferManager.readData(0x06, DataRepresentation.UINT16_LE))
    }

    @Test
    fun testPercentageDecreaseWithClampingUint8() {
        val map = MapDefinition(
            startAddress = 0x10,
            rows = 1,
            columns = 2,
            dataRepresentation = DataRepresentation.UINT8
        )

        bufferManager.writeData(0x10, 100L, DataRepresentation.UINT8)
        bufferManager.writeData(0x11, 250L, DataRepresentation.UINT8)

        // Obniżka o -20%
        mapEditor.applyPercentageChange(
            map = map,
            selectedCells = listOf(0 to 0), // Tylko pierwsza komórka
            percentDelta = -20.0,
            applyOnPhysical = false
        )

        assertEquals(80L, bufferManager.readData(0x10, DataRepresentation.UINT8))
        assertEquals(250L, bufferManager.readData(0x11, DataRepresentation.UINT8)) // Druga nietknięta
    }

    @Test
    fun testPercentageUndo() {
        val map = MapDefinition(
            startAddress = 0x20,
            rows = 1,
            columns = 1,
            dataRepresentation = DataRepresentation.UINT16_LE
        )

        bufferManager.writeData(0x20, 500L, DataRepresentation.UINT16_LE)

        mapEditor.applyPercentageChange(map, percentDelta = 50.0)
        assertEquals(750L, bufferManager.readData(0x20, DataRepresentation.UINT16_LE))

        assertTrue(mapEditor.canUndo)
        mapEditor.undo(DataRepresentation.UINT16_LE)
        assertEquals(500L, bufferManager.readData(0x20, DataRepresentation.UINT16_LE))
    }
}