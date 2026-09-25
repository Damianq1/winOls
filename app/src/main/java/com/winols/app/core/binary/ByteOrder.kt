package com.winols.app.core.binary

enum class DataEndianness {
    LITTLE_ENDIAN, // LoHi (np. większość x86, niektóre Renesas/TriCore)
    BIG_ENDIAN     // HiLo (np. Bosch EDC15/EDC16 Motorola MPC5xx)
}

enum class ValueType(val byteSize: Int) {
    UBYTE(1),
    SBYTE(1),
    UWORD_16(2),
    SWORD_16(2),
    UDWORD_32(4),
    SDWORD_32(4)
}