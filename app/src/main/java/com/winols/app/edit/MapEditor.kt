package com.winols.app.edit

import com.winols.app.data.BinaryBufferManager
import com.winols.app.model.MapDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MapEditor(private val bufferManager: BinaryBufferManager) {

    suspend fun getMatrix(map: MapDefinition): Array<DoubleArray> = withContext(Dispatchers.Default) {
        val matrix = Array(map.rows) { DoubleArray(map.cols) }
        var currentAddr = map.startAddress

        for (r in 0 until map.rows) {
            for (c in 0 until map.cols) {
                val raw = bufferManager.readRawValue(currentAddr, map.cellFormat)
                matrix[r][c] = map.rawToPhysical(raw)
                currentAddr += map.cellFormat.bytesCount
            }
        }
        matrix
    }

    suspend fun applyBatchEdit(
        map: MapDefinition,
        selectedCells: List<Pair<Int, Int>>,
        operation: (Double) -> Double
    ) = withContext(Dispatchers.Default) {
        bufferManager.snapshotForUndo()

        for ((r, c) in selectedCells) {
            if (r in 0 until map.rows && c in 0 until map.cols) {
                val cellAddress = map.startAddress + ((r * map.cols + c) * map.cellFormat.bytesCount)
                val currentRaw = bufferManager.readRawValue(cellAddress, map.cellFormat)
                val currentPhys = map.rawToPhysical(currentRaw)
                val newPhys = operation(currentPhys)
                val newRaw = map.physicalToRaw(newPhys)
                bufferManager.writeRawValue(cellAddress, map.cellFormat, newRaw)
            }
        }
    }

    suspend fun smoothSelection(
        map: MapDefinition,
        selectedCells: Set<Pair<Int, Int>>,
        intensity: Double = 0.5
    ) = withContext(Dispatchers.Default) {
        bufferManager.snapshotForUndo()
        val currentMatrix = getMatrix(map)
        val smoothed = Array(map.rows) { DoubleArray(map.cols) }

        for (r in 0 until map.rows) {
            for (c in 0 until map.cols) {
                if (selectedCells.contains(Pair(r, c))) {
                    var sum = 0.0
                    var count = 0

                    for (dr in -1..1) {
                        for (dc in -1..1) {
                            val nr = r + dr
                            val nc = c + dc
                            if (nr in 0 until map.rows && nc in 0 until map.cols) {
                                sum += currentMatrix[nr][nc]
                                count++
                            }
                        }
                    }
                    val avg = sum / count
                    smoothed[r][c] = (currentMatrix[r][c] * (1.0 - intensity)) + (avg * intensity)
                } else {
                    smoothed[r][c] = currentMatrix[r][c]
                }
            }
        }

        for ((r, c) in selectedCells) {
            val cellAddress = map.startAddress + ((r * map.cols + c) * map.cellFormat.bytesCount)
            val newRaw = map.physicalToRaw(smoothed[r][c])
            bufferManager.writeRawValue(cellAddress, map.cellFormat, newRaw)
        }
    }
}