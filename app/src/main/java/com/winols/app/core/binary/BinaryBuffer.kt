package com.winols.app.core.binary

import java.nio.ByteBuffer
import java.nio.ByteOrder

class BinaryBuffer private constructor(private var buffer: ByteBuffer) {

    var order: ByteOrder
        get() = buffer.order()
        set(value) { buffer.order(value) }

    val size: Int
        get() = buffer.capacity()

    fun getByte(offset: Int): Byte = buffer.get(offset)

    fun getShort(offset: Int): Short = buffer.getShort(offset)

    fun getInt(offset: Int): Int = buffer.getInt(offset)

    fun setByte(offset: Int, value: Byte) {
        buffer.put(offset, value)
    }

    fun setShort(offset: Int, value: Short) {
        buffer.putShort(offset, value)
    }

    fun readWindow(offset: Int, length: Int): ByteArray {
        val safeLen = length.coerceAtMost(size - offset).coerceAtLeast(0)
        val dst = ByteArray(safeLen)
        val slice = buffer.duplicate()
        slice.position(offset)
        slice.get(dst, 0, safeLen)
        return dst
    }

    fun toByteArray(): ByteArray {
        val out = ByteArray(buffer.capacity())
        buffer.duplicate().apply {
            position(0)
            get(out)
        }
        return out
    }

    companion object {
        fun fromBytes(bytes: ByteArray, byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN): BinaryBuffer {
            val directBuffer = ByteBuffer.allocateDirect(bytes.size).order(byteOrder)
            directBuffer.put(bytes)
            directBuffer.flip()
            return BinaryBuffer(directBuffer)
        }
    }
}