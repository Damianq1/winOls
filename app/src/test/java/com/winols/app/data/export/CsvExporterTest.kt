package com.winols.app.data.export

import com.winols.app.domain.model.DataType
import com.winols.app.domain.model.EcuMap
import com.winols.app.domain.model.ModifiedVariable
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class CsvExporterTest {

    @Test
    fun testExportFullReport_containsHeadersAndValues() {
        val exporter = CsvExporter(delimiter = ";")
        val outputStream = ByteArrayOutputStream()

        val sampleMaps = listOf(
            EcuMap(
                id = "MAP_01",
                name = "Drivers Wish",
                startAddress = 0x1C0000,
                rows = 16,
                columns = 16,
                dataType = DataType.UINT16_BE,
                factor = 0.1,
                offset = 0.0,
                unit = "Nm",
                description = "Żądanie momentu kierowcy"
            )
        )

        val sampleChanges = listOf(
            ModifiedVariable(
                mapId = "MAP_01",
                mapName = "Drivers Wish",
                address = 0x1C0002,
                originalRawValue = 2500,
                modifiedRawValue = 3000,
                originalPhysicalValue = 250.0,
                modifiedPhysicalValue = 300.0,
                unit = "Nm"
            )
        )

        exporter.exportFullReport(outputStream, sampleMaps, sampleChanges)
        val result = outputStream.toString("UTF-8")

        // Weryfikacja nagłówków i zawartości
        assertTrue(result.contains("=== ZDEFINIOWANE MAPY ECU ==="))
        assertTrue(result.contains("Drivers Wish"))
        assertTrue(result.contains("0x1C0000"))
        assertTrue(result.contains("=== ZMIENIONE WARTOSCI / ZMIENNE ==="))
        assertTrue(result.contains("0x1C0002"))
        assertTrue(result.contains("20.00%"))
    }
}