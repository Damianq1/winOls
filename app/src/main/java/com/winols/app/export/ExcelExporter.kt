package com.winols.app.export

import com.winols.app.edit.MapEditor
import com.winols.app.model.MapDefinition
import java.io.File
import java.io.FileWriter

class ExcelExporter(private val mapEditor: MapEditor) {

    fun exportToCsv(map: MapDefinition, destination: File) {
        val data = mapEditor.getPhysicalValues(map)
        FileWriter(destination).use { writer ->
            writer.append("# Map: ${map.name} (${map.rows}x${map.columns})\n")
            for (r in 0 until map.rows) {
                val rowStr = data[r].joinToString(separator = ";") { String.format("%.2f", it) }
                writer.append(rowStr).append("\n")
            }
        }
    }
}