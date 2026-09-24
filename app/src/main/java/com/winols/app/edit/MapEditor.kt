package com.winols.app.edit

import com.winols.app.data.BinaryBufferManager
import com.winols.app.model.MapDefinition

class MapEditor(private val bufferManager: BinaryBufferManager) {

    fun getPhysicalValues(map: MapDefinition): Array<DoubleArray> {
        val result = Array(map.rows) { DoubleArray(map.columns) }
        var currentAddr = map.startAddress

        for (r in 0 until map.rows) {
            for (c in 0 until map.columns) {
                val rawValue = bufferManager.readValue(currentAddr, map.dataType)
                result[r][c] = rawValue * map.factor + map.offset
                currentAddr += map.dataType.byteSize
            }
        }
        return result
    }

    fun setPhysicalValue(map: MapDefinition, row: Int, col: Int, physicalValue: Double) {
        require(row in 0 until map.rows && col in 0 until map.columns) { "Cell indices out of bounds" }
        val targetAddr = map.startAddress + (row * map.columns + col) * map.dataType.byteSize
        val rawValue = (physicalValue - map.offset) / map.factor
        bufferManager.writeValue(targetAddr, map.dataType, rawValue)
    }

    fun applyPercentageOffset(map: MapDefinition, row: Int, col: Int, percentDelta: Double) {
        val targetAddr = map.startAddress + (row * map.columns + col) * map.dataType.byteSize
        val currentRaw = bufferManager.readValue(targetAddr, map.dataType)
        val currentPhysical = currentRaw * map.factor + map.offset
        val newPhysical = currentPhysical * (1.0 + percentDelta / 100.0)
        val newRaw = (newPhysical - map.offset) / map.factor
        bufferManager.writeValue(targetAddr, map.dataType, newRaw)
    }
}