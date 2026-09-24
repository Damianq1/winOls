package com.winols.app

import com.winols.app.data.BinaryBufferManager
import com.winols.app.engine.ChecksumEngine
import com.winols.app.engine.MapFinderEngine
import com.winols.app.model.DataType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CoreEngineTest {

    private lateinit var bufferManager: BinaryBufferManager
    private lateinit var checksumEngine: ChecksumEngine
    private lateinit var mapFinderEngine: MapFinderEngine

    @Before
    fun setUp() {
        // Bufor testowy o rozmiarze 1024 bajtów
        val initialBytes = ByteArray(1024)
        bufferManager = BinaryBufferManager(initialBytes)
        checksumEngine = ChecksumEngine(bufferManager)
        mapFinderEngine = MapFinderEngine(bufferManager)
    }

    @Test
    fun testBufferReadWritePrimitives() {
        bufferManager.writeValue(0x10, DataType.UBYTE, 255.0)
        assertEquals(255.0, bufferManager.readValue(0x10, DataType.UBYTE), 0.0)

        bufferManager.writeValue(0x20, DataType.UWORD_LE, 45000.0)
        assertEquals(45000.0, bufferManager.readValue(0x20, DataType.UWORD_LE), 0.0)

        bufferManager.writeValue(0x30, DataType.SWORD_BE, -1200.0)
        assertEquals(-1200.0, bufferManager.readValue(0x30, DataType.SWORD_BE), 0.0)
    }

    @Test
    fun testChecksumCalculationAndUpdate() {
        bufferManager.writeValue(0x00, DataType.UWORD_LE, 1000.0)
        bufferManager.writeValue(0x02, DataType.UWORD_LE, 2000.0)

        val calculated = checksumEngine.calculateSimple16BitSum(0x00, 0x03)
        assertEquals(3000, calculated)

        checksumEngine.updateChecksum16Bit(0x00, 0x03, 0x08)
        assertTrue(checksumEngine.verifyChecksum16Bit(0x00, 0x03, 0x08))
    }

    @Test
    fun testMonotonicMapFinder() {
        // Generowanie monotonicznie rosnącej osi/sekwencji o długości 8 elementów (16 bajtów)
        val startAddr = 0x80
        for (i in 0 until 8) {
            val addr = startAddr + (i * 2)
            val value = (1000 + i * 250).toDouble()
            bufferManager.writeValue(addr, DataType.UWORD_LE, value)
        }

        val detected = mapFinderEngine.scanForMonotonicRegions(minElements = 8, dataType = DataType.UWORD_LE)
        assertTrue("Powinien wykryć co najmniej jeden region monotoniczny", detected.isNotEmpty())
        assertTrue("Wykryty adres powinien zawierać $startAddr", detected.contains(startAddr))
    }
}