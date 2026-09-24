package com.winols.app

import com.winols.app.data.BinaryBufferManager
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

class CoreEngineTest {

    private lateinit var bufferManager: BinaryBufferManager
    private lateinit var binModel: BinModel
    private lateinit var mapEditor: MapEditor
    private lateinit var checksumEngine: ChecksumEngine

    @Before
    fun setUp() {
        bufferManager = BinaryBufferManager(1024)
        binModel = BinModel(bufferManager)
        mapEditor = MapEditor(binModel)
        checksumEngine = ChecksumEngine()
    }

    @Test
    fun testBufferReadWriteWordLE() {
        val testAddr = 0x100L
        bufferManager.writeValue(testAddr, DataType.UWORD_LE, 12500.0)
        val readVal = bufferManager.readValue(testAddr, DataType.UWORD_LE)
        assertEquals(12500.0, readVal, 0.001)
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

        // Ustawienie wartości inżynierskiej: 1100.0 mbar
        // raw = (1100.0 - 100.0) / 0.05 = 20000
        binModel.setCellValue(map, 0, 0, 1100.0)

        val retrievedVal = binModel.getCellValue(map, 0, 0)
        assertEquals(1100.0, retrievedVal, 0.001)

        val rawRead = bufferManager.readValue(0x200L, DataType.UWORD_LE)
        assertEquals(20000.0, rawRead, 0.001)
    }

    @Test
    fun testChecksumCalculationAndPatch16Bit() {
        val buffer = ByteArray(512)
        // Wypełnienie bufora przykładowymi danymi
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
    fun testMapEditorInterpolation() {
        val map = MapDefinition(
            id = "SPARK_ADVANCE",
            name = "Base Ignition",
            startAddress = 0x50L,
            rows = 3,
            columns = 3,
            dataType = DataType.UWORD_LE,
            factor = 1.0,
            offset = 0.0
        )

        binModel.setCellValue(map, 0, 0, 10.0)
        binModel.setCellValue(map, 0, 2, 20.0)
        binModel.setCellValue(map, 2, 0, 30.0)
        binModel.setCellValue(map, 2, 2, 40.0)

        mapEditor.interpolate2D(map, 0, 0, 2, 2)

        val centerValue = binModel.getCellValue(map, 1, 1)
        // Środek płaszczyzny interpolowanej liniowo powinien wynosić 25.0
        assertEquals(25.0, centerValue, 0.5)
    }
}