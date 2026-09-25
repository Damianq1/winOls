package com.winols.app.domain.model

enum class Endianness {
    LITTLE_ENDIAN,
    BIG_ENDIAN
}

enum class DataSize(val byteCount: Int) {
    UBYTE(1),
    UWORD_16(2),
    UDWORD_32(4)
}

data class EcuMapDefinition(
    val id: String,
    val name: String,
    val startAddress: Int,
    val rows: Int,
    val columns: Int,
    val dataSize: DataSize = DataSize.UWORD_16,
    val endianness: Endianness = Endianness.BIG_ENDIAN,
    val factor: Double = 1.0,
    val offset: Double = 0.0
)

data class EcuBinaryFile(
    val fileName: String,
    val rawBytes: ByteArray,
    val size: Int = rawBytes.size
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EcuBinaryFile
        return rawBytes.contentEquals(other.rawBytes)
    }

    override fun hashCode(): Int = rawBytes.contentHashCode()
}