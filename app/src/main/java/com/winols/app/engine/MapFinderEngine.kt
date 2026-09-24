package com.winols.app.engine

import com.winols.app.data.BinaryBufferManager
import com.winols.app.model.AxisDefinition
import com.winols.app.model.BitDepth
import com.winols.app.model.DataRepresentation
import com.winols.app.model.MapDefinition
import com.winols.app.model.ValueType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteOrder

class MapFinderEngine(private val bufferManager: BinaryBufferManager) {

    suspend fun scanPotentialMaps(): List<MapDefinition> = withContext(Dispatchers.Default) {
        val detected = mutableListOf<MapDefinition>()
        val totalBytes = bufferManager.capacity
        if (totalBytes < 32) return@withContext detected

        var address = 0
        val rep16 = DataRepresentation(BitDepth.BITS_16, ValueType.UNSIGNED, ByteOrder.LITTLE_ENDIAN)

        while (address < totalBytes - 32) {
            val potentialCols = bufferManager.readRawValue(address, rep16).toInt()
            val potentialRows = bufferManager.readRawValue(address + 2, rep16).toInt()

            if (potentialCols in 4..32 && potentialRows in 4..32) {
                val mapSize = potentialCols * potentialRows * 2
                val mapStart = address + 4

                if (mapStart + mapSize <= totalBytes) {
                    if (isMonotonicSequence(mapStart, potentialCols, rep16)) {
                        val xAxis = AxisDefinition(
                            startAddress = mapStart,
                            length = potentialCols,
                            representation = rep16
                        )
                        val actualMapStart = mapStart + (potentialCols * 2)
                        if (actualMapStart + mapSize <= totalBytes) {
                            detected.add(
                                MapDefinition(
                                    id = "MAP_0x${Integer.toHexString(actualMapStart).uppercase()}",
                                    name = "Candidate ${potentialCols}x${potentialRows}",
                                    startAddress = actualMapStart,
                                    rows = potentialRows,
                                    columns = potentialCols,
                                    representation = rep16,
                                    xAxis = xAxis,
                                    confidence = 0.85
                                )
                            )
                            address = actualMapStart + mapSize
                            continue
                        }
                    }
                }
            }
            address += 2
        }
        detected
    }

    private fun isMonotonicSequence(address: Int, length: Int, rep: DataRepresentation): Boolean {
        var lastVal = -1L
        for (i in 0 until length) {
            val v = bufferManager.readRawValue(address + (i * rep.bitDepth.bytesPerElement), rep)
            if (v <= lastVal) return false
            lastVal = v
        }
        return true
    }
}