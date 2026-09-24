package com.winols.app.data

import java.nio.ByteOrder

enum class Endianness(val order: ByteOrder) {
    LITTLE_ENDIAN(ByteOrder.LITTLE_ENDIAN), // Lo/Hi (Intel)
    BIG_ENDIAN(ByteOrder.BIG_ENDIAN);       // Hi/Lo (Motorola)

    companion object {
        val LOHI = LITTLE_ENDIAN
        val HILO = BIG_ENDIAN
    }
}

enum class DataViewType(
    val byteLength: Int,
    val isSigned: Boolean,
    val displayName: String
) {
    UINT8(1, false, "8-bit Unsigned"),
    INT8(1, true, "8-bit Signed"),
    UINT16(2, false, "16-bit Unsigned"),
    INT16(2, true, "16-bit Signed"),
    HEX8(1, false, "8-bit HEX"),
    HEX16(2, false, "16-bit HEX");

    val isHex: Boolean
        get() = this == HEX8 || this == HEX16
}