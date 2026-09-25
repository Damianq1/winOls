package com.winols.app.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream

class IntelHexParserTest {

    @Test
    fun `parse valid intel hex with extended linear address`() {
        // Rekordy:
        // 1. Rozszerzony adres liniowy: 0x0001 (baza: 0x00010000)
        // 2. Dane: 4 bajty [0x11, 0x22, 0x33, 0x44] pod adres 0x0000
        // 3. EOF
        val hexContent = """
            :020000040001F9
            :040000001122334452
            :00000001FF
        """.trimIndent()

        val parser = IntelHexParser()
        val buffer = parser.parse(ByteArrayInputStream(hexContent.toByteArray()))

        assertEquals(0x00010000L, buffer.baseAddress)
        assertEquals(4, buffer.size)
        assertEquals(0x11, buffer.getByte(0x00010000L))
        assertEquals(0x22, buffer.getByte(0x00010001L))
        assertEquals(0x33, buffer.getByte(0x00010002L))
        assertEquals(0x44, buffer.getByte(0x00010003L))
    }

    @Test(expected = IllegalStateException::class)
    fun `parse hex with corrupted checksum throws exception`() {
        val corruptedHex = ":040000001122334400\n:00000001FF"
        val parser = IntelHexParser()
        parser.parse(ByteArrayInputStream(corruptedHex.toByteArray()))
    }
}