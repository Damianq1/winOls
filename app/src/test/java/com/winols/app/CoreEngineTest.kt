package com.winols.app

import com.winols.app.data.BinaryBufferManager
import com.winols.app.model.BitWidth
import com.winols.app.model.Endianness
import com.winols.app.model.HexViewConfig
import com.winols.app.model.SignMode
import org.junit.Assert.assertEquals
import org.junit.Test

class CoreEngineTest {

    @Test
    fun test8BitUnsignedAndSigned() {
        // 0xFE = 254 (unsigned) lub -2 (signed)
        val data = byteArrayOf(0xFE.toByte())
        val manager = BinaryBufferManager(data)

        val unsignedVal = manager.readValue(0, BitWidth.BIT_8, Endianness.BIG_ENDIAN, SignMode.UNSIGNED)
        assertEquals(254, unsignedVal.toInt())

        val signedVal = manager.readValue(0, BitWidth.BIT_8, Endianness.BIG_ENDIAN, SignMode.SIGNED)
        assertEquals(-2, signedVal.toByte().toInt())
    }

    @Test
    fun test16BitLittleEndianVsBigEndian() {
        // Bajty: [0x12, 0x34]
        // Big Endian (Hi/Lo):    0x1234 = 4660
        // Little Endian (Lo/Hi): 0x3412 = 13330
        val data = byteArrayOf(0x12.toByte(), 0x34.toByte())
        val manager = BinaryBufferManager(data)

        val bigEndianVal = manager.readValue(0, BitWidth.BIT_16, Endianness.BIG_ENDIAN, SignMode.UNSIGNED)
        assertEquals(0x1234, bigEndianVal.toInt())
        assertEquals(4660, bigEndianVal.toInt())

        val littleEndianVal = manager.readValue(0, BitWidth.BIT_16, Endianness.LITTLE_ENDIAN, SignMode.UNSIGNED)
        assertEquals(0x3412, littleEndianVal.toInt())
        assertEquals(13330, littleEndianVal.toInt())
    }

    @Test
    fun test16BitSigned() {
        // Bajty: [0xFF, 0xFE]
        // Big Endian Signed: 0xFFFE = -2
        val data = byteArrayOf(0xFF.toByte(), 0xFE.toByte())
        val manager = BinaryBufferManager(data)

        val signedBig = manager.readValue(0, BitWidth.BIT_16, Endianness.BIG_ENDIAN, SignMode.SIGNED)
        assertEquals(-2, signedBig.toShort().toInt())

        val unsignedBig = manager.readValue(0, BitWidth.BIT_16, Endianness.BIG_ENDIAN, SignMode.UNSIGNED)
        assertEquals(65534, unsignedBig.toInt())
    }

    @Test
    fun testWrite16BitEndianness() {
        val buffer = ByteArray(2)
        val manager = BinaryBufferManager(buffer)

        // Zapis wartości 0x0A0B w Little Endian (Lo/Hi) -> na dysku: [0x0B, 0x0A]
        val configLe = HexViewConfig(bitWidth = BitWidth.BIT_16, endianness = Endianness.LITTLE_ENDIAN)
        manager.writeValue(0, 0x0A0B, configLe)

        assertEquals(0x0B.toByte(), manager.getRawBytes()[0])
        assertEquals(0x0A.toByte(), manager.getRawBytes()[1])

        // Zapis wartości 0x0A0B w Big Endian (Hi/Lo) -> na dysku: [0x0A, 0x0B]
        val configBe = HexViewConfig(bitWidth = BitWidth.BIT_16, endianness = Endianness.BIG_ENDIAN)
        manager.writeValue(0, 0x0A0B, configBe)

        assertEquals(0x0A.toByte(), manager.getRawBytes()[0])
        assertEquals(0x0B.toByte(), manager.getRawBytes()[1])
    }
}