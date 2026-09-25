package com.winols.app.core.binary.finder

import com.winols.app.core.binary.EcuBinaryBuffer
import com.winols.app.core.binary.model.EcuMapDefinition
import java.util.UUID

class MapFinder {

    /**
     * Skanuje bufor pod kątem typowych struktur Bosch (np. nagłówki 2-bajtowe z rozmiarem osi)
     */
    fun findPotential16BitMaps(
        ecuBuffer: EcuBinaryBuffer,
        minRows: Int = 4,
        maxRows: Int = 32,
        minCols: Int = 4,
        maxCols: Int = 32
    ): List<EcuMapDefinition> {
        val foundMaps = mutableListOf<EcuMapDefinition>()
        val bufferSize = ecuBuffer.size

        var offset = 0
        while (offset < bufferSize - 4) {
            val potentialCols = ecuBuffer.read16Bit(offset)
            val potentialRows = ecuBuffer.read16Bit(offset + 2)

            if (potentialCols in minCols..maxCols && potentialRows in minRows..maxRows) {
                val dataSize = potentialRows * potentialCols * 2
                val mapDataStart = offset + 4

                if (mapDataStart + dataSize <= bufferSize) {
                    foundMaps.add(
                        EcuMapDefinition(
                            id = UUID.randomUUID().toString(),
                            name = "Map @ 0x${Integer.toHexString(mapDataStart).uppercase()}",
                            startOffset = mapDataStart,
                            rows = potentialRows,
                            cols = potentialCols,
                            is16Bit = true
                        )
                    )
                    offset += dataSize
                }
            }
            offset += 2
        }
        return foundMaps
    }
}