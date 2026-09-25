package com.winols.app.data.buffer

import com.winols.app.domain.model.DataSize
import com.winols.app.domain.model.Endianness
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BinaryMemoryBuffer(private val bytes: ByteArray) {

    val size: Int get() = bytes.size

    fun readValue(offset: Int, size: DataSize, endianness: Endianness): Int {
        if (offset + size.byteCount > bytes.size) return 0
        val buffer = ByteBuffer.wrap(bytes, offset, size.byteCount)
        buffer.order(
            if (endianness == Endianness.LITTLE_ENDIAN) ByteOrder.LITTLE_ENDIAN 
            else ByteOrder.BIG_ENDIAN
        )

        return when (size) {
            DataSize.UBYTE -> buffer.get().toInt() and 0xFF
            DataSize.UWORD_16 -> buffer.short.toInt() and 0xFFFF
            DataSize.UDWORD_32 -> buffer.int
        }
    }

    fun writeValue(offset: Int, value: Int, size: DataSize, endianness: Endianness) {
        if (offset + size.byteCount > bytes.size) return
        val buffer = ByteBuffer.allocate(size.byteCount)
        buffer.order(
            if (endianness == Endianness.LITTLE_ENDIAN) ByteOrder.LITTLE_ENDIAN 
            else ByteOrder.BIG_ENDIAN
        )

        when (size) {
            DataSize.UBYTE -> buffer.put(value.toByte())
            DataSize.UWORD_16 -> buffer.putShort(value.toShort())
            DataSize.UDWORD_32 -> buffer.putInt(value)
        }
        System.arraycopy(buffer.array(), 0, bytes, offset, size.byteCount)
    }

    fun getRawBytes(): ByteArray = bytes.copyOf()
}