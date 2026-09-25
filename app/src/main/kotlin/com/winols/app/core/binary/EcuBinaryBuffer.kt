package com.winols.app.core.binary

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Zoptymalizowany bufor binarny dla wsadów ECU.
 * Obsługuje przełączanie Endianness (Little/Big) i bezpieczny dostęp do rejestrów.
 */
class EcuBinaryBuffer private constructor(
    private var data: ByteArray,
    var byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN
) {
    val size: Int get() = data.size

    fun getByte(offset: Int): Int {
        checkBounds(offset, 1)
        return data[offset].toInt() and 0xFF
    }

    fun getWord(offset: Int, order: ByteOrder = byteOrder): Int {
        checkBounds(offset, 2)
        val buffer = ByteBuffer.wrap(data, offset, 2).order(order)
        return buffer.short.toInt() and 0xFFFF
    }

    fun getDWord(offset: Int, order: ByteOrder = byteOrder): Long {
        checkBounds(offset, 4)
        val buffer = ByteBuffer.wrap(data, offset, 4).order(order)
        return buffer.int.toLong() and 0xFFFFFFFFL
    }

    fun setByte(offset: Int, value: Int) {
        checkBounds(offset, 1)
        data[offset] = (value and 0xFF).toByte()
    }

    fun setWord(offset: Int, value: Int, order: ByteOrder = byteOrder) {
        checkBounds(offset, 2)
        val buffer = ByteBuffer.allocate(2).order(order)
        buffer.putShort((value and 0xFFFF).toShort())
        System.arraycopy(buffer.array(), 0, data, offset, 2)
    }

    fun slice(offset: Int, length: Int): ByteArray {
        checkBounds(offset, length)
        return data.copyOfRange(offset, offset + length)
    }

    fun getRawBytes(): ByteArray = data.copyOf()

    private fun checkBounds(offset: Int, length: Int) {
        require(offset >= 0 && offset + length <= data.size) {
            "Przekroczenie zakresu bufora ECU: offset=$offset, długość=$length, rozmiar bufora=${data.size}"
        }
    }

    companion object {
        fun fromBytes(bytes: ByteArray, byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN): EcuBinaryBuffer {
            return EcuBinaryBuffer(bytes.copyOf(), byteOrder)
        }
    }
}