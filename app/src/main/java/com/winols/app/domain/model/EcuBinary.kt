package com.winols.app.domain.model

data class EcuBinary(
    val fileName: String,
    val sizeBytes: Int,
    val rawBytes: ByteArray,
    val checksum: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EcuBinary
        return rawBytes.contentEquals(other.rawBytes) && fileName == other.fileName
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + rawBytes.contentHashCode()
        return result
    }
}