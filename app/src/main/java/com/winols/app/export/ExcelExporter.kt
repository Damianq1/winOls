package com.winols.app.export

import com.winols.app.data.BinaryBufferManager
import com.winols.app.model.MapDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

object ExcelExporter {

    suspend fun exportMapToCsv(
        map: MapDefinition,
        bufferManager: BinaryBufferManager,
        destinationFile: File
    ) = withContext(Dispatchers.IO) {
        BufferedWriter(OutputStreamWriter(FileOutputStream(destinationFile), StandardCharsets.UTF_8)).use { writer ->
            writer.write("Map ID:;${map.id}\n")
            writer.write("Map Name:;${map.name}\n")
            writer.write("Address:;0x${Integer.toHexString(map.startAddress).uppercase()}\n")
            writer.write("Unit:;${map.unit}\n\n")

            // Nagłówek osi X
            writer.write("Y / X;")
            for (c in 0 until map.cols) {
                writer.write("Col $c;")
            }
            writer.write("\n")

            // Wiersze z danymi
            for (r in 0 until map.rows) {
                writer.write("Row $r;")
                for (c in 0 until map.cols) {
                    val addr = map.cellAddress(r, c)
                    val raw = bufferManager.readRaw(addr, map.dataType, map.byteOrder)
                    val physical = map.dataType.rawToPhysical(raw, map.factor, map.offset)
                    writer.write(String.format(java.util.Locale.US, "%.2f;", physical))
                }
                writer.write("\n")
            }
        }
    }
}