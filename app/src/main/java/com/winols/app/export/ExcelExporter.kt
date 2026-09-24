package com.winols.app.export

import com.winols.app.data.BinaryBufferManager
import com.winols.app.model.MapDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

class ExcelExporter(private val bufferManager: BinaryBufferManager) {

    suspend fun exportToCsv(mapDef: MapDefinition, destination: File) = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        val bytesPerCell = mapDef.representation.bitDepth.bytesPerElement

        if (mapDef.xAxis != null) {
            sb.append("Y\\X,")
            for (c in 0 until mapDef.columns) {
                val xRaw = bufferManager.readRawValue(
                    mapDef.xAxis.startAddress + (c * mapDef.xAxis.representation.bitDepth.bytesPerElement),
                    mapDef.xAxis.representation
                )
                sb.append(mapDef.xAxis.representation.formatPhysical(mapDef.xAxis.representation.rawToPhysical(xRaw)))
                if (c < mapDef.columns - 1) sb.append(",")
            }
            sb.append("\n")
        }

        for (r in 0 until mapDef.rows) {
            if (mapDef.yAxis != null) {
                val yRaw = bufferManager.readRawValue(
                    mapDef.yAxis.startAddress + (r * mapDef.yAxis.representation.bitDepth.bytesPerElement),
                    mapDef.yAxis.representation
                )
                sb.append(mapDef.yAxis.representation.formatPhysical(mapDef.yAxis.representation.rawToPhysical(yRaw)))
                sb.append(",")
            }

            for (c in 0 until mapDef.columns) {
                val offset = (r * mapDef.columns + c) * bytesPerCell
                val raw = bufferManager.readRawValue(mapDef.startAddress + offset, mapDef.representation)
                val phys = mapDef.representation.rawToPhysical(raw)
                sb.append(mapDef.representation.formatPhysical(phys))
                if (c < mapDef.columns - 1) sb.append(",")
            }
            sb.append("\n")
        }

        FileOutputStream(destination).use { fos ->
            fos.write(sb.toString().toByteArray(StandardCharsets.UTF_8))
        }
    }
}