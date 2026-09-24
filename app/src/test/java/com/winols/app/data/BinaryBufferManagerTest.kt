package com.winols.app.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.ByteOrder

class BinaryBufferManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testFileLoadAndSaveDirectBuffer() {
        val size = 2 * 1024 * 1024 // 2 MB
        val sampleData = ByteArray(size) { (it % 256).toByte() }

        val inputFile = tempFolder.newFile("sample_2mb.bin")
        inputFile.writeBytes(sampleData)

        val manager = BinaryBufferManager()
        manager.loadFile(inputFile)

        assertEquals(size, manager.capacity)
        assertArrayEquals(
            byteArrayOf(sampleData[0], sampleData[1], sampleData[2]),
            manager.readBytes(0, 3)
        )

        // Modyfikacja pojedynczego słowa
        manager.putUShort(0x1000, 0xABCD)
        assertEquals(0xABCD, manager.getUShort(0x1000))

        val outputFile = tempFolder.newFile("sample_saved.bin")
        manager.saveFile(outputFile)

        val readBack = outputFile.readBytes()
        assertEquals(size, readBack.size)
        assertEquals(0xCD.toByte(), readBack[0x1000])
        assertEquals(0xAB.toByte(), readBack[0x1001])
    }

    @Test
    fun testByteOrderHandling() {
        val manager = BinaryBufferManager()
        manager.allocate(4)

        // Domyślnie Little Endian
        manager.setByteOrder(ByteOrder.LITTLE_ENDIAN)
        manager.putShort(0, 0x1234.toShort())
        assertEquals(0x34.toByte(), manager.getByte(0))
        assertEquals(0x12.toByte(), manager.getByte(1))

        // Big Endian
        manager.setByteOrder(ByteOrder.BIG_ENDIAN)
        manager.putShort(0, 0x1234.toShort())
        assertEquals(0x12.toByte(), manager.getByte(0))
        assertEquals(0x34.toByte(), manager.getByte(1))
    }
}