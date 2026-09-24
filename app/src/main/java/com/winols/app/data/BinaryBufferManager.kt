package com.winols.app.data

import com.winols.app.model.DataType
import com.winols.app.model.Endianness
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BinaryBufferManager(private val buffer: ByteArray) {

    val size: Int get() = buffer.size

    constructor(file: File) : this(file.readBytes())

    fun readByte(offset: Long): Byte {
        checkBounds(offset, 1)
        return buffer[offset.toInt()]
    }

    fun readValue(offset: Long, dataType: DataType, endianness: Endianness): Double {
        checkBounds(offset, dataType.byteSize)
        val idx = offset.toInt()
        val byteBuf = ByteBuffer.wrap(buffer, idx, dataType.byteSize)
        byteBuf.order(if (endianness == Endianness.LITTLE_ENDIAN) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)

        return when (dataType) {
            DataType.UINT8 -> (buffer[idx].toInt() and 0xFF).toDouble()
            DataType.INT8 -> buffer[idx].toDouble()
            DataType.UINT16 -> (byteBuf.short.toInt() and 0xFFFF).toDouble()
            DataType.INT16 -> byteBuf.short.toDouble()
            DataType.UINT32 -> (byteBuf.int.toLong() and 0xFFFFFFFFL).toDouble()
            DataType.INT32 -> byteBuf.int.toDouble()
        }
    }

    fun writeValue(offset: Long, value: Double, dataType: DataType, endianness: Endianness) {
        checkBounds(offset, dataType.byteSize)
        val idx = offset.toInt()
        val byteBuf = ByteBuffer.allocate(dataType.byteSize)
        byteBuf.order(if (endianness == Endianness.LITTLE_ENDIAN) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)

        when (dataType) {
            DataType.UINT8 -> {
                buffer[idx] = (value.toInt() and 0xFF).toByte()
            }
            DataType.INT8 -> {
                buffer[idx] = value.toInt().toByte()
            }
            DataType.UINT16 -> {
                byteBuf.putShort((value.toInt() and 0xFFFF).toShort())
                System.arraycopy(byteBuf.array(), 0, buffer, idx, 2)
            }
            DataType.INT16 -> {
                byteBuf.putShort(value.toInt().toShort())
                System.arraycopy(byteBuf.array(), 0, buffer, idx, 2)
            }
            DataType.UINT32 -> {
                byteBuf.putInt((value.toLong() and 0xFFFFFFFFL).toInt())
                System.arraycopy(byteBuf.array(), 0, buffer, idx, 4)
            }Oto szkielet architektoniczny i implementacja kluczowych modułów silnika WinOLS w Kotlinie, oparta na wykrytych w projekcie plikach (`BinaryBufferManager`, `MapDefinition`, `MapEditor`, `MapFinderEngine`, `ChecksumEngine`, `BinModel`).

---

### app/src/main/java/com/winols/app/data/BinaryBufferManager.kt