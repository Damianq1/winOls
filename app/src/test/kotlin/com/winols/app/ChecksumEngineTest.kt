package com.winols.app

import com.winols.app.checksum.ChecksumBlock
import com.winols.app.checksum.ChecksumEngine
import com.winols.app.checksum.ChecksumType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ChecksumEngineTest {

    @Test
    fun testAdd16LeCalculationAndPatching() {
        val binary = ByteArray(256)
        // Wypełnienie bufora przykładowymi danymi
        for (i in 0 until 200) {
            binary[i] = (i and 0xFF).toByte()
        }

        val block = ChecksumBlock(
            id = "test_16",
            name = "Test 16-bit",
            startOffset = 0x00,
            endOffset = 0xBF, // 192 bajty
            targetOffset = 0xC0,
            type = ChecksumType.ADD_16_LE
        )

        // Stan początkowy - suma niepoprawna (0 != calculated)
        val initialResults = ChecksumEngine.verifyChecksums(binary, listOf(block))
        assertFalse(initialResults.first().isValid)

        // Przeliczenie i korekta
        val patched = ChecksumEngine.correctChecksums(binary, listOf(block))
        assertEquals(1, patched)

        // Ponowna weryfikacja
        val finalResults = ChecksumEngine.verifyChecksums(binary, listOf(block))
        assertTrue(finalResults.first().isValid)
        assertEquals(finalResults.first().expectedValue, finalResults.first().actualValue)
    }

    @Test
    fun testCRC32Calculation() {
        val binary = ByteArray(128)
        binary[0] = 0x12
        binary[1] = 0x34
        binary[2] = 0x56

        val block = ChecksumBlock(
            id = "test_crc",
            name = "Test CRC32",
            startOffset = 0x00,
            endOffset = 0x0F,
            targetOffset = 0x10,
            type = ChecksumType.CRC32_STANDARD
        )

        ChecksumEngine.correctChecksums(binary, listOf(block))

        val results = ChecksumEngine.verifyChecksums(binary, listOf(block))
        assertTrue(results.first().isValid)
    }
}