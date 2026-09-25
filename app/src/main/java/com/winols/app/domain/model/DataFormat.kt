package com.winols.app.domain.model

enum class DataFormat(val byteSize: Int, val isSigned: Boolean, val minValue: Long, val maxValue: Long) {
    UBYTE(1, false, 0L, 255L),
    SBYTE(1, true, -128L, 127L),
    UWORD_LE(2, false, 0L, 65535L),
    UWORD_BE(2, false, 0L, 65535L),
    SWORD_LE(2, true, -32768L, 32767L),
    SWORD_BE(2, true, -32768L, 32767L),
    ULONG_LE(4, false, 0L, 4294967295L),
    ULONG_BE(4, false, 0L, 4294967295L),
    SLONG_LE(4, true, -2147483648L, 2147483647L),
    SLONG_BE(4, true, -2147483648L, 2147483647L)
}