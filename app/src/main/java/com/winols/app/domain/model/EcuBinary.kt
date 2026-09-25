package com.winols.app.domain.model

data class EcuBinary(
    val fileName: String,
    val sizeInBytes: Long,
    val rawBytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EcuBinary
        return rawBytes.contentEquals(other.rawBytes)
    }

    override fun hashCode(): Int {
        return rawBytes.contentHashCode()
    }
}