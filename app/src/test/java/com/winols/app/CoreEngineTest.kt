package com.winols.app

import com.winols.app.engine.ChecksumAlgorithm
import com.winols.app.engine.ChecksumBlock
import com.winols.app.engine.ChecksumEngine
import com.winols.app.export.ExcelExporter
import com.winols.app.export.ExportOptions
import com.winols.app.model.DataRepresentation
import com.winols.app.model.MapDefinition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.zip.ZipInputStream

class CoreEngineTest {

    @Test
    fun testChecksumAdd8CalculationAndCorrection() {
        val engine = ChecksumEngine()
        val buffer = ByteBuffer.allocate(16)
        
        // Dane testowe: 0x01, 0x02, 0x03, 0x04
        buffer.put(0, 0x01.toByte())
        buffer.put(1, 0x02.toByte())
        buffer.put(2, 0x03.toByte())
        buffer.put(3, 0x04.toByte())
        // Adres 4 przeznaczony na zapisaną sumę

        val block = ChecksumBlock(
            id = "B1",
            name = "Test Block 8-bit",
            startAddress = 0,
            endAddress = 3,
            storedChecksumAddress = 4,
            algorithm = ChecksumAlgorithm.ADD8
        )

        // 1 + 2 + 3 + 4 = 10 (0x0A)
        val initialVerify = engine.verifyBlock(buffer, block)
        assertEquals(10L, initialVerify.calculatedChecksum)
        assertEquals(false, initialVerify.isValid) // pod adresem 4 jest 0x00

        // Zastosowanie poprawki
        val success = engine.applyCorrection(buffer, block)
        assertTrue(success)

        val updatedVerify = engine.verifyBlock(buffer, block)
        assertEquals(10L, updatedVerify.storedChecksum)
        assertTrue(updatedVerify.isValid)
    }

    @Test
    fun testExcelExporterGeneratesValidXlsxZipStructure() {
        val exporter = ExcelExporter(ExportOptions())
        val buffer = ByteBuffer.allocate(64)
        for (i in 0 until 16) {
            buffer.put(i, (i * 10).toByte())
        }

        val map = MapDefinition(
            id = "MAP_1",
            name = "DriversWish",
            address = 0,
            rows = 4,
            cols = 4,
            cellType = DataRepresentation.UINT8,
            factor = 1.0,
            offset = 0.0,
            unit = "Nm",
            xAxis = doubleArrayOf(1000.0, 2000.0, 3000.0, 4000.0),
            yAxis = doubleArrayOf(0.0, 25.0, 50.0, 100.0)
        )

        val out = ByteArrayOutputStream()
        exporter.exportMapsToXlsx(buffer, listOf(map), out)
        val bytes = out.toByteArray()

        assertTrue(bytes.isNotEmpty())

        // Weryfikacja struktury ZIP OpenXML
        val zipIn = ZipInputStream(bytes.inputStream())
        val entries = mutableListOf<String>()
        var entry = zipIn.nextEntry
        while (entry != null) {
            entries.add(entry.name)
            entry = zipIn.nextEntry
        }

        assertTrue(entries.contains("[Content_Types].xml"))
        assertTrue(entries.contains("xl/workbook.xml"))
        assertTrue(entries.contains("xl/worksheets/sheet1.xml"))
    }
}