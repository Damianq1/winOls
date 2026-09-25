package com.winols.app.domain.detector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MapScannerTest {

    @Test
    fun testDetectsBoschStyleMapWithAxes() {
        val buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN)

        val rows = 4
        val cols = 4

        // Padding początkowy
        buffer.position(0x10)

        // Nagłówek wymiarów (Rows, Cols)
        buffer.putShort(rows.toShort())
        buffer.putShort(cols.toShort())

        // Oś X (monotonicznie rosnąca, np. obroty RPM: 1000, 2000, 3000, 4000)
        listOf(1000, 2000, 3000, 4000).forEach { buffer.putShort(it.toShort()) }

        // Oś Y (monotonicznie rosnąca, np. dawka paliwa: 10, 20, 30, 40)
        listOf(10, 20, 30, 40).forEach { buffer.putShort(it.toShort()) }

        // Wartości mapy (zmienne gradientowo)
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                buffer.putShort(((r + 1) * (c + 1) * 100).toShort())
            }
        }

        val scanner = MapScanner()
        val results = scanner.scanBinary(buffer.array(), ByteOrder.LITTLE_ENDIAN)

        assertEquals(1, results.size)
        val map = results.first()
        assertEquals(4, map.rows)
        assertEquals(4, map.cols)
        assertNotNull(map.xAxis)
        assertNotNull(map.yAxis)
        assertEquals(true, map.xAxis?.isMonotonic)
        assertEquals(true, map.yAxis?.isMonotonic)
    }
}