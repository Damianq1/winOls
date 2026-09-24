package com.winols.app.engine

import com.winols.app.data.BinaryBufferManager
import com.winols.app.model.DataType
import com.winols.app.model.MapDefinition
import java.util.UUID

class MapFinderEngine(private val bufferManager: BinaryBufferManager) {

    fun scanPotentialMaps(
        minRows: Int = 4,
        maxRows: Int = 32,
        minCols: Int = 4,
        maxCols: Int = 32,
        dataType: DataType = DataType.UWORD_BE
    ): List<MapDefinition> {
        val candidates = mutableListOf<MapDefinition>()
        val step = dataType.byteSize
        val end = bufferManager.size - (minRows * minCols * step)

        var addr = 0
        while (addr < end) {
            if (isPlausibleHeader(addr, dataType)) {
                val rows = 16
                val cols = 16
                if (addr + rows * cols * step <= bufferManager.size) {
                    candidates.add(
                        MapDefinition(
                            id = UUID.randomUUID().toString(),
                            name = "AutoMap_${Integer.toHexString(addr).uppercase()}",
                            startAddress = addr,
                            rows = rows,
                            columns = cols,
                            dataType = dataType
                        )
                    )
                    addr += rows * cols * step
                    continue
                }
            }
            addr += step
        }
        return candidates
    }

    private fun isPlausibleHeader(address: Int, dataType: DataType): Boolean {
        if (address + 4 > bufferManager.size) return false
        val val1 = bufferManager.readValue(address, dataType)
        val val2 = bufferManager.readValue(address + dataType.byteSize, dataType)
        return val1 > 0 && val2 > 0 && val1 != val2 && val1 < 0xFFFF
    }
}