package com.winols.app.engine

import com.winols.app.data.BinaryBufferManager
import com.winols.app.model.DataFormat
import com.winols.app.model.MapDefinition

/**
 * Heurystyczny silnik skanujący bufor pod kątem typowych struktur map Bosch / Siemens / Delphi (np. preambuły osi).
 */
class MapFinderEngine @JvmOverloads constructor(
    private val bufferManager: BinaryBufferManager,
    private val minRows: Int = 4,
    private val maxRows: Int = 32,
    private val minCols: Int = 4,
    private val maxCols: Int = 32
) {

    /**
     * Wyszukuje potencjalne mapy 2D bazując na nagłówkach osi (standard Bosch EDC15/EDC16: wymiar X, wymiar Y).
     */
    fun scanForMaps(): List<MapDefinition> {
        val results = mutableListOf<MapDefinition>()
        val maxAddr = bufferManager.size - (minRows * minCols * 2) - 4

        var addr = 0
        while (addr < maxAddr) {
            // Przykładowy wzorzec Bosch 16-bit: [cols: 16bit] [rows: 16bit] lub odwrotnie
            val colsCandidate = bufferManager.readRawValue(addr, DataFormat.UWORD_LE).toInt()
            val rowsCandidate = bufferManager.readRawValue(addr + 2, DataFormat.UWORD_LE).toInt()

            if (colsCandidate in minCols..maxCols && rowsCandidate in minRows..maxRows) {
                val dataStart = addr + 4
                val mapSize = colsCandidate * rowsCandidate * 2

                if (dataStart + mapSize <= bufferManager.size) {
                    val map = MapDefinition(
                        id = "MAP_0x${addr.toString(16).uppercase()}",
                        name = "Potential Map (${colsCandidate}x${rowsCandidate})",
                        address = dataStart,
                        columns = colsCandidate,
                        rows = rowsCandidate,
                        dataFormat = DataFormat.UWORD_LE
                    )
                    results.add(map)
                    addr += mapSize // Przeskocz wykryty blok danych
                }
            }
            addr += 2 // Wyrównanie do słowa 16-bitowego
        }

        return results
    }
}