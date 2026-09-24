package com.winols.app

import com.winols.app.data.BinaryBufferManager
import com.winols.app.edit.MapEditor
import com.winols.app.engine.ChecksumEngine
import com.winols.app.model.DataType
import com.winols.app.model.MapDefinition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreEngineTest {

    @Test
    fun testBufferReadWrite() {
        val manager = BinaryBufferManager(64)
        manager.writeValue(0x10, DataType.UWORD_BE, 1250.0)
        val readVal = manager.readValue(0x10, DataType.UWORD_BE)
        assertEquals(1250.0, readVal, 0.001)
    }

    @Test
    fun testMapEditorScaling() {
        val manager = BinaryBufferManager(128)
        val map = MapDefinition(
            id = "MAP_1",
            name = "Test Map",
            startAddress = 0,
            rows = 2,
            columns = 2,
            dataType = DataType.UWORD_BE,
            factor = 0.5,
            offset = 10.0
        )
        val editor = MapEditor(manager)
        editor.setPhysicalValue(map, 0, 0, 100.0) // Raw: (100 - 10) / 0.5 = 180

        val physicalValues = editor.getPhysicalValues(map)
        assertEquals(100.0, physicalValues[0][0], 0.001)
    }

    @Test
    fun testChecksumCalculation() {
        val manager = BinaryBufferManager(16)
        val engine = ChecksumEngine(manager)
        val crc = engine.calculateCRC32(0, 16)
        assertTrue(crc != 0L)
    }
}