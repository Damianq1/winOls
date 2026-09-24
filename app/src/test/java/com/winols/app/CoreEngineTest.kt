package com.winols.app

import com.winols.app.data.BinaryBufferManager
import com.winols.app.edit.MapEditor
import com.winols.app.model.MapDefinition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer

class CoreEngineTest {

    @Test
    fun testMatrixSmoothingRemovesSpikes() {
        val size = 9 * 2 // 3x3 mapa 16-bit
        val buffer = ByteBuffer.allocateDirect(size)
        val manager = BinaryBufferManager(buffer)

        val map = MapDefinition(
            name = "Test Map",
            address = 0,
            rows = 3,
            cols = 3,
            dataType = MapDefinition.DataType.WORD,
            isSigned = false,
            isBigEndian = false
        )

        // Macierz z widocznym pikiem na środku:
        // [ 100, 100, 100 ]
        // [ 100, 500, 100 ]
        // [ 100, 100, 100 ]
        val initialValues = longArrayOf(
            100, 100, 100,
            100, 500, 100,
            100, 100, 100
        )

        for (i in initialValues.indices) {
            manager.writeRawValue(i * 2, initialValues[i], MapDefinition.DataType.WORD, false, false)
        }

        val editor = MapEditor(manager)
        editor.smooth(map, strength = 1.0)

        val centerValue = manager.readRawValue(4 * 2, MapDefinition.DataType.WORD, false, false)

        // Środek powinien spaść ze skrajnej wartości 500 w stronę wartości sąsiadów
        assertTrue("Wartość środkowa powinna zostać wygładzona w dół", centerValue < 500)
        assertTrue("Wartość środkowa powinna pozostać powyżej bazy 100", centerValue > 100)
    }
}