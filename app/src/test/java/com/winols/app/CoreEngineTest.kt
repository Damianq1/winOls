package com.winols.app

import com.winols.app.data.BinaryBufferManager
import com.winols.app.edit.MapEditor
import com.winols.app.engine.ChecksumEngine
import com.winols.app.export.ExcelExporter
import com.winols.app.model.DataType
import com.winols.app.model.MapDefinition
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class CoreEngineTest {

    private lateinit var buffer: ByteArray
    private lateinit var bufferManager: BinaryBufferManager

    @BeforeEach
    fun setUp() {
        buffer = ByteArray(512) { 0 }
        bufferManager = BinaryBufferManager(buffer)
    }

    @Test
    fun `test checksum calculate verify and patch`() {
        // Blok danych 0x10 do 0x13: 4 bajty [0x10, 0x20, 0x30, 0x40]
        buffer[0x10] = 0x10.toByte()
        buffer[0x11] = 0x20.toByte()
        buffer[0x12] = 0x30.toByte()
        buffer[0x13] = 0x40.toByte()

        val block = ChecksumEngine.ChecksumBlock(
            id = "CHK_1",
            description = "Main Add16 Checksum",
            startAddress = 0x10,
            endAddress = 0x13,
            checksumAddress = 0x20,
            algorithm = ChecksumEngine.Algorithm.ADD16_LE
        )

        val engine = ChecksumEngine(bufferManager)

        // Suma 16-bit: (0x2010 + 0x4030) = 0x6040 (24640)
        val calculated = engine.calculate(block)
        assertEquals(0x6040L, calculated)

        // Weryfikacja przed spatchowaniem powinna zwrócić false
        assertFalse(engine.verify(block))

        // Spatchowanie sumy kontrolnej w buforze
        engine.patch(block)
        assertTrue(engine.verify(block))

        val writtenLow = bufferManager.readRawValue(0x20, 1)
        val writtenHigh = bufferManager.readRawValue(0x21, 1)
        assertEquals(0x40L, writtenLow)
        assertEquals(0x60L, writtenHigh)
    }

    @Test
    fun `test excel csv export`() {
        val map = MapDefinition(
            id = "map_export_test",
            name = "TestMap",
            unit = "mg",
            startAddress = 0x30,
            rows = 2,
            columns = 2,
            dataType = DataType.UINT8,
            factor = 1.0,
            additionOffset = 0.0
        )
        val editor = MapEditor(bufferManager, map)
        editor.writePhysicalValue(0, 0, 10.0)
        editor.writePhysicalValue(0, 1, 20.0)
        editor.writePhysicalValue(1, 0, 30.0)
        editor.writePhysicalValue(1, 1, 40.0)

        val tempFile = File.createTempFile("map_export", ".csv")
        tempFile.deleteOnExit()

        ExcelExporter.exportToCsv(editor, tempFile)

        val lines = tempFile.readLines()
        assertTrue(lines.isNotEmpty())
        assertTrue(lines.any { it.contains("TestMap") })
        assertTrue(lines.any { it.contains("10.00") && it.contains("20.00") })
        assertTrue(lines.any { it.contains("30.00") && it.contains("40.00") })
    }
}