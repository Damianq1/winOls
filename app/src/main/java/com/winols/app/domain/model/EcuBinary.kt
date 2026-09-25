package com.winols.app.domain.model

data class EcuBinary(
    val fileName: String,
    val data: ByteArray,
    val sizeInBytes: Int = data.size
) {
    fun readByte(offset: Int): Int {
        if (offset !in 0 until sizeInBytes) return 0
        return data[offset].toInt() and 0xFF
    }

    fun readWord16(offset: Int, isLittleEndian: Boolean = false): Int {
        if (offset + 1 >= sizeInBytes) return 0
        val b1 = data[offset].toInt() and 0xFF
        val b2 = data[offset + 1].toInt() and 0xFF
        return if (isLittleEndian) {
            (b2 shl 8) or b1
        } else {
            (b1 shl 8) or b2
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EcuBinary
        return data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        return data.contentHashCode()
    }
}