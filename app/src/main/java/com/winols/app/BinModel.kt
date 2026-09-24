package com.winols.app

import com.winols.app.data.BinaryBufferManager
import com.winols.app.engine.ChecksumEngine
import com.winols.app.engine.MapFinderEngine
import com.winols.app.model.MapDefinition
import java.io.File

/**
 * Główna fasada reprezentująca wczytany plik binarny, listę map i operacje projektowe.
 */
class BinModel(val file: File) {

    val bufferManager: BinaryBufferManager = BinaryBufferManager(file)
    val mapList: MutableList<MapDefinition> = mutableListOf()

    val fileSize: Int
        get() = bufferManager.size

    /**
     * Wykonuje automatyczne wyszukiwanie map i dodaje je do modelu projektu.
     */
    fun autoDetectMaps(): List<MapDefinition> {
        val finder = MapFinderEngine(bufferManager)
        val detected = finder.scanForMaps()
        mapList.clear()
        mapList.addAll(detected)
        return detected
    }

    /**
     * Zapisuje bieżący stan bufora binarnego do wskazanego pliku.
     */
    fun saveToFile(targetFile: File) {
        targetFile.writeBytes(bufferManager.rawData)
    }

    /**
     * Weryfikuje sumę kontrolną CRC32 całego pliku.
     */
    fun getFullCRC32(): Long {
        return ChecksumEngine.calculateCRC32(bufferManager, 0, bufferManager.size)
    }
}