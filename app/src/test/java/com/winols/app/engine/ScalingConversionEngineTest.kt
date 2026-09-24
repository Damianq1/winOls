package com.winols.app.engine

import com.winols.app.model.DataType
import org.junit.Assert.assertEquals
import org.junit.Test

class ScalingConversionEngineTest {

    @Test
    fun testRpmConversion() {
        // Bosch RPM factor: 0.25, offset: 0
        val rawRpm = 3200.0
        val physical = ScalingConversionEngine.rawToPhysical(rawRpm, factor = 0.25, offset = 0.0)
        assertEquals(800.0, physical, 0.001)

        val backToRaw = ScalingConversionEngine.physicalToRaw(800.0, factor = 0.25, offset = 0.0, dataType = DataType.UINT16)
        assertEquals(3200.0, backToRaw, 0.001)
    }

    @Test
    fun testTemperatureConversionWithOffset() {
        // Bosch EDC15 Temp factor: 0.1, offset: -273.1
        val rawTemp = 3631.0
        val physical = ScalingConversionEngine.rawToPhysical(rawTemp, factor = 0.1, offset = -273.1)
        assertEquals(90.0, physical, 0.01)

        val backToRaw = ScalingConversionEngine.physicalToRaw(90.0, factor = 0.1, offset = -273.1, dataType = DataType.UINT16)
        assertEquals(3631.0, backToRaw, 0.001)
    }

    @Test
    fun testSaturationClampingUint8() {
        // UINT8 zakres 0-255: próba zapisu wartości 300 fizycznej przy factor=1 powinna zostać ograniczona do 255
        val clampedRaw = ScalingConversionEngine.physicalToRaw(300.0, factor = 1.0, offset = 0.0, dataType = DataType.UINT8)
        assertEquals(255.0, clampedRaw, 0.001)

        // Próba zapisu ujemnej do UINT8 powinna dać 0
        val clampedNegative = ScalingConversionEngine.physicalToRaw(-50.0, factor = 1.0, offset = 0.0, dataType = DataType.UINT8)
        assertEquals(0.0, clampedNegative, 0.001)
    }
}