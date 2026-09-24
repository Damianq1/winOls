package com.winols.app

import com.winols.app.data.BinaryBufferManager
import com.winols.app.edit.MapEditor
import com.winols.app.engine.ChecksumEngine
import com.winols.app.engine.MapFinderEngine
import com.winols.app.model.MapDefinition
import java.io.File

class BinModel {
    val bufferManager = BinaryBufferManager()
    val editor = MapEditor(bufferManager)
    val mapFinder = MapFinderEngine(bufferManager)
    val checksumEngine = ChecksumEngine(bufferManager)

    val registeredMaps = mutableListOf<MapDefinition>()

    fun loadBin(file: File) {
        bufferManager.loadFromFile(file)
    }

    fun registerMap(definition: MapDefinition) {
        registeredMaps.removeAll { it.id == definition.id }
        registeredMaps.add(definition)
    }

    fun saveBin(destination: File) {
        destination.writeBytes(bufferManager.toByteArray())
    }
}