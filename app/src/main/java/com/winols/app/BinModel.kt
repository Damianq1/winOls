package com.winols.app

import com.winols.app.data.BinaryBufferManager
import com.winols.app.engine.ChecksumEngine
import com.winols.app.engine.ChecksumFamily
import com.winols.app.engine.ChecksumResult
import com.winols.app.engine.MapFinderEngine
import com.winols.app.model.DataType
import com.winols.app.model.MapDefinition
import java.io.File

/**
 * Główny kontener stanu otwartego projektu binarnego ECU.
 */
class BinModel(
    val bufferManager: BinaryBufferManager = BinaryBufferManager()
) {
    val maps: MutableList<MapDefinition> = mutableListOf()
    val checksumEngine = ChecksumEngine()
    val mapFinderEngine = MapFinderEngine()

    fun loadBin(file: File) {
        bufferManager.loadFromFile(file)
        detectMaps()
    }

    fun loadBytes(bytes: ByteArray) {
        bufferManager.loadFromBytes(bytes)
        detectMaps()
    }

    fun detectMaps() {
        maps.clear()
        if (bufferManager.size > 0) {
            val detected = mapFinderEngine.scanForPotentialMaps(bufferManager.rawBuffer)
            maps.addAll(detected)
        }
    }

    fun getCellValue(map: MapDefinition, row: Int, col: Int): Double {
        if (row !in 0 until map.rows || col !in 0 until map.columns) return 0.0
        val cellOffset = (row * map.columns + col) * map.dataType.byteSize
        val targetAddr = map.startAddress + cellOffset
        val raw = bufferManager.readValue(targetAddr, map.dataType)
        return (raw * map.factor) + map.offset
    }

    fun setCellValue(map: MapDefinition, row: Int, col: Int, engineeringValue: Double) {
        if (row !in 0 until map.rows || col !in 0 until map.columns) return
        val cellOffset = (row * map.columns + col) * map.dataType.byteSize
        val targetAddr = map.startAddress + cellOffset
        val raw = if (map.factor != 0.0) {
            (engineeringValue - map.offset) / map.factor
        } else {
            0.0
        }
        bufferManager.writeValue(targetAddr, map.dataType, raw)
    }

    fun applyPercentageChange(map: MapDefinition, row: Int, col: Int, percentDelta: Double) {
        val currentVal = getCellValue(map, row, col)
        val newVal = currentVal * (1.0 + (percentDelta / 100.0))
        setCellValue(map, row, col, newVal)
    }

    fun verifyChecksum(start: Int, end: Int, loc: Int, family: ChecksumFamily): ChecksumResult {
        return checksumEngine.verifyBlock(bufferManager.rawBuffer, start, end, loc, family)
    }

    fun correctChecksum(start: Int, end: Int, loc: Int, family: ChecksumFamily): Boolean {
        return checksumEngine.patchChecksum(bufferManager.rawBuffer, start, end, loc, family)
    }
}