package com.winols.app

import com.winols.app.data.BinaryBufferManager
import com.winols.app.edit.MapEditor
import com.winols.app.model.DataRepresentation
import com.winols.app.model.MapDefinition
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.nio.ByteOrder

class SetConstantValueTest {

    private lateinit var bufferManager: BinaryBufferManager
    private lateinit var editor: MapEditor

    @Before
    fun setUp() {
        bufferManager = BinaryBufferManager(64)
        editor = MapEditor(bufferManager)
    }

    @Test
    fun testSetConstantRawValue_16Bit_Unsigned() {
        val map = MapDefinition(
            name = "TestMap_16Bit",
            address = 0x00,
            rows = 2,
            cols = 2,
            representation = DataRepresentation.UINT16_LE,
            factor = 1.0,
            offset = 0.0
        )

        // Ustawienie stałej wartości 5000 dla całej mapy
        editor.setConstantRawValue(map, 5000L)

        val b0 = bufferManager.readShort(0, ByteOrder.LITTLE_ENDIAN).toInt() and 0xFFFF
        val b1 = bufferManager.readShort(2, ByteOrder.LITTLE_ENDIAN).toInt() and 0xFFFF
        val b2 = bufferManager.readShort(4, ByteOrder.LITTLE_ENDIAN).toInt() and 0xFFFF
        val b3 = bufferManager.readShort(6, ByteOrder.LITTLE_ENDIAN).toInt() and 0xFFFF

        assertEquals(5000, b0)
        assertEquals(5000, b1)
        assertEquals(5000, b2)
        assertEquals(5000, b3)
    }

    @Test
    fun testSetConstantPhysicalValue_WithFactorAndOffset() {
        val map = MapDefinition(
            name = "PressureMap",
            address = 0x00,
            rows = 1,
            cols = 2,
            representation = DataRepresentation.UINT8,
            factor = 0.1,
            offset = 1.0
        )

        // Physical = RAW * 0.1 + 1.0 -> RAW = (Physical - 1.0) / 0.1
        // Dla Physical = 3.5 -> RAW = (3.5 - 1.0) / 0.1 = 25
        editor.setConstantPhysicalValue(map, 3.5)

        val raw0 = bufferManager.readByte(0).toInt() and 0xFF
        val raw1 = bufferManager.readByte(1).toInt() and 0xFF

        assertEquals(25, raw0)
        assertEquals(25, raw1)
    }

    @Test
    fun testSetConstantPhysicalValue_SelectedCellsOnly() {
        val map = MapDefinition(
            name = "SelectedCellsMap",
            address = 0x00,
            rows = 2,
            cols = 2,
            representation = DataRepresentation.UINT8,
            factor = 1.0,
            offset = 0.0
        )

        // Tylko komórka (0, 1) oraz (1, 0)
        val selected = setOf(Pair(0, 1), Pair(1, 0))
        editor.setConstantPhysicalValue(map, 100.0, selected)

        assertEquals(0, bufferManager.readByte(0).toInt())
        assertEquals(100, bufferManager.readByte(1).toInt())
        assertEquals(100, bufferManager.readByte(2).toInt())
        assertEquals(0, bufferManager.readByte(3).toInt())
    }

    @Test
    fun testClampUpperBound() {
        val map = MapDefinition(
            name = "ClampMap",
            address = 0x00,
            rows = 1,
            cols = 1,
            representation = DataRepresentation.UINT8,
            factor = 1.0,
            offset = 0.0
        )

        // Próba wpisania 999 do pola 8-bit unsigned (max 255)
        editor.setConstantRawValue(map, 999L)

        val raw = bufferManager.readByte(0).toInt() and 0xFF
        assertEquals(255, raw)
    }
}