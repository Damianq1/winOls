package com.winols.app.export

import com.winols.app.data.BinaryBufferManager
import com.winols.app.model.DataRepresentation
import com.winols.app.model.MapDefinition
import com.winols.app.model.PhysicalConversion
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipFile

class ExcelExporterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var bufferManager: BinaryBufferManager
    private lateinit var exporter: ExcelExporter

    @Before
    fun setUp() {
        val rawBytes = ByteArray(1024) { i -> (i % 256).toByte() }
        bufferManager = BinaryBufferManager()
        bufferManager.loadBuffer(rawBytes)
        exporter = ExcelExporter(bufferManager)
    }

    @Test
    fun testExportToCsv_GeneratesValidContent() = runBlocking {
        val map = MapDefinition(
            name = "Torque Limiter",
            startAddress = 0x10,
            rows = 2,
            columns = 3,
            dataRepresentation = DataRepresentation.UINT8,
            conversion = PhysicalConversion(factor = 0.5, offset = 0.0),
            unit = "Nm",
            xAxisValues = doubleArrayOf(1000.0, 2000.0, 3000.0),
            yAxisValues = doubleArrayOf(50.0, 100.0)
        )

        val csvFile = tempFolder.newFile("test_map.csv")
        val result = exporter.exportMap(map, csvFile, ExportConfig(format = ExportFormat.CSV, includeDelta = true))

        assertTrue(result.isSuccess)
        val content = csvFile.readText()
        assertTrue(content.contains("Torque Limiter"))
        assertTrue(content.contains("### MODIFIED / CURRENT DATA ###"))
        assertTrue(content.contains("### ORIGINAL DATA ###"))
        assertTrue(content.contains("### DELTA (MOD - ORI) ###"))
    }

    @Test
    fun testExportToXlsx_GeneratesValidZipArchive() = runBlocking {
        val map = MapDefinition(
            name = "Boost Target",
            startAddress = 0x20,
            rows = 4,
            columns = 4,
            dataRepresentation = DataRepresentation.UINT16_LE,
            conversion = PhysicalConversion(factor = 1.0, offset = 0.0),
            unit = "mbar"
        )

        val xlsxFile = tempFolder.newFile("test_map.xlsx")
        val result = exporter.exportMap(map, xlsxFile, ExportConfig(format = ExportFormat.XLSX, includeDelta = true))

        assertTrue(result.isSuccess)
        assertTrue(xlsxFile.length() > 0)

        // Weryfikacja czy wygenerowany plik to poprawny ZIP ze strukturą OpenXML
        ZipFile(xlsxFile).use { zip ->
            val entryNames = zip.entries().toList().map { it.name }
            assertTrue(entryNames.contains("[Content_Types].xml"))
            assertTrue(entryNames.contains("xl/workbook.xml"))
            assertTrue(entryNames.contains("xl/worksheets/sheet1.xml"))
            assertTrue(entryNames.contains("xl/worksheets/sheet2.xml"))
            assertTrue(entryNames.contains("xl/worksheets/sheet3.xml"))
        }
    }
}