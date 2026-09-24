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
 * Model domenowy spinający bufor binarny, definicje map oraz operacje silnika obliczeniowego.
 */
class BinModel(
    val bufferManager: BinaryBufferManager = BinaryBufferManager()
) {
    val maps: MutableList<MapDefinition> = mutableListOf()
    private val checksumEngine = ChecksumEngine()
    private val mapFinderEngine = MapFinderEngine()

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

    fun readCell(map: MapDefinition, row: Int, col: Int): Double {
        if (row !in 0 until map.rows || col !in 0 until map.columns) return 0.0
        val cell### project_tracker.py