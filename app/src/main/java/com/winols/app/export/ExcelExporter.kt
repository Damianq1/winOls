package com.winols.app.export

import com.winols.app.data.BinaryBufferManager
import com.winols.app.edit.MapEditor
import com.winols.app.model.MapDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

object ExcelExporter {

    suspend fun exportMapToCsv(
        map: MapDefinition,
        bufferManager: BinaryBufferManager,
        outputFile: File
    ) = withContext(Dispatchers.IO) {
        val editor = MapEditor(bufferManager)
        val matrix = editor.getMatrix(map)

        FileOutputStream(outputFile).use { fos ->
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                writer.append("\"Map Name: ${map.name}\"\n")
                writer.append("\"Address: 0x${Integer.toHexString(map.startAddress).uppercase()}\"\n\n")

                for (r in 0 until map.rows) {
                    val rowString = matrix[r].joinToString(separator = ";") { "%.2f".format(it) }
                    writer.append(rowString)
                    writer.append("\n")
                }
                writer.flush()
            }
        }
    }
}