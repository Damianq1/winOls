package com.winols.app.domain.model

data class EcuBinary(
    val fileName: String,
    val rawBytes: ByteArray,
    val size: Int = rawBytes.size
) {
    fun toHexPreview(maxBytes: Int = 512): String {
        val sb = StringBuilder()
        val limit = minOf(rawBytes.size, maxBytes)
        for (i in 0 until limit step 16) {
            sb.append(String.format("%08X: ", i))
            val lineBytes = rawBytes.sliceArray(i until minOf(i + 16, limit))
            for (b in lineBytes) {
                sb.append(String.format("%02X ", b))
            }
            sb.append("\n")
        }
        if (rawBytes.size > maxBytes) {
            sb.append("\n... [obcięto podgląd: łącznie ${rawBytes.size} bajtów]")
        }
        return sb.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EcuBinary
        return rawBytes.contentEquals(other.rawBytes)
    }

    override fun hashCode(): Int = rawBytes.contentHashCode()
}