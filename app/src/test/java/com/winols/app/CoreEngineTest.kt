package com.winols.app

import com.winols.app.data.BinaryBufferManager
import com.winols.app.edit.MapEditor
import com.winols.app.engine.ChecksumEngine
import com.winols.app.model.DataFormat
import com.winols.app.model.MapDefinition
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CoreEngineTest {

    @Test
    fun testBufferReadWrite() {
        val buffer = ByteArray(16)
        val manager = BinaryBufferManager(buffer)

        // Zapis wartości 16-bit LE (0x1234 -> [0x34, 0x12])
        manager.writeRawValue(0, 0x1234, DataFormat.UWORD_LE)
        assertEquals(0x1234L, manager.readRawValue(0, DataFormat.UWORD_LE))
        assertEquals(0x34.toByte(), manager.getRawByte(0))
        assertEquals(0x12.toByte(), manager.getRawByte(1))

        // Test przeliczania fizycznego (wartość * 0.1)
        val phys = manager.readPhysicalValue(0, DataFormat.UWORD_LE, 0.1, 0.0)
        assertEquals(0x1234 * 0.1, phys, 0.0001)
    }

    @Test
    fun testMapEditorMatrixModification() {
        val rawBytes = ByteArray(32)
        val manager = BinaryBufferManager(rawBytes)

        val mapDef = MapDefinition(
            id = "TEST_MAP",
            name = "Test 2x2",
            address = 0,
            columns = 2,
            rows = 2,
            dataFormat = DataFormat.UWORD_LE,
            factor = 1.0,
            offset = 0.0
        )

        val editor = MapEditor(manager, mapDef)
        val input = arrayOf(
            doubleArrayOf(10.0, 20.0),
            doubleArrayOf(30.0, 40.0)
        )

        editor.writePhysicalMatrix(input)
        val output = editor.readPhysicalMatrix()

        assertArrayEquals(input[0], output[0], 0.001)
        assertArrayEquals(input[1], output[1], 0.001)

        // Zwiększenie o 10%
        editor.applyPercentageChange(10.0)
        val modified = editor.readPhysicalMatrix()
        assertEquals(11.0, modified[0][0], 0.001)
        assertEquals(44.0, modified[1][1], 0.001)
    }

    @Test
    fun testChecksumCalculation() {
        val raw = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val manager = BinaryBufferManager(raw)

        val sum16 = ChecksumEngine.calculateSum16(manager, 0, 4)
        assertEquals(10, sum16)
    }
}