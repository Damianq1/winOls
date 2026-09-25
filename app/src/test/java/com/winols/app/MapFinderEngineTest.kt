package com.winols.app

import com.winols.app.domain.engine.MapFinderEngine
import com.winols.app.domain.model.Endianness
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MapFinderEngineTest {

    @Test
    fun testDetectsSyntheticMapWithHeader() = runBlocking {
        val rows = 8
        val cols = 8
        val buffer = ByteArray(1024)
        val byteBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.BIG_ENDIAN)

        val headerOffset = 64
        byteBuffer.putShort(headerOffset, rows.toShort())
        byteBuffer.putShort(headerOffset + 2, cols.toShort())

        val dataStart = headerOffset + 4
        for (i in 0 until (rows * cols)) {
            byteBuffer.putShort(dataStart + (i * 2), (1000 + i * 15).toShort())
        }

        val engine = MapFinderEngine()
        val maps = engine.scanBinary(buffer, Endianness.BIG_ENDIAN)

        assertTrue(maps.isNotEmpty())
        val found = maps.first()
        assertEquals(rows, found.rows)
        assertEquals(cols, found.columns)
        assertEquals(dataStart, found.address)
        assertTrue(found.confidenceScore > 0.65f)
    }
}