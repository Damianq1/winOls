package com.winols.app.export

import com.winols.app.data.BinaryBufferManager
import com.winols.app.model.DataRepresentation
import com.winols.app.model.MapDefinition
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
        // Bufor 1 KB z przykładowymi danymi 16-bit
        val rawBytes = ByteArray(1024)
        for (i in 0 until 1024 step 2) {
            val val16 = (i / 2) and 0xFFFF
            rawBytes[i] = (val16 and 0xFF).toByte()
            rawBytes[i + 1] = ((val16 shr 8) and 0xFF).toByte()
        }

        bufferManager = BinaryBufferManager()
        bufferManager.loadBytes(rawBytes)
        exporter = ExcelExporter(bufferManager)
    }

    @Test
    fun testExportToCsv_ContainsDifferenceAndHeaders() = runBlocking {
        val mapDef = MapDefinition(
            name = "Drivers_Wish",
            startAddress = 0x0010,
            rows = 2,
            cols = 2,
            representation = DataRepresentation.UINT16_LE,
            factor = 0.1,
            offset = 0.0,
            xAxisValues = listOf(1000.0, 2000.0),
            yAxisValues = listOf(10.0, 20.0)
        )

        // Zmodyfikuj komórkę [0, 0] w buforze edycyjnym
        bufferManager.writeMapCell(mapDef, 0, 0, 100)

        val csvFile = tempFolder.newFile("test_map.csv")
        exporter.exportMap(mapDef, csvFile, ExportOptions(format = ExportFormat.CSV))

        val content = csvFile.readText()
        assertTrue("Musi zawierać nagłówek mapy", content.contains("Drivers_Wish"))
        assertTrue("Musi zawierać sekcję zmodyfikowaną", content.contains("--- MAPA ZMODYFIKOWANA (CURRENT) ---"))
        assertTrue("Musi zawierać sekcję oryginalną", content.contains("--- MAPA ORYGINALNA (ORIGINAL) ---"))
        assertTrue("Musi zawierać tabelę delta", content.contains("--- RÓŻNICA DELTA (MOD - ORI) ---"))
    }

    @Test
    fun testExportToXlsx_GeneratesValidZipPackage() = runBlocking {
        val mapDef = MapDefinition(
            name = "Turbo_Boost",
            startAddress = 0x0020,
            rows = 3,
            cols = 3,
            representation = DataRepresentation.UINT16_LE,
            factor = 1.0,
            offset = 0.0
        )

        val xlsxFile = tempFolder.newFile("test_map.xlsx")
        exporter.exportMap(mapDef, xlsxFile, ExportOptions(format = ExportFormat.XLSX))

        assertTrue("Plik XLSX musi istnieć i mieć rozmiar > 0", xlsxFile.length() > 0)

        // Weryfikacja struktury kontenera OpenXML
        ZipFile(xlsxFile).use { zip ->
            val contentTypes = zip.getEntry("[Content_Types].xml")
            val workbook = zip.getEntry("xl/workbook.xml")
            val sheet = zip.getEntry("xl/worksheets/sheet1.xml")
            val styles = zip.getEntry("xl/styles.xml")

            assertTrue("Musi zawierać [Content_Types].xml", contentTypes != null)
            assertTrue("Musi zawierać xl/workbook.xml", workbook != null)
            assertTrue("Musi zawierać xl/worksheets/sheet1.xml", sheet != null)
            assertTrue("Musi zawierać xl/styles.xml", styles != null)
        }
    }
}