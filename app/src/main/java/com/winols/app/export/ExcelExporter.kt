package com.winols.app.export

import com.winols.app.edit.MapEditor
import com.winols.app.model.MapDefinition
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.Locale

class ExcelExporter {

    /**
     * Eksportuje pojedynczą mapę do formatu CSV z uwzględnieniem osi X i Y,
     * metadanych oraz separatora (domyślnie średnik dla polskiej/europejskiej wersji Excela).
     */
    fun exportMapToCsv(editor: MapEditor, outputStream: OutputStream, delimiter: String = ";") {
        val mapDef = editor.mapDef
        val writer = outputStream.bufferedWriter(StandardCharsets.UTF_8)

        val xAxisValues = editor.getAxisPhysicalValues(isXAxis = true)
        val yAxisValues = editor.getAxisPhysicalValues(isXAxis = false)

        writer.write("# Map: ${mapDef.name}")
        writer.newLine()
        writer.write("# ID: ${mapDef.id}")
        writer.newLine()
        writer.write("# Address: 0x${mapDef.dataAddress.toString(16).uppercase()}")
        writer.newLine()
        writer.write("# Dimensions: ${mapDef.rows}x${mapDef.cols}")
        writer.newLine()
        writer.write("# Unit: ${mapDef.unit.ifBlank { "raw" }}")
        writer.newLine()
        writer.newLine()

        val xUnit = mapDef.xAxis?.unit?.takeIf { it.isNotBlank() }?.let { " [$it]" } ?: ""
        val yUnit = mapDef.yAxis?.unit?.takeIf { it.isNotBlank() }?.let { " [$it]" } ?: ""
        val headerCorner = "${mapDef.yAxis?.name ?: "Y"}$yUnit \\ ${mapDef.xAxis?.name ?: "X"}$xUnit"

        writer.write(escapeCsvField(headerCorner, delimiter))

        // Nagłówek osi X
        for (col in 0 until mapDef.cols) {
            writer.write(delimiter)
            val xVal = if (col < xAxisValues.size) xAxisValues[col] else col.toDouble()
            writer.write(formatNumber(xVal, mapDef.xAxis?.formula?.precision ?: 2))
        }
        writer.newLine()

        // Wiersze osi Y oraz wartości komórek
        for (row in 0 until mapDef.rows) {
            val yVal = if (row < yAxisValues.size) yAxisValues[row] else row.toDouble()
            writer.write(formatNumber(yVal, mapDef.yAxis?.formula?.precision ?: 2))

            for (col in 0 until mapDef.cols) {
                writer.write(delimiter)
                val cellVal = editor.getPhysicalValue(row, col)
                writer.write(formatNumber(cellVal, mapDef.formula.precision))
            }
            writer.newLine()
        }

        writer.flush()
    }

    fun exportMapToCsv(editor: MapEditor, targetFile: File, delimiter: String = ";") {
        FileOutputStream(targetFile).use { fos ->
            exportMapToCsv(editor, fos, delimiter)
        }
    }

    /**
     * Eksportuje raport zmodyfikowanych komórek dla podanej mapy (wartość oryginalna, zmodyfikowana, delta).
     */
    fun exportDifferencesToCsv(editor: MapEditor, outputStream: OutputStream, delimiter: String = ";") {
        val mapDef = editor.mapDef
        val writer = outputStream.bufferedWriter(StandardCharsets.UTF_8)

        writer.write("# Differences Report: ${mapDef.name} (0x${mapDef.dataAddress.toString(16).uppercase()})")
        writer.newLine()
        writer.write("Row${delimiter}Col${delimiter}Original${delimiter}Modified${delimiter}Delta${delimiter}DeltaPercent")
        writer.newLine()

        for (r in 0 until mapDef.rows) {
            for (c in 0 until mapDef.cols) {
                if (editor.isCellModified(r, c)) {
                    val orig = editor.getOriginalPhysicalValue(r, c)
                    val curr = editor.getPhysicalValue(r, c)
                    val delta = editor.getPhysicalDelta(r, c)
                    val pct = editor.getPercentageDelta(r, c)

                    writer.write("$r$delimiter$c$delimiter")
                    writer.write(formatNumber(orig, mapDef.formula.precision))
                    writer.write(delimiter)
                    writer.write(formatNumber(curr, mapDef.formula.precision))
                    writer.write(delimiter)
                    writer.write(formatNumber(delta, mapDef.formula.precision))
                    writer.write(delimiter)
                    writer.write(String.format(Locale.US, "%.2f%%", pct))
                    writer.newLine()
                }
            }
        }
        writer.flush()
    }

    /**
     * Eksportuje pełny spis zdefiniowanych map i ich metadanych do formatu tabeli CSV.
     */
    fun exportMapListToCsv(maps: List<MapDefinition>, outputStream: OutputStream, delimiter: String = ";") {
        val writer = outputStream.bufferedWriter(StandardCharsets.UTF_8)
        writer.write("ID${delimiter}Name${delimiter}Category${delimiter}AddressHex${delimiter}Rows${delimiter}Cols${delimiter}DataType${delimiter}Unit${delimiter}SizeBytes")
        writer.newLine()

        for (m in maps) {
            val addrHex = "0x" + m.dataAddress.toString(16).uppercase()
            writer.write(escapeCsvField(m.id, delimiter))
            writer.write(delimiter)
            writer.write(escapeCsvField(m.name, delimiter))
            writer.write(delimiter)
            writer.write(escapeCsvField(m.category, delimiter))
            writer.write(delimiter)
            writer.write(addrHex)
            writer.write(delimiter)
            writer.write("${m.rows}$delimiter${m.cols}$delimiter${m.dataType.name}$delimiter")
            writer.write(escapeCsvField(m.unit, delimiter))
            writer.write(delimiter)
            writer.write(m.totalBytes.toString())
            writer.newLine()
        }
        writer.flush()
    }

    /**
     * Eksportuje raport wszystkich zmodyfikowanych bajtów w całym wsadzie binarnym.
     */
    fun exportGlobalByteDifferencesToCsv(
        originalBytes: ByteArray,
        modifiedBytes: ByteArray,
        outputStream: OutputStream,
        delimiter: String = ";"
    ) {
        val writer = outputStream.bufferedWriter(StandardCharsets.UTF_8)
        writer.write("OffsetHex${delimiter}OffsetDec${delimiter}OriginalHex${delimiter}ModifiedHex${delimiter}OriginalDec${delimiter}ModifiedDec")
        writer.newLine()

        val length = minOf(originalBytes.size, modifiedBytes.size)
        for (i in 0 until length) {
            val bOrig = originalBytes[i].toInt() and 0xFF
            val bMod = modifiedBytes[i].toInt() and 0xFF
            if (bOrig != bMod) {
                val hexAddr = "0x" + i.toString(16).uppercase().padStart(6, '0')
                val hexOrig = "%02X".format(bOrig)
                val hexMod = "%02X".format(bMod)
                writer.write("$hexAddr$delimiter$i$delimiter$hexOrig$delimiter$hexMod$delimiter$bOrig$delimiter$bMod")
                writer.newLine()
            }
        }
        writer.flush()
    }

    private fun formatNumber(value: Double, precision: Int): String {
        return String.format(Locale.US, "%.${precision}f", value)
    }

    private fun escapeCsvField(field: String, delimiter: String): String {
        return if (field.contains(delimiter) || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }
    }
}