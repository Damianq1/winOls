package com.winols.app.engine

import com.winols.app.model.AxisDefinition
import com.winols.app.model.DataType
import com.winols.app.model.MapDefinition
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MapFinderEngine {

    /**
     * Wyszukuje charakterystyczne nagłówki osi (np. Bosch 16-bit: identyfikator osi, długość, punkty pracy).
     */
    fun scanForPotentialMaps(buffer: ByteArray, minRows: Int = 4, maxRows: Int = 32, minCols: Int = 4, maxCols: Int = 32): List<MapDefinition> {
        val detectedMaps = mutableListOf<MapDefinition>()
        val maxOffset = buffer.size - 64

        var i = 0
        while (i < maxOffset) {
            val rows = buffer[i].toInt() and 0xFF
            val cols = buffer[i + 1].toInt() and 0xFF

            if (rows in minRows..maxRows && cols in minCols..maxCols) {
                val dataSize = rows * cols * 2
                val mapStart = i + 2

                if (mapStart + dataSize <= buffer.size && isDataPlausible(buffer, mapStart, rows, cols)) {
                    val mapDef = MapDefinition(
                        id = "MAP_${Integer.toHexString(mapStart).uppercase()}",
                        name = "Potential Map ${rows}x${cols} @ 0x${Integer.toHexString(mapStart).uppercase()}",
                        startAddress = mapStart.toLong(),
                        rows = rows,
                        columns = cols,
                        dataType = DataType.UWORD_LE,
                        xAxis = AxisDefinition("X-Axis", length = cols),
                        yAxis = AxisDefinition("Y-Axis", length = rows)
                    )
                    detectedMaps.add(mapDef)
                    i += dataSize
                }
            }
            i += 2
        }
        return detectedMaps
    }

    private fun isDataPlausible(buffer: ByteArray, start: Int, rows: Int, cols: Int): Boolean {
        var nonZeroCount = 0
        var monotonicTrends = 0
        val totalElements = rows * cols
        var previousVal = -1

        val bb = ByteBuffer.wrap(buffer, start, totalElements * 2).order(ByteOrder.LITTLE_ENDIAN)

        for (idx in 0 until totalElements) {
            val value = bb.short.toInt() and 0xFFFF
            if (value > 0) nonZeroCount++
            if (previousVal != -1 && value >= previousVal) {
                monotonicTrends++
            }
            previousVal = value
        }

        val nonZeroRatio = nonZeroCount.toDouble() / totalElements
        val monotonicRatio = monotonicTrends.toDouble() / totalElements

        return nonZeroRatio > 0.40 && (monotonicRatio > 0.35 || nonZeroRatio > 0.85)
    }
}