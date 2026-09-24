package com.winols.app

import com.winols.app.data.BinaryBufferManager
import com.winols.app.model.MapDefinition
import java.io.File

class BinModel(val bufferManager: BinaryBufferManager = BinaryBufferManager()) {
    val maps: MutableList<MapDefinition> = mutableListOf()
    var projectFile: File? = null

    fun loadBinary(file: File) {
        projectFile = file
        bufferManager.loadFromFile(file)
    }

    fun loadBinary(bytes: ByteArray) {
        bufferManager.loadFromBytes(bytes)
    }

    fun addMap(mapDefinition: MapDefinition) {
        maps.add(mapDefinition)
    }

    fun removeMap(mapId: String) {
        maps.removeAll { it.id == mapId }
    }

    fun getMap(mapId: String): MapDefinition? = maps.firstOrNull { it.id == mapId }
}