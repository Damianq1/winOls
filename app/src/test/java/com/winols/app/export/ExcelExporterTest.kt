package com.winols.app.export

import com.winols.app.model.MapAxis
import com.winols.app.model.MapDefinition
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class ExcelExporterTest {

    @Test
    fun `test exportToCsv outputs changed variables and maps correctly`() {
        val original = Array(2) { DoubleArray(2) { 10.0 } }
        val modified = Array(2) { DoubleArray(2) { 10.0 } }
        modified[0][1] = 25.0 // zmiana komórki (0, 1)

        val map = MapDefinition(
            id = "MAP_001",
            name = "Drivers_Wish",
            address = 0x1C0000,
            rows = 2,
            cols = 2,
            unit = "Nm",
            xAxis = MapAxis(name = "RPM", values = listOf(1000.0, 2000.0)),
            yAxis = MapAxis(name = "Pedal", values = listOf(0.0, 50.0)),
            originalValues = original,
            modifiedValues = modified
        )

        val out = ByteArrayOutputStream()
        ExcelExporter.exportToCsv(listOf(map), out, ';')
        val result = out.toString(Charsets.UTF_8.name())

        assertTrue(result.contains("=== RAPORT ZMIENIONYCH WARTOŚCI ==="))
        assertTrue(result.contains("Drivers_Wish"))
        assertTrue(result.contains("0x1C0000"))
        assertTrue(result.contains("25"))
        assertTrue(result.contains("15")) // różnica (25 - 10)
    }

    @Test
    fun `test exportToXmlSpreadsheet generates valid excel xml`() {
        val original = Array(1) { DoubleArray(1) { 100.0 } }
        val modified = Array(1) { DoubleArray(1) { 150.0 } }

        val map = MapDefinition(
            id = "TURBO_01",
            name = "Boost_Target",
            address = 0x1D5000,
            rows = 1,
            cols = 1,
            unit = "hPa",
            originalValues = original,
            modifiedValues = modified
        )

        val out = ByteArrayOutputStream()
        ExcelExporter.exportToXmlSpreadsheet(listOf(map), out)
        val result = out.toString(Charsets.UTF_8.name())

        assertTrue(result.contains("urn:schemas-microsoft-com:office:spreadsheet"))
        assertTrue(result.contains("ChangedCell"))
        assertTrue(result.contains("Boost_Target"))
        assertTrue(result.contains("150"))
    }
}