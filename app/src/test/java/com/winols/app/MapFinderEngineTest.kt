package com.winols.app

import com.winols.app.engine.MapFinderEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MapFinderEngineTest {

    @Test
    fun testDetectBosch3DMapWithHeaders() {
        val engine = MapFinderEngine()
        val buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN)

        // Nagłówek osi X: ID = 0x01E0, Rozmiar = 4
        val xHeaderAddr = 0x20
        buffer.putShort(xHeaderAddr, 0x01E0.toShort())
        buffer.putShort(xHeaderAddr + 2, 4.toShort())
        // Oś X: RPM [1000, 2000, 3000, 4000]
        buffer.putShort(xHeaderAddr + 4, 1000.toShort())
        buffer.putShort(xHeaderAddr + 6, 2000.toShort())
        buffer.putShort(xHeaderAddr + 8, 3000.toShort())
        buffer.putShort(xHeaderAddr + 10, 4000.toShort())

        // Nagłówek osi Y: ID = 0x0280, Rozmiar = 3
        val yHeaderAddr = xHeaderAddr + 12
        buffer.putShort(yHeaderAddr, 0x0280.toShort())
        buffer.putShort(yHeaderAddr + 2, 3.toShort())
        // Oś Y: Throttle [%]: [20, 50, 100]
        buffer.putShort(yHeaderAddr + 4, 20.toShort())
        buffer.putShort(yHeaderAddr + 6, 50.toShort())
        buffer.putShort(yHeaderAddr + 8, 100.toShort())

        // Dane mapy: 3 wiersze x 4 kolumny = 12 słów (24 bajty)
        val dataAddr = yHeaderAddr + 10
        for (i in 0 until 12) {
            buffer.putShort(dataAddr + (i * 2), (250 + i * 10).toShort())
        }

        val maps = engine.findCandidateMaps(buffer)

        assertTrue(maps.isNotEmpty())
        val found = maps.firstOrNull { it.startAddress == dataAddr }
        assertTrue("Mapa 3x4 powinna zostać poprawnie wykryta", found != null)
        assertEquals(3, found!!.rows)
        assertEquals(4, found.cols)
        assertEquals(dataAddr, found.startAddress)
        assertEquals(4, found.xAxis?.size)
        assertEquals(3, found.yAxis?.size)
        assertEquals(1000.0, found.xAxis!![0], 0.001)
        assertEquals(4000.0, found.xAxis!![3], 0.001)
    }

    @Test
    fun testDetectBosch2DCurve() {
        val engine = MapFinderEngine()
        val buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN)

        // Nagłówek osi X: ID = 0x00E0, Rozmiar = 5
        val xHeaderAddr = 0x10
        buffer.putShort(xHeaderAddr, 0x00E0.toShort())
        buffer.putShort(xHeaderAddr + 2, 5.toShort())
        for (i in 0 until 5) {
            buffer.putShort(xHeaderAddr + 4 + (i * 2), (i * 100).toShort())
        }

        // Kolejne bajty nie pasują do kolejnego deskryptora osi (np. 0x0000)
        val dataAddr = xHeaderAddr + 14
        for (i in 0 until 5) {
            buffer.putShort(dataAddr + (i * 2), (1000 - i * 50).toShort())
        }

        val maps = engine.findCandidateMaps(buffer)
        val curve = maps.firstOrNull { it.startAddress == dataAddr }

        assertTrue("Krzywa 1x5 powinna zostać wykryta", curve != null)
        assertEquals(1, curve!!.rows)
        assertEquals(5, curve.cols)
    }
}