package com.winols.app.engine

import java.nio.ByteBuffer
import java.nio.ByteOrder

class BinaryProject(private val rawBuffer: ByteArray) {
    private val buffer: ByteBuffer = ByteBuffer.wrap(rawBuffer)

    val size: Int get() = rawBuffer.size

    fun readValue(
        address: Int,
        width: DataWidth,
        order: ByteOrderType,
        isSigned: Boolean,
        factor: Double = 1.0,
        offset: Double = 0.0
    ): Double {
        if (address < 0 || address + width.bytesCount > rawBuffer.size) return 0.0
        
        buffer.order(if (order == ByteOrderType.LITTLE_ENDIAN) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)
        
        val rawValue: Long = when (width) {
            DataWidth.BYTE_8 -> {
                val b = buffer.get(address).toLong()
                if (isSigned) b else b and 0xFFL
            }
            DataWidth.WORD_16 -> {
                val s = buffer.getShort(address).toLong()
                if (isSigned) s else s and 0xFFFFL
            }
            DataWidth.DWORD_32 -> {
                val i = buffer.getInt(address).toLong()
                if (isSigned) i else i and 0xFFFFFFFFL
            }
        }
        return (rawValue * factor) + offset
    }

    fun writeValue(
        address: Int,
        value: Double,
        width: DataWidth,
        order: ByteOrderType,
        factor: Double = 1.0,
        offset: Double = 0.0
    ) {
        if (address < 0 || address + width.bytesCount > rawBuffer.size) return
        
        val rawTarget = if (factor != 0.0) ((value - offset) / factor).toLong() else 0L
        buffer.order(if (order == ByteOrderType.LITTLE_ENDIAN) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)

        when (width) {
            DataWidth.BYTE_8 -> buffer.put(address, rawTarget.toByte())
            DataWidth.WORD_16 -> buffer.putShort(address, rawTarget.toShort())
            DataWidth.DWORD_32 -> buffer.putInt(address, rawTarget.toInt())
        }
    }

    fun extractMapTable(map: EcuMapDefinition): Array<DoubleArray> {
        val result = Array(map.rows) { DoubleArray(map.columns) }
        var currentAddr = map.address
        
        for (r in 0 until map.rows) {
            for (c in 0 until map.columns) {
                result[r][c] = readValue(
                    address = currentAddr,
                    width = map.dataWidth,
                    order = map.byteOrder,
                    isSigned = map.isSigned,
                    factor = map.factor,
                    offset = map.offset
                )
                currentAddr += map.dataWidth.bytesCount
            }
        }
        return result
    }

    fun getRawBytes(): ByteArray = rawBuffer
}