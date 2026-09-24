package com.winols.app.export

import com.winols.app.model.MapDefinition
import java.io.File
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.Locale

object ExcelExporter {

    /**
     * Eksportuje listę zdefiniowanych map oraz tylko zmodyfikowane komórki do pliku CSV.
     */
    fun exportToCsv(maps: List<MapDefinition>, targetFile: File, delimiter: Char = ';') {
        targetFile.outputStream().use { stream ->
            exportToCsv(maps, stream, delimiter)
        }
    }

    fun exportToCsv(maps: List<MapDefinition>, outputStream: OutputStream, delimiter: Char = ';') {
        val writer = OutputStreamWriter(outputStream, StandardCharsets.UTF_8)
        // UTF-8 BOM dla poprawnego kodowania polskich znaków w Microsoft Excel
        writer.write("\uFEFF")

        writer.write("sep=$delimiter\n")
        writer.write("=== RAPORT ZMIENIONYCH WARTOŚCI ===\n")
        writer.write(listOf("ID Mapy", "Nazwa Mapy", "Adres Hex", "Wiersz (Y)", "Kolumna (X)", "Wartość Oryginalna", "Wartość Zmieniona", "Różnica", "Jednostka").joinToString(delimiter.toString()) + "\n")

        for (map in maps) {
            for (r in 0 until map.rows) {
                for (c in 0 until map.cols) {
                    val orig = map.originalValues[r][c]
                    val mod = map.modifiedValues[r][c]
                    if (orig != mod) {
                        val diff = mod - orig
                        val rowLine = listOf(
                            escapeCsv(map.id, delimiter),
                            escapeCsv(map.name, delimiter),
                            String.format("0x%06X", map.address),
                            r.toString(),
                            c.toString(),
                            formatDouble(orig),
                            formatDouble(mod),
                            formatDouble(diff),
                            escapeCsv(map.unit, delimiter)
                        ).joinToString(delimiter.toString())
                        writer.write(rowLine + "\n")
                    }
                }
            }
        }

        writer.write("\n=== DEFINICJE I STRUKTURA MAP ===\n")
        for (map in maps) {
            writer.write("\n")
            writer.write("Mapa;$delimiter${escapeCsv(map.name, delimiter)}$delimiter(0x${map.address.toString(16).uppercase(Locale.ROOT)})\n")
            writer.write("Wymiary;$delimiter${map.rows} x ${map.cols}$delimiter Jednostka:;$delimiter${escapeCsv(map.unit, delimiter)}\n")

            // Nagłówki osi X (jeśli zdefiniowana)
            if (map.xAxis?.values?.isNotEmpty() == true) {
                val xHeaders = mutableListOf("Y \\ X")
                xHeaders.addAll(map.xAxis.values.map { formatDouble(it) })
                writer.write(xHeaders.joinToString(delimiter.toString()) + "\n")
            }

            // Dane siatki (wartości zmodyfikowane)
            for (r in 0 until map.rows) {
                val line = mutableListOf<String>()
                val yLabel = if (map.yAxis?.values?.size ?: 0 > r) {
                    formatDouble(map.yAxis!!.values[r])
                } else {
                    "R$r"
                }
                line.add(yLabel)

                for (c in 0 until map.cols) {
                    val value = map.modifiedValues[r][c]
                    line.add(formatDouble(value))
                }
                writer.write(line.joinToString(delimiter.toString()) + "\n")
            }
        }

        writer.flush()
    }

    /**
     * Eksportuje do formatu Microsoft Excel XML Spreadsheet 2003 (.xls).
     * Format ten jest w 100% natywnie otwierany przez aplikację Excel bez dodatkowych bibliotek typu Apache POI.
     */
    fun exportToXmlSpreadsheet(maps: List<MapDefinition>, targetFile: File) {
        targetFile.outputStream().use { stream ->
            exportToXmlSpreadsheet(maps, stream)
        }
    }

    fun exportToXmlSpreadsheet(maps: List<MapDefinition>, outputStream: OutputStream) {
        val writer = OutputStreamWriter(outputStream, StandardCharsets.UTF_8)

        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        writer.write("<?mso-application progid=\"Excel.Sheet\"?>\n")
        writer.write("<Workbook )" writer.write(" xmlns=""urn:schemas-microsoft-com:office:spreadsheet"\n" xmlns:html=""[http://www.w3.org/TR/REC-html40](http://www.w3.org/TR/REC-html40)"" xmlns:o=""urn:schemas-microsoft-com:office:office"\n" xmlns:ss=""urn:schemas-microsoft-com:office:spreadsheet"\n" xmlns:x=""urn:schemas-microsoft-com:office:excel"\n">\n")

        // Definicje stylów
        writer.write("""
          <Styles>
            <Style ss:ID="Default" ss:Name="Normal">
              <Alignment ss:Vertical="Center"/>
              <Font ss:Color="#000000" ss:FontName="Calibri" ss:Size="11"/>
            </Style>
            <Style ss:ID="HeaderStyle">
              <Font ss:Bold="1" ss:Color="#FFFFFF" ss:FontName="Calibri" ss:Size="11"/>
              <Interior ss:Color="#1F4E79" ss:Pattern="Solid"/>
              <Alignment ss:Horizontal="Center" ss:Vertical="Center"/>
            </Style>
            <Style ss:ID="ChangedCell">
              <Interior ss:Color="#FCE4D6" ss:Pattern="Solid"/>
              <Font ss:Bold="1" ss:Color="#C00000" ss:FontName="Calibri" ss:Size="11"/>
            </Style>
            <Style ss:ID="AxisStyle">
              <Interior ss:Color="#D9E1F2" ss:Pattern="Solid"/>
              <Font ss:Bold="1" ss:FontName="Calibri" ss:Size="10"/>
            </Style>
          </Styles>
        """.trimIndent() + "\n")

        // Arkusz 1: Zmodyfikowane zmienne
        writer.write("  <Worksheet ss:Name=""Zmienione_Wartosci"">\n")
        writer.write("    <Table>\n")
        writer.write("      <Row ss:StyleID=""HeaderStyle"">\n")
        listOf("ID Mapy", "Nazwa Mapy", "Adres Hex", "Wiersz", "Kolumna", "Wartość Oryginalna", "Wartość Zmieniona", "Różnica", "Jednostka").forEach {
            writer.write("        <Cell><Data ss:Type=""String"">$it</Data></Cell>\n")
        }
        writer.write("      </Row>\n")

        for (map in maps) {
            for (r in 0 until map.rows) {
                for (c in 0 until map.cols) {
                    val orig = map.originalValues[r][c]
                    val mod = map.modifiedValues[r][c]
                    if (orig != mod) {
                        val diff = mod - orig
                        writer.write("      <Row>\n")
                        writer.write("        <Cell><Data ss:Type=""String"">${xmlEscape(map.id)}</Data></Cell>\n")
                        writer.write("        <Cell><Data ss:Type=""String"">${xmlEscape(map.name)}</Data></Cell>\n")
                        writer.write("        <Cell><Data ss:Type=""String"">0x${map.address.toString(16).uppercase(Locale.ROOT)}</Data></Cell>\n")
                        writer.write("        <Cell><Data ss:Type=""Number"">$r</Data></Cell>\n")
                        writer.write("        <Cell><Data ss:Type=""Number"">$c</Data></Cell>\n")
                        writer.write("        <Cell><Data ss:Type=""Number"">$orig</Data></Cell>\n")
                        writer.write("        <Cell ss:StyleID=""ChangedCell""><Data ss:Type=""Number"">$mod</Data></Cell>\n")
                        writer.write("        <Cell><Data ss:Type=""Number"">$diff</Data></Cell>\n")
                        writer.write("        <Cell><Data ss:Type=""String"">${xmlEscape(map.unit)}</Data></Cell>\n")
                        writer.write("      </Row>\n")
                    }
                }
            }
        }
        writer.write("    </Table>\n")
        writer.write("  </Worksheet>\n")

        // Arkusz 2+: Poszczególne mapy
        for (map in maps) {
            val safeSheetName = xmlEscape(map.name.take(30).replace("[:\\\\/?*\\[\\]]".toRegex(), "_"))
            writer.write("  <Worksheet ss:Name=""$safeSheetName"">\n")
            writer.write("    <Table>\n")

            // Nagłówki osi X
            if (map.xAxis?.values?.isNotEmpty() == true) {
                writer.write("      <Row>\n")
                writer.write("        <Cell ss:StyleID=""AxisStyle""><Data ss:Type=""String"">Y \\ X</Data></Cell>\n")
                for (xVal in map.xAxis.values) {
                    writer.write("        <Cell ss:StyleID=""AxisStyle""><Data ss:Type=""Number"">$xVal</Data></Cell>\n")
                }
                writer.write("      </Row>\n")
            }

            for (r in 0 until map.rows) {
                writer.write("      <Row>\n")
                val yVal = if (map.yAxis?.values?.size ?: 0 > r) map.yAxis!!.values[r].toString() else "R$r"
                writer.write("        <Cell ss:StyleID=""AxisStyle""><Data ss:Type=""String"">$yVal</Data></Cell>\n")

                for (c in 0 until map.cols) {
                    val orig = map.originalValues[r][c]
                    val mod = map.modifiedValues[r][c]
                    val styleAttr = if (orig != mod) " ss:StyleID=\"ChangedCell\"" else ""
                    writer.write("        <Cell$styleAttr><Data ss:Type=""Number"">$mod</Data></Cell>\n")
                }
                writer.write("      </Row>\n")
            }

            writer.write("    </Table>\n")
            writer.write("  </Worksheet>\n")
        }

        writer.write("</Workbook>\n")
        writer.flush()
    }

    private fun formatDouble(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            String.format(Locale.US, "%.3f", value)
        }
    }

    private fun escapeCsv(value: String, delimiter: Char): String {
        return if (value.contains(delimiter) || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun xmlEscape(value: String): String {
        return value.replace("&", "&")
            .replace("<", "<")
            .replace(">", ">")
            .replace("\"", """)
            .replace("'", "'")
    }
}