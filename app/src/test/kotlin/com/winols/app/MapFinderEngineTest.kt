package com.winols.app

import com.winols.app.core.engine.MapFinderEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MapFinderEngineTest {

    @Test
    fun testFindPotentialMap() {
        val buffer = ByteArray(1024)
        val byteBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.BIG_ENDIAN)

        val headerOffset = 128
        val cols = 8
        val rows = 6

        // Nagłówek wymiarów osi
        byteBuffer.putShort(headerOffset, cols.toShort())
        byteBuffer.putShort(headerOffset + 2, rows.toShort())

        // Dane mapy (gradient liniowy o kontrolowanej entropii)
        var dataPos = headerOffset + 4
        for (i in 0 until (cols * rows)) {
            byteBuffer.putShort(dataPos, (i * 100).toShort())
            dataPos += 2
        }

        val engine = MapFinderEngine(buffer, ByteOrder.BIG_ENDIAN)
        val maps = engine.findPotentialMaps(minDimension = 4, maxDimension = 16)

        assertTrue(maps.isNotEmpty())
        val found = maps.firstOrNull { it.offset == headerOffset + 4 }
        assertTrue(found != null)
        assertEquals(8, found?.cols)
        assertEquals(6, found?.rows)

        val values = engine.readMapValues(found!!)
        assertEquals(6, values.size)
        assertEquals(8, values[0].size)
        assertEquals(0.0, values[0][0], 0.001)
        assertEquals(100.0, values[0][1], 0.001)
    }
}