package com.winols.app

import com.winols.app.data.BinaryBufferManager
import com.winols.app.export.ExcelExporter
import com.winols.app.model.DataType
import com.winols.app.model.MapDefinition
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class ExcelExporterTest {

    @Test
    fun testExportMapsToCsv() {
        val testData = ByteArray(128) { 0 }
        testData[0x10] = 0x01
        testData[0x11] = 0x00
        testData[0x12] = 0x02
        testData[0x13] = 0x00

        val bufferManager = BinaryBufferManager(testData)
        val exporter = ExcelExporter(bufferManager)

        val map = MapDefinition(
            id = "map_01",
            name = "Test Map Injection",
            startAddress = 0x10,
            rows = 1,
            columns = 2,
            dataType = DataType.WORD,
            isLittleEndian = true,
            factor = 0.5,
            offset = 10.0
        )

        val out = ByteArrayOutputStream()
        exporter.exportMapsToCsv(listOf(map), out)
        val output = out.toString(Charsets.UTF_8.name())

        assertTrue(output.contains("=== MAPA: Test Map Injection ==="))
        assertTrue(output.contains("10.5;11"))
    }

    @Test
    fun testExportModificationsToCsv() {
        val testData = ByteArray(32) { 0xFF.toByte() }
        val bufferManager = BinaryBufferManager(testData)
        bufferManager.setByte(0x05, 0x0A)

        val exporter = ExcelExporter(bufferManager)
        val map = MapDefinition(
            id = "map_01",
            name = "Limit Map",
            startAddress = 0x00,
            rows = 2,
            columns = 4,
            dataType = DataType.BYTE
        )

        val out = ByteArrayOutputStream()
        exporter.exportModificationsToCsv(listOf(map), out)
        val output = out.toString(Charsets.UTF_8.name())

        assertTrue(output.contains("0x5;5;FF;0A;255;10;-245;Limit Map"))
    }
}