package com.winols.app

import com.winols.app.data.BinaryBufferManager
import com.winols.app.data.IntelHexCodec
import com.winols.app.edit.MapEditor
import com.winols.app.engine.ChecksumEngine
import com.winols.app.engine.ChecksumFamily
import com.winols.app.model.AxisDefinition
import com.winols.app.model.DataType
import com.winols.app.model.MapDefinition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

class CoreEngineTest {

    private lateinit var bufferManager: BinaryBufferManager
    private lateinit var binModel: BinModel
    private lateinit var mapEditor: MapEditor
    private lateinit var checksumEngine: ChecksumEngine

    @Before
    fun setUp() {
        bufferManager = BinaryBufferManager(2048)
        binModel = BinModel(bufferManager)
        mapEditor = MapEditor(binModel)
        checksumEngine = ChecksumEngine()
    }

    @Test
    fun testBufferReadWritePerformanceTypes() {
        val testAddr = 0x100L
        bufferManager.writeValue(testAddr, DataType.UWORD_LE, 45200.0)
        assertEquals(45200.0, bufferManager.readValue(testAddr, DataType.UWORD_LE), 0.001)

        bufferManager.writeValue(testAddr + 4, DataType.UWORD_BE, 12345.0)
        assertEquals(12345.0, bufferManager.readValue(testAddr + 4, DataType.UWORD_BE), 0.001)

        bufferManager.writeValue(testAddr + 8, DataType.UDWORD_LE, 305419896.0) // 0x12345678
        assertEquals(305419896.0, bufferManager.readValue(testAddr + 8, DataType.UDWORD_LE), 0.001)
    }

    @Test
    fun testOptimizedWordLevelDifferences() {
        val size = 1024
        val bytes = ByteArray(size) { 0xAA.toByte() }
        bufferManager.loadFromBytes(bytes)

        assertTrue(bufferManager.getDifferences().isEmpty())

        // Modyfikacja w dwóch różnych blokach 64-bitowych
        bufferManager.writeValue(16L, DataType.UBYTE, 0xFF.toDouble())
        bufferManager.writeValue(500L, DataType.UBYTE, 0x11.toDouble())

        val diffs = bufferManager.getDifferences()
        assertEquals(2, diffs.size)
        assertTrue(diffs.contains(16L))
        assertTrue(diffs.contains(500L))
    }

    @Test
    fun testIntelHexCodecEncodeDecode() {
        val hexCodec = IntelHexCodec()
        val originalData = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte())
        val baseAddr = 0x80000L

        val hexString = hexCodec.encodeToHex(originalData, startAddress = baseAddr, bytesPerLine = 4)
        assertTrue(hexString.contains(":020000040008")) // Extended linear address for 0x80000

        val decoded = hexCodec.decodeHex(ByteArrayInputStream(hexString.toByteArray(Charsets.US_ASCII)))
        assertEquals(baseAddr, decoded.baseAddress)
        assertEquals(originalData.size, decoded.binaryData.size)
        for (i in originalData.indices) {
            assertEquals(originalData[i], decoded.binaryData[i])
        }
    }

    @Test
    fun testChecksumCalculationAndPatch16Bit() {
        val buffer = ByteArray(512)
        for (i in 0 until 500) {
            buffer[i] = (i and 0xFF).toByte()
        }

        val start = 0
        val end = 500
        val checksumLoc = 502

        val initialVerify = checksumEngine.verifyBlock(
            buffer,
            start,
            end,
            checksumLoc,
            ChecksumFamily.GENERIC_SUM_16_LE
        )
        assertFalse(initialVerify.isValid)

        val patched = checksumEngine.patchChecksum(
            buffer,
            start,
            end,
            checksumLoc,
            ChecksumFamily.GENERIC_SUM_16_LE
        )
        assertTrue(patched)

        val verified = checksumEngine.verifyBlock(
            buffer,
            start,
            end,
            checksumLoc,
            ChecksumFamily.GENERIC_SUM_16_LE
        )
        assertTrue(verified.isValid)
        assertEquals(verified.calculatedChecksum, verified.storedChecksum)
    }

    @Test
    fun testMapEngineeringValueConversion() {
        val map = MapDefinition(
            id = "TURBO_PRESSURE",
            name = "Turbo Target Pressure",
            startAddress = 0x200L,
            rows = 4,
            columns = 4,
            dataType = DataType.UWORD_LE,
            factor = 0.05,
            offset = 100.0,
            unit = "mbar",
            xAxis = AxisDefinition("RPM", length = 4),
            yAxis = AxisDefinition("Load", length = 4)
        )

        binModel.setCellValue(map, 0, 0, 1100.0)
        val retrievedVal = binModel.getCellValue(map, 0, 0)
        assertEquals(1100.0, retrievedVal, 0.001)

        val rawRead = bufferManager.readValue(0x200L, DataType.UWORD_LE)
        assertEquals(20000.0, rawRead, 0.001)
    }
}