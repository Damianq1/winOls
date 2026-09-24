package com.winols.app.export

import com.winols.app.edit.MapEditor
import com.winols.app.model.MapDefinition
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

object ExcelExporter {

    /**
     * Eksportuje mapę do formatu CSV (zgodnego z MS Excel / LibreOffice Calc)
     * z nagłówkami osi X/Y, jednostkami i przeliczonymi wartościami fizycznymi.
     */
    fun exportToCsv(
        editor: MapEditor,
        outputFile: File,
        delimiter: String = ";"
    ) {
        val mapDef = editor.mapDefinition
        val matrix = editor.readPhysicalMatrix()

        FileOutputStream(outputFile).use { fos ->
            // UTF-8 BOM dla poprawnego otwierania znaków diakrytycznych w Excelu
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            OutputStreamWriter(fos, StandardCharsets.UTF_8).buffered().use { writer ->
                // Metadane
                writer.write("# Map: ${mapDef.name}${delimiter}Unit: ${mapDef.unit}${delimiter}Address: 0x${Integer.toHexString(mapDef.startAddress).uppercase()}\n")

                // Nagłówek osi X
                val xHeader = StringBuilder()
                xHeader.append("Y \\ X").append(delimiter)
                for (c in 0 until mapDef.columns) {
                    val colTitle = mapDef.xAxis?.let { "X[$c]" } ?: "Col $c"
                    xHeader.append(colTitle).append(if (c == mapDef.columns - 1) "" else delimiter)
                }
                writer.write(xHeader.toString() + "\n")

                // Wiersze danych
                for (r in 0 until mapDef.rows) {
                    val rowLine = StringBuilder()
                    val rowTitle = mapDef.yAxis?.let { "Y[$r]" } ?: "Row $r"
                    rowLine.append(rowTitle).append(delimiter)

                    for (c in 0 until mapDef.columns) {
                        val value = matrix[r][c]
                        val formattedVal = String.format(java.util.Locale.US, "%.2f", value)
                        rowLine.append(formattedVal).append(if (c == mapDef.columns - 1) "" else delimiter)
                    }
                    writer.write(rowLine.toString() + "\n")
                }
                writer.flush()
            }
        }
    }

    /**
     * Generuje plik XML zgodny ze specyfikacją Excel XML Spreadsheet 2003 (rozszerzenie .xml / .xls).
     */
    fun exportToExcelXml(
        editor: MapEditor,
        outputFile: File
    ) {
        val mapDef = editor.mapDefinition
        val matrix = editor.readPhysicalMatrix()

        outputFile.bufferedWriter(StandardCharsets.UTF_8).use { writer ->
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            writer.write("<?mso-application progid=\"Excel.Sheet\"?>\n")
            writer.write("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"\n")
            writer.write(" xmlns:o=\"urn:schemas-microsoft-com:office:office\"\n")
            writer.write(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\"\n")
            writer.write(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\"\n")
            writer.write(" xmlns:html=\"http://www.w3.org/TR/REC-html40\">\n")
            writer.write(" <Worksheet ss:Name=\"${mapDef.name}\">\n")
            writer.write("  <Table>\n")

            // Nagłówki kolumn
            writer.write("   <Row>\n")
            writer.write("    <Cell><Data ss:Type=\"String\">Y \\ X</Data></Cell>\n")
            for (c in 0 until mapDef.columns) {
                writer.write("    <Cell><Data ss:Type=\"String\">Col $c</Data></Cell>\n")
            }
            writer.write("   </Row>\n")

            // Wiersze z wartościami
            for (r in 0 until mapDef.rows) {
                writer.write("   <Row>\n")
                writer.write("    <Cell><Data ss:Type=\"String\">Row $r</Data></Cell>\n")
                for (c in 0 until mapDef.columns) {
                    val value = matrix[r][c]
                    writer.write("    <Cell><Data ss:Type=\"Number\">$value</Data></Cell>\n")
                }
                writer.write("   </Row>\n")
            }

            writer.write("  </Table>\n")
            writer.write(" </Worksheet>\n")
            writer.write("</Workbook>\n")
            writer.flush()
        }
    }
}