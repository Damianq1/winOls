package com.winols.app.edit

import com.winols.app.data.BinaryBufferManager
import com.winols.app.model.DataType
import com.winols.app.model.MapDefinition

class MapEditor(private val bufferManager: BinaryBufferManager) {

    fun readRawValue(map: MapDefinition, row: Int, col: Int): Double {
        val cellOffset = map.startOffset + ((row * map.columns) + col) * map.dataType.byteSize
        return when (map.dataType) {
            DataType.UBYTE -> bufferManager.getUByte(cellOffset).toDouble()
            DataType.SBYTE -> bufferManager.getByte(cellOffset).toDouble()
            DataType.USHORT -> bufferManager.getUShort(cellOffset, map.byteOrder).toDouble()
            DataType.SSHORT -> bufferManager.getShort(cellOffset, map.byteOrder).toDouble()
            DataType.UINT -> (bufferManager.getInt(cellOffset, map.byteOrder).toLong() and 0xFFFFFFFFL).toDouble()
            DataType.SINT -> bufferManager.getInt(cellOffset, map.byteOrder).toDouble()
            DataType.FLOAT -> java.lang.Float.intBitsToFloat(bufferManager.getInt(cellOffset, map.byteOrder)).toDouble()
        }
    }

    fun readPhysicalValue(map: MapDefinition, row: Int, col: Int): Double {
        val raw = readRawValue(map, row, col)
        return raw * map.factor + map.offset
    }

    fun writePhysicalValue(map: MapDefinition, row: Int, col: Int, physicalValue: Double) {
        val raw = (physicalValue - map.offset) / map.factor
        val cellOffset = map.startOffset + ((row * map.columns) + col) * map.dataType.byteSize

        when (map.dataType) {
            DataType.UBYTE, DataType.SBYTE -> {
                bufferManager.setByte(cellOffset, raw.toInt().toByte())
            }
            DataType.USHORT, DataType.SSHORT -> {
                bufferManager.setShort(cellOffset, raw.toInt().toShort(), map.byteOrder)
            }
            DataType.UINT, DataType.SINT -> {
                bufferManager.setInt(cellOffset, raw.toInt(), map.byteOrder)
            }
            DataType.FLOAT -> {
                val bits = java.lang.Float.floatToIntBits(raw.toFloat())
                bufferManager.setInt(cellOffset, bits, map.byteOrder)
            }
        }
    }

    fun readMatrix(map: MapDefinition): Array<DoubleArray> {
        return Array(map.rows) { r ->
            DoubleArray(map.columns) { c ->
                readPhysicalValue(map, r, c)
            }
        }
    }

    fun applyDeltaPercentage(map: MapDefinition, row: Int, col: Int, percentage: Double) {
        val current = readPhysicalValue(map, row, col)
        val updated = current * (1.0 + (percentage / 100.0))
        writePhysicalValue(map, row, col, updated)
    }
}