package com.winols.app

import com.winols.app.domain.model.MapTable
import com.winols.app.domain.model.SelectionArea
import org.junit.Assert.assertEquals
import org.junit.Test

class MapTableTest {

    @Test
    fun testApplyOffset() {
        val table = MapTable(rows = 2, cols = 2, initialData = intArrayOf(10, 20, 30, 40))
        val selection = SelectionArea(0, 0, 0, 1) // Pierwszy wiersz
        table.applyOffset(selection, 5)

        assertEquals(15, table.get(0, 0))
        assertEquals(25, table.get(0, 1))
        assertEquals(30, table.get(1, 0)) // Niezmienione
    }

    @Test
    fun testApplyPercentage() {
        val table = MapTable(rows = 2, cols = 2, initialData = intArrayOf(100, 200, 300, 400))
        val selection = SelectionArea(0, 0, 1, 0) // Pierwsza kolumna
        table.applyPercentage(selection, 10.0) // +10%

        assertEquals(110, table.get(0, 0))
        assertEquals(200, table.get(0, 1))
        assertEquals(330, table.get(1, 0))
    }

    @Test
    fun testSetValue() {
        val table = MapTable(rows = 2, cols = 2, initialData = intArrayOf(1, 2, 3, 4))
        val selection = SelectionArea(0, 0, 1, 1) // Cała mapa
        table.setValue(selection, 99)

        assertEquals(99, table.get(0, 0))
        assertEquals(99, table.get(1, 1))
    }

    @Test
    fun testSmoothing() {
        // Mapa 3x3 ze szpilką w centrum
        val data = intArrayOf(
            10, 10, 10,
            10, 100, 10,
            10, 10, 10
        )
        val table = MapTable(rows = 3, cols = 3, initialData = data)
        val selection = SelectionArea(1, 1, 1, 1) // Tylko środek
        table.applySmoothing(selection)

        // Średnia z 9 komórek: (8 * 10 + 100) / 9 = 180 / 9 = 20
        assertEquals(20, table.get(1, 1))
    }
}