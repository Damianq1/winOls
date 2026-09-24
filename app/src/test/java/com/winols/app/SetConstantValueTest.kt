package com.winols.app

import com.winols.app.data.BinaryBufferManager
import com.winols.app.edit.MapEditor
import com.winols.app.model.DataRepresentation
import com.winols.app.model.MapDefinition
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer

class SetConstantValueTest {

    @Test
    fun testSetConstantValueUint16WithFactor() {
        // Bufor 64 bajty
        val byteBuf = ByteBuffer.allocateDirect(64)
        val manager = BinaryBufferManager(byteBuf)
        val editor = MapEditor(manager)

        // Mapa 2x2, 16-bit LE, adres bazowy 0x00
        // Physical = RAW * 0.25 - 40.0
        // Chcemy wpisać stałą fizyczną = 80.0
        // RAW = (80.0 - (-40.0)) / 0.25 = 120.0 / 0.25 = 480
        val map = MapDefinition(
            name = "DriversWish",
            startAddress = 0,
            rows = 2,
            columns = 2,
            representation = DataRepresentation.UINT16_LE,
            factor = 0.25,
            offset = -40.0
        )

        val affected = editor.setConstantValue(map, 80.0)

        assertEquals(4, affected)
        assertEquals(480L, manager.readValue(0, DataRepresentation.UINT16_LE))
        assertEquals(480L, manager.readValue(2, DataRepresentation.UINT16_LE))
        assertEquals(480L, manager.readValue(4, DataRepresentation.UINT16_LE))
        assertEquals(480L, manager.readValue(6, DataRepresentation.UINT16_LE))
    }

    @Test
    fun testSetConstantValueSelectedCellsOnly() {
        val byteBuf = ByteBuffer.allocateDirect(16)
        val manager = BinaryBufferManager(byteBuf)
        val editor = MapEditor(manager)

        // Mapa 2x2 8-bit
        val map = MapDefinition(
            name = "EGR",
            startAddress = 0,
            rows = 2,
            columns = 2,
            representation = DataRepresentation.UINT8,
            factor = 1.0,
            offset = 0.0
        )

        // Zaznaczamy tylko komórkę (0, 1) oraz (1, 0)
        val selection = setOf(Pair(0, 1), Pair(1, 0))
        editor.setConstantValue(map, 250.0, selection)

        assertEquals(0L, manager.readValue(0, DataRepresentation.UINT8))   // (0,0) - bez zmian
        assertEquals(250L, manager.readValue(1, DataRepresentation.UINT8)) // (0,1) - zmieniona
        assertEquals(250L, manager.readValue(2, DataRepresentation.UINT8)) // (1,0) - zmieniona
        assertEquals(0L, manager.readValue(3, DataRepresentation.UINT8))   // (1,1) - bez zmian
    }
}