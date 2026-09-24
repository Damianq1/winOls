package com.winols.app.engine

import com.winols.app.data.BinaryBufferManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ChecksumEngineTest {

    private lateinit var bufferManager: BinaryBufferManager
    private lateinit var engine: ChecksumEngine

    @Before
    fun setup() {
        // Bufor z przykładowymi danymi: 0x01, 0x02, 0x03, 0x04, 0x05, 0x06
        // Miejsce na sumy kontrolne od indeksu 10
        val data = ByteArray(32) { 0 }
        data[0] = 0x01
        data[1] = 0x02
        data[2] = 0x03
        data[3] = 0x04
        data[4] = 0x05
        data[5] = 0x06
        
        bufferManager = BinaryBufferManager(data)
        engine = ChecksumEngine(bufferManager)
    }

    @Test
    fun testAdd8Checksum() {
        val block = ChecksumBlock(
            id = "chk_add8",
            name = "Test 8-bit Add",
            startAddress = 0,
            endAddress = 5,
            checksumAddress = 10,
            algorithm = ChecksumAlgorithm.ADD_8
        )
        // Suma 8-bit: 1 + 2 + 3 + 4 + 5 + 6 = 21 (0x15)
        val calculated = engine.calculate(block)
        assertEquals(0x15L, calculated)

        // Symulacja zapisanej poprawnej sumy
        bufferManager.setByte(10, 0x15.toByte())
        val result = engine.verify(block)
        assertTrue(result.isValid)
    }

    @Test
    fun testAdd16BigEndianChecksum() {
        val block = ChecksumBlock(
            id = "chk_add16_be",
            name = "Test 16-bit Add (BE)",
            startAddress = 0,
            endAddress = 5,
            checksumAddress = 10,
            algorithm = ChecksumAlgorithm.ADD_16,
            isLittleEndian = false
        )
        // Suma 16-bit BE: 0x0102 + 0x0304 + 0x0506 = 0x090C
        val calculated = engine.calculate(block)
        assertEquals(0x090CL, calculated)
    }

    @Test
    fun testAdd16LittleEndianChecksum() {
        val block = ChecksumBlock(
            id = "chk_add16_le",
            name = "Test 16-bit Add (LE)",
            startAddress = 0,
            endAddress = 5,
            checksumAddress = 10,
            algorithm = ChecksumAlgorithm.ADD_16,
            isLittleEndian = true
        )
        // Suma 16-bit LE: 0x0201 + 0x0403 + 0x0605 = 0x0C09
        val calculated = engine.calculate(block)
        assertEquals(0x0C09L, calculated)
    }

    @Test
    fun testCorrectionMechanism() {
        val block = ChecksumBlock(
            id = "chk_correction",
            name = "Correction Test",
            startAddress = 0,
            endAddress = 3,
            checksumAddress = 10,
            algorithm = ChecksumAlgorithm.ADD_32,
            isLittleEndian = false
        )
        // 32-bit BE dla [01, 02, 03, 04] = 0x01020304
        
        // Bufor ma zainicjowane zera na pozycji 10, więc suma jest błędna
        assertFalse(engine.verify(block).isValid)

        // Korekcja sumy
        val results = engine.correctAll(listOf(block))
        assertTrue(results.first().isValid)
        
        // Weryfikacja czy bajty w buforze zostały faktycznie zmienione (Big Endian)
        assertEquals(0x01.toByte(), bufferManager.getByte(10))
        assertEquals(0x02.toByte(), bufferManager.getByte(11))
        assertEquals(0x03.toByte(), bufferManager.getByte(12))
        assertEquals(0x04.toByte(), bufferManager.getByte(13))
    }
}