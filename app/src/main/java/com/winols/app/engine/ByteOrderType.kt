package com.winols.app.engine

enum class ByteOrderType {
    LITTLE_ENDIAN,
    BIG_ENDIAN
}

enum class DataWidth(val bytesCount: Int) {
    BYTE_8(1),
    WORD_16(2),
    DWORD_32(4)
}