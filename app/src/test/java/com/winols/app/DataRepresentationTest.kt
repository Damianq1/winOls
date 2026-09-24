package com.winols.app

import com.winols.app.model.BitWidth
import com.winols.app.model.DisplayRadix
import com.winols.app.model.Signedness
import com.winols.app.model.ViewMode
import com.winols.app.viewmodel.DataPresentationController
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DataRepresentationTest {

    @Test
    fun testEndiannessAndBitWidthRepresentations() {
        // [0x12, 0x34, 0xFF, 0xFE]
        val bytes = byteArrayOf(0x12, 0x34, 0xFF.toByte(), 0xFE.toByte())
        val buffer = ByteBuffer.wrap(bytes)
        val controller = DataPresentationController(buffer)

        // 16-bit Big Endian (Hi-Lo): 0x1234 = 4660
        controller.setFormat(BitWidth.BIT_16, Signedness.UNSIGNED, ByteOrder.BIG_ENDIAN, DisplayRadix.HEX)
        val hex16HiLo = controller.getFormattedHexRow(0, 4)
        assertEquals("1234", hex16HiLo[0])
        assertEquals("FFFE", hex16HiLo[1])

        // 16-bit Little Endian (Lo-Hi): 0x3412, 0xFEFF
        controller.toggleByteOrder()
        val hex16LoHi = controller.getFormattedHexRow(0, 4)
        assertEquals("3412", hex16LoHi[0])
        assertEquals("FEFF", hex16LoHi[1])

        // 8-bit Unsigned vs Signed
        controller.setFormat(BitWidth.BIT_8, Signedness.UNSIGNED, ByteOrder.BIG_ENDIAN, DisplayRadix.DECIMAL)
        val dec8Unsigned = controller.getFormattedHexRow(2, 2)
        assertEquals("255", dec8Unsigned[0])
        assertEquals("254", dec8Unsigned[1])

        controller.toggleSignedness()
        val dec8Signed = controller.getFormattedHexRow(2, 2)
        assertEquals("-1", dec8Signed[0])
        assertEquals("-2", dec8Signed[1])
    }

    @Test
    fun testExtraction2DAnd3D() {
        val testData = ByteArray(16) { it.toByte() }
        val buffer = ByteBuffer.wrap(testData)
        val controller = DataPresentationController(buffer)

        controller.setFormat(BitWidth.BIT_8, Signedness.UNSIGNED, ByteOrder.BIG_ENDIAN, DisplayRadix.DECIMAL)

        // 2D Profile
        val points = controller.extract2DProfile(0, 4)
        assertEquals(4, points.size)
        assertEquals(0.0, points[0].physicalValue, 0.001)
        assertEquals(3.0, points[3].physicalValue, 0.001)

        // 3D Matrix 2x2 (16-bit)
        controller.setFormat(BitWidth.BIT_16, Signedness.UNSIGNED, ByteOrder.BIG_ENDIAN, DisplayRadix.DECIMAL)
        val matrix = controller.extract3DMatrix(0, 2, 2)
        assertEquals(2, matrix.rows)
        assertEquals(2, matrix.cols)
        // 0x0001 = 1, 0x0203 = 515
        assertEquals(1.0, matrix.data[0][0], 0.001)
        assertEquals(515.0, matrix.data[0][1], 0.001)
    }
}