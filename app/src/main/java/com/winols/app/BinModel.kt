package com.winols.app

import com.winols.app.data.BinaryBufferManager
import com.winols.app.engine.ChecksumBlock
import com.winols.app.engine.ChecksumEngine
import com.winols.app.engine.ChecksumResult
import com.winols.app.engine.MapFinderEngine
import com.winols.app.model.MapDefinition
import java.io.File

class BinModel(val bufferManager: BinaryBufferManager) {

    val maps = mutableListOf<MapDefinition>()
    val checksumBlocks = mutableListOf<ChecksumBlock>()

    private val finderEngine = MapFinderEngine(bufferManager)
    private val checksumEngine = ChecksumEngine(bufferManager)

    fun autoDetectMaps(): List<MapDefinition> {
        val detected = finderEngine.scanForMaps()
        maps.clear()
        maps.addAll(detected)
        return maps
    }

    fun verifyChecksums(): List<ChecksumResult> {
        return checksumBlocks.map { checksumEngine.verifyBlock(it) }
    }

    fun correctAllChecksums() {
        checksumBlocks.forEach { checksumEngine.applyCorrection(it) }
    }

    fun undo(): Boolean = bufferManager.undo()

    fun redo(): Boolean = bufferManager.redo()

    fun exportBinary(targetFile: File) {
        bufferManager.saveToFile(targetFile)
    }

    companion object {
        fun open(file: File): BinModel {
            val buf = BinaryBufferManager.fromFile(file)
            return BinModel(buf)
        }
    }
}