package com.winols.app.engine

import com.winols.app.data.BinaryBufferManager
import com.winols.app.model.DataType
import com.winols.app.model.MapDefinition

class MapFinderEngine(private val bufferManager: BinaryBufferManager) {

    fun scanForMonotonicRegions(
        minElements: Int = 8,
        dataType: DataType = DataType.UWORD_LE
    ): List<Int> {
        val candidateAddresses = mutableListOf<Int>()
        val step = dataType.byteSize
        val totalBytes = bufferManager.size

        if (totalBytes < minElements * step) return candidateAddresses

        var count = 1
        var startAddr = 0
        var prevVal = bufferManager.readValue(0, dataType)

        var addr = step
        while (addr + step <= totalBytes) {
            val currVal = bufferManager.readValue(addr, dataType)
            if (currVal >= prevVal && currVal > 0.0) {
                if (count == 1) {
                    startAddr = addr - step
                }
                count++
                if (count >= minElements) {
                    if (!candidateAddresses.contains(startAddr)) {
                        candidateAddresses.add(startAddr)
                    }
                }
            } else {
                count = 1
            }
            prevVal = currVal
            addr += step
        }
        return candidateAddresses
    }

    fun detectPotentialMaps(rows: Int, cols: Int, dataType: DataType = DataType.UWORD_LE): List<MapDefinition> {
        val maps = mutableListOf<MapDefinition>()
        val byteLength = rows * cols * dataType.byteSize
        val candidates = scanForMonotonicRegions(minElements = cols, dataType = dataType)

        for (cand in candidates) {
            if (cand + byteLength <= bufferManager.size) {
                maps.add(
                    MapDefinition(
                        id = "MAP_0x${cand.toString(16).uppercase()}",
                        name = "Potential Map @ 0x${cand.toString(16).uppercase()}",
                        address = cand,
                        rows = rows,
                        cols = cols,
                        dataType = dataType
                    )
                )
            }
        }
        return maps
    }
}