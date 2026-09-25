package com.winols.app.core.binary

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.ByteOrder

class EcuBinaryBufferTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testDirectBufferReadWriteAndEndianness() {
        val buffer = EcuBinaryBuffer.allocateDirect(1024 * 1024) // 1 MB wsad
        buffer.byteOrder = ByteOrder.BIG_ENDIAN

        val offset = 0x1000
        val testWord: Short = 0x1234
        buffer.writeShort(offset, testWord)

        assertEquals(0x12.toByte(), buffer.readByte(offset))
        assertEquals(0x34.toByte(), buffer.readByte(offset + 1))
        assertEquals(testWord, buffer.readShort(offset))

        // Sprawdzenie Little Endian
        buffer.byteOrder = ByteOrder.LITTLE_ENDIAN
        buffer.writeShort(offset, testWord)
        assertEquals(0x34.toByte(), buffer.readByte(offset))
        assertEquals(0x12.toByte(), buffer.readByte(offset + 1))
        assertEquals(testWord, buffer.readShort(offset))
    }

    @Test
    fun testFileStreamIoIntegrity() {
        val testSize = 2 * 1024 * 1024 // 2 MB (np. EDC16)
        val initialBuffer = EcuBinaryBuffer.allocateDirect(testSize)
        
        val testPattern = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
        initialBuffer.writeBytes(0x00010000, testPattern)

        val tempFile = tempFolder.newFile("ecu_dump.bin")
        initialBuffer.saveToFile(tempFile)

        val loadedBuffer = EcuBinaryBuffer.loadFromFile(tempFile)
        assertEquals(testSize, loadedBuffer.size)

        val readPattern = loadedBuffer.readBytes(0x00010000, 4)
        assertEquals(testPattern[0], readPattern[0])
        assertEquals(testPattern[1], readPattern[1])
        assertEquals(testPattern[2], readPattern[2])
        assertEquals(testPattern[3], readPattern[3])
    }
}