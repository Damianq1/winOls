package com.winols.app.data

import com.winols.app.model.DataType
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BinaryBufferManager private constructor(
    private var buffer: ByteBuffer,
    val capacity: Int
) {
    private val originalSnapshot: ByteBuffer = ByteBuffer.allocateDirect(capacity).apply {
        buffer.rewind()
        put(buffer)
        buffer.rewind()
        rewind()
    }

    private val undoStack = ArrayDeque<ByteArray>()
    private val redoStack = ArrayDeque<ByteArray>()

    val size: Int get() = buffer.capacity()

    val isModified: Boolean
        get() = undoStack.isNotEmpty() || hasUnsavedDifferences()

    fun readNumeric(
        address: Int,
        dataType: DataType,
        byteOrder: ByteOrder = dataType.byteOrder
    ): Number {
        validateBounds(address, dataType.byteSize)
        buffer.order(byteOrder)
        return when (dataType) {
            DataType.UBYTE -> buffer.get(address).toInt() and 0xFF
            DataType.SBYTE -> buffer.get(address).toInt()
            DataType.UWORD_LE, DataType.UWORD_BE -> buffer.getShort(address).toInt() and 0xFFFF
            DataType.SWORD_LE, DataType.SWORD_BE -> buffer.getShort(address).toInt()
            DataType.UDWORD_LE, DataType.UDWORD_BE -> buffer.getInt(address).toLong() and 0xFFFFFFFFL
            DataType.SDWORD_LE, DataType.SDWORD_BE -> buffer.getInt(address)
            DataType.FLOAT_LE, DataType.FLOAT_BE -> buffer.getFloat(address)
        }
    }

    fun writeNumeric(
        address: Int,
        value: Number,
        dataType: DataType,
        byteOrder: ByteOrder = dataType.byteOrder,
        recordUndo: Boolean = true
    ) {
        validateBounds(address, dataType.byteSize)
        if (recordUndo) {
            pushUndoSnapshot()
        }

        buffer.order(byteOrder)
        when (dataType) {
            DataType.UBYTE, DataType.SBYTE -> buffer.put(address, value.toByte())
            DataType.UWORD_LE, DataType.UWORD_BE,
            DataType.SWORD_LE, DataType.SWORD_BE -> buffer.putShort(address, value.toShort())
            DataType.UDWORD_LE, DataType.UDWORD_BE,
            DataType.SDWORD_LE, DataType.SDWORD_BE -> buffer.putInt(address, value.toInt())
            DataType.FLOAT_LE, DataType.FLOAT_BE -> buffer.putFloat(address, value.toFloat())
        }
    }

    fun getByte(address: Int): Byte {
        validateBounds(address, 1)
        return buffer.get(address)
    }

    fun getOriginalByte(address: Int): Byte {
        validateBounds(address, 1)
        return originalSnapshot.get(address)
    }

    fun setByte(address: Int, value: Byte, recordUndo: Boolean = true) {
        validateBounds(address, 1)
        if (recordUndo) {
            pushUndoSnapshot()
        }
        buffer.put(address, value)
    }

    fun isAddressModified(address: Int): Boolean {
        validateBounds(address, 1)
        return buffer.get(address) != originalSnapshot.get(address)
    }

    fun getModifiedAddresses(): List<Int> {
        val diffList = ArrayList<Int>(1024)
        for (i in 0 until buffer.capacity()) {
            if (buffer.get(i) != originalSnapshot.get(i)) {
                diffList.add(i)
            }
        }
        return diffList
    }

    fun undo(): Boolean {
        if (undoStack.isEmpty()) return false
        redoStack.addLast(exportDirectToByteArray(buffer))
        val previousState = undoStack.removeLast()
        loadByteArrayToDirect(previousState, buffer)
        return true
    }

    fun redo(): Boolean {
        if (redoStack.isEmpty()) return false
        undoStack.addLast(exportDirectToByteArray(buffer))
        val nextState = redoStack.removeLast()
        loadByteArrayToDirect(nextState, buffer)
        return true
    }

    fun saveToFile(destination: File) {
        RandomAccessFile(destination, "rw").use { raf ->
            raf.channel.use { channel ->
                channel.truncate(0)
                val duplicate = buffer.duplicate()
                duplicate.rewind()
                channel.write(duplicate)
            }
        }
        originalSnapshot.rewind()
        val duplicate = buffer.duplicate()
        duplicate.rewind()
        originalSnapshot.put(duplicate)
        originalSnapshot.rewind()
        undoStack.clear()
        redoStack.clear()
    }

    fun getRawBytes(): ByteArray = exportDirectToByteArray(buffer)
    fun getOriginalBytes(): ByteArray = exportDirectToByteArray(originalSnapshot)

    private fun pushUndoSnapshot() {
        if (undoStack.size >= MAX_UNDO_DEPTH) {
            undoStack.removeFirst()
        }
        undoStack.addLast(exportDirectToByteArray(buffer))
        redoStack.clear()
    }

    private fun hasUnsavedDifferences(): Boolean {
        for (i in 0 until buffer.capacity()) {
            if (buffer.get(i) != originalSnapshot.get(i)) return true
        }
        return false
    }

    private fun validateBounds(address: Int, length: Int) {
        if (address < 0 || address + length > buffer.capacity()) {
            throw IndexOutOfBoundsException("Address 0x${address.toString(16).uppercase()} with length $length exceeds buffer bounds (${buffer.capacity()})")
        }
    }

    private fun exportDirectToByteArray(src: ByteBuffer): ByteArray {
        val array = ByteArray(src.capacity())
        val duplicate = src.duplicate()
        duplicate.rewind()
        duplicate.get(array)
        return array
    }

    private fun loadByteArrayToDirect(src: ByteArray, dest: ByteBuffer) {
        dest.rewind()
        dest.put(src)
        dest.rewind()
    }

    companion object {
        private const val MAX_UNDO_DEPTH = 50

        @JvmStatic
        fun fromFile(file: File): BinaryBufferManager {
            val length = file.length().toInt()
            val directBuffer = ByteBuffer.allocateDirect(length)
            RandomAccessFile(file, "r").use { raf ->
                raf.channel.use { channel ->
                    var bytesRead = 0
                    while (bytesRead < length) {
                        val read = channel.read(directBuffer)
                        if (read == -1) break
                        bytesRead += read
                    }
                }
            }
            directBuffer.rewind()
            return BinaryBufferManager(directBuffer, length)
        }

        @JvmStatic
        fun fromByteArray(data: ByteArray): BinaryBufferManager {
            val directBuffer = ByteBuffer.allocateDirect(data.size)
            directBuffer.put(data)
            directBuffer.rewind()
            return BinaryBufferManager(directBuffer, data.size)
        }
    }
}