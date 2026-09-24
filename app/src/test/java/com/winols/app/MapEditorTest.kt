package com.winols.app

import com.winols.app.data.BinaryBufferManager
import com.winols.app.edit.MapEditor
import com.winols.app.model.MapDefinition
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MapEditorTest {

    private lateinit var bufferManager: BinaryBufferManager
    private lateinit var mapEditor: MapEditor

    @Before
    fun setUp() {
        // Bufor testowy o rozmiarze 256 bajtów
        val initialBytes = ByteArray(256)
        bufferManager = BinaryBufferManager(initialBytes)
        mapEditor = MapEditor(bufferManager)
    }

    @Test
    fun `test apply positive percentage change on 16-bit unsigned cells`() {
        val map = MapDefinition(
            name = "DriversWish",
            startAddress = 0x10,
            rows = 2,
            columns = 2,
            bytesPerCell = 2,
            isSigned = false
        )

        // Ustawienie wartości początkowych: 100, 200, 300, 400
        bufferManager.setShort(0x10, 100.toShort())
        bufferManager.setShort(0x12, 200.toShort())
        bufferManager.setShort(0x14, 300.toShort())
        bufferManager.setShort(0x16, 400.toShort())

        // Zmiana o +10%
        mapEditor.applyPercentageChange(map, 10.0)

        assertEquals(110.toShort(), bufferManager.getShort(0x10))
        assertEquals(220.toShort(), bufferManager.getShort(0x12))
        assertEquals(330.toShort(), bufferManager.getShort(0x14))
        assertEquals(440.toShort(), bufferManager.getShort(0x16))
    }

    @Test
    fun `test apply negative percentage change with clamp to zero on unsigned`() {
        val map = MapDefinition(
            name = "EGR_Map",
            startAddress = 0x00,
            rows = 1,
            columns = 2,
            bytesPerCell = 1,
            isSigned = false
        )

        bufferManager.setByte(0x00, 100.toByte())
        bufferManager.setByte(0x01, 10.toByte())

        // Zmiana o -50%
        mapEditor.applyPercentageChange(map, -50.0)

        assertEquals(50.toByte(), bufferManager.getByte(0x00))
        assertEquals(5.toByte(), bufferManager.getByte(0x01))
    }
}