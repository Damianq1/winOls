package com.winols.app

import com.winols.app.data.BinaryBufferManager
import com.winols.app.engine.ChecksumEngine
import com.winols.app.engine.MapFinderEngine
import com.winols.app.model.MapDefinition
import java.io.File
import java.io.IOException

/**
 * Główna reprezentacja pliku binarnego ECU (ROM/Flash).
 * Odpowiada za koordynację bufora pamięci, zarządzanie projektami i operacje domenowe.
 */
class BinModel private constructor(
    private var sourceFile: File?,
    val bufferManager: BinaryBufferManager
) {
    val checksumEngine: ChecksumEngine = ChecksumEngine(bufferManager)
    val mapFinder: MapFinderEngine = MapFinderEngine(bufferManager)

    private val _maps: MutableList<MapDefinition> = mutableListOf()
    val maps: List<MapDefinition> get() = _maps.toList()

    var projectName: String = sourceFile?.nameWithoutExtension ?: "Nowy_Projekt"
    val fileSize: Int get() = bufferManager.size

    /**
     * Uruchamia silnik heurystycznego wyszukiwania map i indeksuje znalezione struktury.
     */
    fun scanAndRegisterMaps(minDim: Int = 4, maxDim: Int = 20): Int {
        val detected = mapFinder.scanPotentialMaps(minDim, maxDim)
        _maps.clear()
        _maps.addAll(detected)
        return _maps.size
    }

    /**
     * Ręczne dodanie zidentyfikowanej definicji mapy do projektu (np. z plików *.kp / *.dam).
     */
    fun registerMap(mapDefinition: MapDefinition) {
        if (!_maps.any { it.id == mapDefinition.id || it.startAddress == mapDefinition.startAddress }) {
            _maps.add(mapDefinition)
        }
    }

    /**
     * Usuwa definicję mapy z projektu.
     */
    fun removeMap(mapId: String): Boolean {
        return _maps.removeIf { it.id == mapId }
    }

    /**
     * Weryfikuje spójność sumy kontrolnej dla zarejestrowanego bloku.
     */
    fun verifyChecksum(block: ChecksumEngine.ChecksumBlock): Boolean {
        return checksumEngine.verify(block)
    }

    /**
     * Przelicza i koryguje w buforze sumę kontrolną danego bloku.
     */
    fun applyChecksumCorrection(block: ChecksumEngine.ChecksumBlock) {
        checksumEngine.recalculateAndFix(block)
    }

    /**
     * Zapisuje obecny stan bufora na dysk.
     */
    @Throws(IOException::class)
    fun save(targetFile: File? = sourceFile) {
        val destination = targetFile ?: throw IllegalArgumentException("Nie określono pliku docelowego")
        bufferManager.saveToFile(destination)
        this.sourceFile = destination
    }

    /**
     * Tworzy binarny zrzut diff (patch) względem oryginalnego stanu lub innego bufora.
     */
    fun exportPatch(originalBytes: ByteArray): Map<Int, Byte> {
        val currentBytes = bufferManager.getByteArray()
        val patch = mutableMapOf<Int, Byte>()
        val minSize = minOf(originalBytes.size, currentBytes.size)

        for (i in 0 until minSize) {
            if (originalBytes[i] != currentBytes[i]) {
                patch[i] = currentBytes[i]
            }
        }
        return patch
    }

    companion object {
        fun createNew(sizeInBytes: Int = 512 * 1024): BinModel {
            val emptyBuffer = ByteArray(sizeInBytes)
            return BinModel(null, BinaryBufferManager.fromByteArray(emptyBuffer))
        }

        fun fromFile(file: File): BinModel {
            require(file.exists() && file.isFile) { "Wskazany plik nie istnieje: ${file.absolutePath}" }
            val manager = BinaryBufferManager.fromFile(file)
            return BinModel(file, manager)
        }
    }
}