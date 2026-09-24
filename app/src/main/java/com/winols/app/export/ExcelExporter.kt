package com.winols.app.export

import com.winols.app.data.BinaryBufferManager
import com.winols.app.model.MapDefinition
import java.io.File

class ExcelExporter {

    fun exportMapToCsv(
        bufferManager: BinaryBufferManager,
        map: MapDefinition,
        outputFile: File
    ) {
        val sb = StringBuilder()

        sb.append("Map Name:;${map.name}\n")
        sb.append("Address:;0x${java.lang.Long.toHexString(map.startAddress).uppercase()}\n")
        sb.append("Dimensions:;${map.rows}x${map.columns}\n")
        sb.append("Unit:;${map.unit}\n\n")

        map.xAxis?.let { x ->
            sb.append("Y\\X;")
            for (c in 0 until map.columns) {
                val headerVal = x.manualValues?.getOrNull(c) ?: (c + 1).toDouble()
                sb.append("${headerVal * x.factor + x.offset};")
            }
            sb.append("\n")
        }

        var currentAddr = map.startAddress
        val step = map.dataType.byteSize.toLong()

        for (r in 0 until map.rows) {
            val yLabel = map.yAxis?.manualValues?.getOrNull(r) ?: (r + 1).toDouble()
            sb.append("${yLabel};")

            for (c in 0 until map.columns) {
                val rawVal = bufferManager.readValue(currentAddr, map.dataType)
                val engVal = (rawVal * map.factor) + map.offset
                sb.append(String.format(java.util.Locale.US, "%.2f;", engVal))
                currentAddr += step
            }
            sb.append("\n")
        }

        outputFile.writeText(sb.toString(), Charsets.UTF_8)
    }

    fun exportDifferencesToCsv(
        bufferManager: BinaryBufferManager,
        outputFile: File
    ) {
        val diffs = bufferManager.getDifferences()
        val sb = StringBuilder()
        sb.append("Address Hex;Address Dec;Modified (MOD)\n")

        for (addr in diffs) {
            val byteVal = bufferManager.rawBuffer[addr.toInt()].toInt() and 0xFF
            sb.append(String.format(java.util.Locale.US, "0x%08X;%d;0x%02X\n", addr, addr, byteVal))
        }

        outputFile.writeText(sb.toString(), Charsets.UTF_8)
    }
}