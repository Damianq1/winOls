package com.winols.app

import com.winols.app.engine.ChecksumAlgorithm
import com.winols.app.engine.ChecksumBlock
import com.winols.app.engine.ChecksumEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ChecksumEngineTest {

    @Test
    fun testAdd8ChecksumCalculationAndPatching() {
        val buffer = ByteBuffer.allocate(10)
        // Wypełnienie danymi: 10, 20, 30 -> suma = 60 (0x3C)
        buffer.put(0, 10.toByte())
        buffer.put(1, 20.toByte())
        buffer.put(2, 30.toByte())
        buffer.put(3, 0.toByte()) // Miejsce na sumę kontrolną

        val block = ChecksumBlock(
            id = "blk1",
            name = "Main ADD8",
            startAddress = 0,
            endAddress = 3,
            checksumAddress = 3,
            algorithm = ChecksumAlgorithm.ADD8
        )

        // Przed patchem: oczekiwana niezgodność
        val verifyBefore = ChecksumEngine.verifyBlock(buffer, block)
        assertFalse(verifyBefore.isValid)
        assertEquals(60L, verifyBefore.calculatedValue)
        assertEquals(0L, verifyBefore.storedValue)

        // Po zaaplikowaniu patcha: zgodność
        ChecksumEngine.patchChecksum(buffer, block)
        val verifyAfter = ChecksumEngine.verifyBlock(buffer, block)
        assertTrue(verifyAfter.isValid)
        assertEquals(60.toByte(), buffer.get(3))
    }

    @Test
    fun testCrc32Checksum() {
        val testBytes = byteArrayOf(0x31, 0x32, 0x33, 0x34, 0x35) // "12345"
        val buffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
        buffer.position(0)
        buffer.put(testBytes)

        val block = ChecksumBlock(
            id = "crc_test",
            name = "CRC32 Test",
            startAddress = 0,
            endAddress = 5,
            checksumAddress = 8,
            algorithm = ChecksumAlgorithm.CRC32
        )

        val calculated = ChecksumEngine.patchChecksum(buffer, block)
        val result = ChecksumEngine.verifyBlock(buffer, block)

        assertTrue(result.isValid)
        assertEquals(calculated, result.calculatedValue)
    }
}