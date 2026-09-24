package com.winols.app.data

import com.winols.app.model.BitWidth
import com.winols.app.model.Endianness
import com.winols.app.model.HexViewConfig
import com.winols.app.model.SignMode
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BinaryBufferManager(private var data: ByteArray) {

    val size: Int
        get() = data.size

    fun getRawBytes(): ByteArray = data

    fun readValue(offset: Int, config: HexViewConfig): Number {
        return readValue(offset, config.bitWidth, config.endianness, config.signMode)
    }

    fun readValue(
        offset: Int,
        bitWidth: BitWidth,
        endianness: Endianness,
        signMode: SignMode
    ): Number {
        require(offset in 0..(data.size - bitWidth.bytesCount)) {
            "Offset $offset out of bounds for buffer of size ${data.size}"
        }

        val buffer = ByteBuffer.wrap(data, offset, bitWidth.bytesCount).apply {
            order(endianness.byteOrder)
        }

        return when (bitWidth) {
            BitWidth.BIT_8 -> {
                val b = buffer.get()
                if (signMode == SignMode.SIGNED) b else b.toUByte().toInt()
            }
            BitWidth.BIT_16 -> {
                val s = buffer.short
                if (signMode == SignMode.SIGNED) s else s.toUShort().toInt()
            }
            BitWidth.BIT_32 -> {
                val i = buffer.int
                if (signMode == SignMode.SIGNED) i else i.toUInt().toLong()
            }
        }
    }

    fun writeValue(offset: Int, value: Number, config: HexViewConfig) {
        writeValue(offset, value, config.bitWidth, config.endianness, config.signMode)
    }

    fun writeValue(
        offset: Int,
        value: Number,
        bitWidth: BitWidth,
        endianness: Endianness,
        signMode: SignMode
    ) {
        require(offset in 0..(data.size - bitWidth.bytesCount)) {
            "Offset $offset out of bounds for buffer of size ${data.size}"
        }

        val buffer = ByteBuffer.allocate(bitWidth.bytesCount).apply {
            order(endianness.byteOrder)
        }

        when (bitWidth) {
            BitWidth.BIT_8 -> {
                val byteVal = value.toLong().toByte()
                buffer.put(byteVal)
            }
            BitWidth.BIT_16 -> {
                val shortVal = value.toLong().toShort()
                buffer.putShort(shortVal)
            }
            BitWidth.BIT_32 -> {
                val intVal = value.toLong().toInt()
                buffer.putInt(intVal)
            }
        }

        System.arraycopy(buffer.array(), 0, data, offset, bitWidth.bytesCount)
    }

    fun readHexDumpLine(startOffset: Int, bytesPerLine: Int, config: HexViewConfig): List<String> {
        val result = mutableListOf<String>()
        val step = config.bitWidth.bytesCount
        var curr = startOffset
        val limit = (startOffset + bytesPerLine).coerceAtMost(data.size)

        while (curr + step <= limit) {
            val raw = readValue(curr, config.bitWidth, config.endianness, SignMode.UNSIGNED).toLong()
            result.add(config.formatRawValue(raw))
            curr += step
        }
        return result
    }
}