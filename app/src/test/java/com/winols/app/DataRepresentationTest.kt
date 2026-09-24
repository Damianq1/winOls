package com.winols.app

import com.winols.app.engine.DataRepresentationEngine
import com.winols.app.model.BitWidth
import com.winols.app.model.DataFormatConfig
import com.winols.app.model.Endianness
import com.winols.app.model.Signedness
import org.junit.Assert.assertEquals
import org.junit.Test

class DataRepresentationTest {

    private val engine = DataRepresentationEngine()

    @Test
    fun testEndiannessAndSignedness16Bit() {
        // Bajty: 0x80, 0x01
        val buffer = byteArrayOf(0x80.toByte(), 0x01.toByte())

        // HiLo (Big-Endian) Unsigned: 0x8001 = 32769
        val cfgHiLoUnsigned = DataFormatConfig(
            bitWidth = BitWidth.BITS_16,
            signedness = Signedness.UNSIGNED,
            endianness = Endianness.HI_LO
        )
        assertEquals(32769, engine.extractValue(buffer, 0, cfgHiLoUnsigned).toInt())

        // HiLo (Big-Endian) Signed: 0x8001 = -32767
        val cfgHiLoSigned = DataFormatConfig(
            bitWidth = BitWidth.BITS_16,
            signedness = Signedness.SIGNED,
            endianness = Endianness.HI_LO
        )
        assertEquals(-32767, engine.extractValue(buffer, 0, cfgHiLoSigned).toInt())

        // LoHi (Little-Endian) Unsigned: 0x0180 = 384
        val cfgLoHiUnsigned = DataFormatConfig(
            bitWidth = BitWidth.BITS_16,
            signedness = Signedness.UNSIGNED,
            endianness = Endianness.LO_HI
        )
        assertEquals(384, engine.extractValue(buffer, 0, cfgLoHiUnsigned).toInt())
    }

    @Test
    fun testMatrix3DExtraction() {
        val buffer = byteArrayOf(
            0x00, 0x01, 0x00, 0x02,
            0x00, 0x03, 0x00, 0x04
        )
        val cfg = DataFormatConfig(
            bitWidth = BitWidth.BITS_16,
            signedness = Signedness.UNSIGNED,
            endianness = Endianness.HI_LO
        )

        val matrix = engine.extractMatrix3D(buffer, 0, rows = 2, cols = 2, config = cfg)
        assertEquals(1.0, matrix[0][0], 0.001)
        assertEquals(2.0, matrix[0][1], 0.001)
        assertEquals(3.0, matrix[1][0], 0.001)
        assertEquals(4.0, matrix[1][1], 0.001)
    }
}