package com.winols.app

import com.winols.app.data.BinaryBufferManager
import com.winols.app.edit.MapEditor
import com.winols.app.model.DataRepresentation
import com.winols.app.model.MapDefinition
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.nio.ByteOrder

class MapEditorDeltaTest {

    private lateinit var bufferManager: BinaryBufferManager
    private lateinit var editor: MapEditor

    @Before
    fun setup() {
        val buffer = ByteArray(256) { 0 }
        bufferManager = BinaryBufferManager.fromBytes(buffer)
        editor = MapEditor(bufferManager)
    }

    @Test
    fun testAddRawValueWithClamping() {
        val map = MapDefinition(
            name = "Test Map",
            address = 0x10,
            rows = 1,
            columns = 2,
            representation = DataRepresentation.UINT8,
            factor = 1.0,
            offset = 0.0
        )

        bufferManager.setByte(0x10, 200.toByte())
        bufferManager.setByte(0x11, 50.toByte())

        // Dodanie 100 RAW: 200 -> obcięte do 255 (UINT8 limit), 50 -> 150
        editor.applyDelta(map, delta = 100.0, isPhysical = false)

        assertEquals(255, bufferManager.getByte(0x10).toInt() and 0xFF)
        assertEquals(150, bufferManager.getByte(0x11).toInt() and 0xFF)
    }

    @Test
    fun testSubtractPhysicalValue() {
        // Factor 0.1, Offset 0.0 -> RAW 100 = 10.0 fizycznie
        val map = MapDefinition(
            name = "Pressure Map",
            address = 0x20,
            rows = 1,
            columns = 1,
            representation = DataRepresentation.UINT16,
            factor = 0.1,
            offset = 0.0
        )

        bufferManager.setShort(0x20, 1000.toShort(), ByteOrder.LITTLE_ENDIAN) // 100.0 Bar

        // Odejmujemy 25.5 Bar fizycznie -> nowy stan: 74.5 Bar -> RAW = 745
        editor.applyDelta(map, delta = -25.5, isPhysical = true)

        val rawResult = bufferManager.getShort(0x20, ByteOrder.LITTLE_ENDIAN).toLong() and 0xFFFFL
        assertEquals(745L, rawResult)
    }
}