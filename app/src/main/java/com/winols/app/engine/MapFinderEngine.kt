package com.winols.app.engine

import com.winols.app.data.BinaryBufferManager
import com.winols.app.model.DataType
import com.winols.app.model.MapDefinition
import java.nio.ByteOrder

class MapFinderEngine(private val bufferManager: BinaryBufferManager) {

    /**
     * Wyszukuje potencjalne tabele na podstawie rosnących/uporządkowanych osi i monotonicznych nagłówków.
     */
    fun findPotentialMaps(minColumns: Int = 4, maxColumns: Int = 32): List<MapDefinition> {
        val candidates = mutableListOf<MapDefinition>()
        val totalSize = bufferManager.size
        var offset = 0

        while (offset < totalSize - (minColumns * 2)) {
            // Heurystyka: wykrywanie wektora osi (np. wartości 16-bit rosnące monotonicznie)
            if (isStrictlyMonotonicSequence(offset, minColumns, DataType.USHORT, ByteOrder.BIG_ENDIAN)) {
                candidates.add(
                    MapDefinition(
                        id = "MAP_${Integer.toHexString(offset).uppercase()}",
                        name = "Found Map @ 0x${Integer.toHexString(offset).uppercase()}",
                        startOffset = offset,
                        columns = minColumns,
                        rows = 1,
                        dataType = DataType.USHORT,
                        byteOrder = ByteOrder.BIG_ENDIAN
                    )
                )
                offset += minColumns * DataType.USHORT.byteSize
            } else {
                offset += 2
            }
        }
        return candidates
    }

    private fun isStrictlyMonotonicSequence(
        offset: Int,
        count: Int,
        type: DataType,
        order: ByteOrder
    ): Boolean {
        if (offset + (count * type.byteSize) > bufferManager.size) return false

        var prev = bufferManager.getUShort(offset, order)
        for (i in 1 until count) {
            val curr = bufferManager.getUShort(offset + (i * type.byteSize), order)
            if (curr <= prev) return false
            prev = curr
        }
        return true
    }
}