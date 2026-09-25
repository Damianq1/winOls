package com.winols.app.core.engine

import com.winols.app.core.binary.EcuBinaryBuffer
import com.winols.app.core.model.AxisDefinition
import com.winols.app.core.model.EcuMapDefinition

data class ExtractedMapData(
    val definition: EcuMapDefinition,
    val xValues: DoubleArray,
    val yValues: DoubleArray,
    val zValues: Array<DoubleArray> // Siatka mapy [kolumny X x wiersze Y]
)

class MapExtractionEngine {

    fun readAxis(buffer: EcuBinaryBuffer, axis: AxisDefinition): DoubleArray {
        val result = DoubleArray(axis.length)
        var currentOffset = axis.startAddress

        for (i in 0 until axis.length) {
            val raw = buffer.readValue(currentOffset, axis.valueType, axis.endianness)
            result[i] = (raw * axis.factor) + axis.offset
            currentOffset += axis.valueType.byteSize
        }
        return result
    }

    fun extractMap(buffer: EcuBinaryBuffer, def: EcuMapDefinition): ExtractedMapData {
        val xVals = readAxis(buffer, def.xAxis)
        val yVals = readAxis(buffer, def.yAxis)
        val zVals = Array(def.yAxis.length) { DoubleArray(def.xAxis.length) }

        var currentOffset = def.zStartAddress
        for (y in 0 until def.yAxis.length) {
            for (x in 0 until def.xAxis.length) {
                val raw = buffer.readValue(currentOffset, def.valueType, def.endianness)
                zVals[y][x] = (raw * def.factor) + def.offset
                currentOffset += def.valueType.byteSize
            }
        }

        return ExtractedMapData(def, xVals, yVals, zVals)
    }

    fun writeCell(
        buffer: EcuBinaryBuffer,
        def: EcuMapDefinition,
        xIndex: Int,
        yIndex: Int,
        realValue: Double
    ) {
        val rawValue = (realValue - def.offset) / def.factor
        val cellOffset = def.zStartAddress + ((yIndex * def.xAxis.length) + xIndex) * def.valueType.byteSize
        buffer.writeValue(cellOffset, rawValue, def.valueType, def.endianness)
    }
}