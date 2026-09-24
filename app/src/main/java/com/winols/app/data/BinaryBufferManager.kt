package com.winols.app.data

import java.nio.ByteBuffer
import java.nio.ByteOrder

class BinaryBufferManager(private val data: ByteArray) {

    private val undoStack = ArrayDeque<ByteArray>()
    private val redoStack = ArrayDeque<ByteArray>()

    val size: Int get() = data.size

    fun getBuffer(): ByteArray = data.copyOf()

    fun readRawValue(address: Int, size: Int, isBigEndian: Boolean = false): Long {
        require(address >= 0 && address + size <= data.size) { "Adres $address poza zakresem danych" }
        val buffer = ByteBuffer.wrap(data, address, size)
        buffer.order(if (isBigEndian) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN)

        return when (size) {
            1 -> buffer.get().toLong() and 0xFFL
            2 -> buffer.short.toLong() and 0xFFFFL
            4 -> buffer.int.toLong() and 0xFFFFFFFFL
            else -> throw IllegalArgumentException("Nieobsługiwany rozmiar słowa: $size")
        }
    }

    fun readSignedValue(address: Int, size: Int, isBigEndian: Boolean = false): Long {
        require(address >= 0 && address + size <= data.size) { "Adres $address poza zakresem danych" }
        val buffer = ByteBuffer.wrap(data, address, size)
        buffer.order(if (isBigEndian) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN)

        return when (size) {
            1 -> buffer.get().toLong()
            2 -> buffer.short.toLong()
            4 -> buffer.int.toLong()
            else -> throw IllegalArgumentException("Nieobsługiwany rozmiar słowa: $size")
        }
    }

    fun writeRawValue(address: Int, value: Long, size: Int, isBigEndian: Boolean = false) {
        require(address >= 0 && address + size <= data.size) { "Adres $address poza zakresem danych" }
        val buffer = ByteBuffer.allocate(size)
        buffer.order(if (isBigEndian) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN)

        when (size) {
            1 -> buffer.put((value and 0xFFL).toByte())
            2 -> buffer.putShort((value and 0xFFFFL).toShort())
            4 -> buffer.putInt((value and 0xFFFFFFFFL).toInt())
            else -> throw IllegalArgumentException("Nieobsługiwany rozmiar słowa: $size")
        }

        System.arraycopy(buffer.array(), 0, data, address, size)
    }

    fun pushSnapshot() {
        undoStack.addLast(data.copyOf())
        redoStack.clear()
    }

    fun undo(): Boolean {
        if (undoStack.isEmpty()) return false
        redoStack.addLast(data.copyOf())
        val previousState = undoStack.removeLast()
        System.arraycopy(previousState, 0, data, 0, data.size)
        return true
    }

    fun redo(): Boolean {
        if (redoStack.isEmpty()) return false
        undoStack.addLast(data.copyOf())
        val nextState = redoStack.removeLast()
        System.arraycopy(nextState, 0, data, 0, data.size)
        return true
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()
}