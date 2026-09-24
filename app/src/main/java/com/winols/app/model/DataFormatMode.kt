package com.winols.app.model

import java.util.Locale

enum class NumberBase {
    HEX,
    DECIMAL_UNSIGNED,
    DECIMAL_SIGNED
}

enum class WordWidth(val byteSize: Int) {
    BYTE_8(1),
    WORD_16(2),
    DWORD_32(4)
}

enum class Endianness {
    LITTLE_ENDIAN, // Lo/Hi
    BIG_ENDIAN     // Hi/Lo
}

data class DataDisplayConfig(
    val base: NumberBase = NumberBase.HEX,
    val wordWidth: WordWidth = WordWidth.WORD_16,
    val endianness: Endianness = Endianness.LITTLE_ENDIAN,
    val applyFormulas: Boolean = false // Wartości inżynierskie (Factor/Offset) vs Surowe
) {
    /**
     * Mapuje bieżącą konfigurację wyświetlania na natywny typ danych w buforze binarnym.
     */
    fun toDataType(signed: Boolean = base == NumberBase.DECIMAL_SIGNED): DataType {
        return when (wordWidth) {
            WordWidth.BYTE_8 -> if (signed) DataType.SBYTE else DataType.UBYTE
            WordWidth.WORD_16 -> when {
                endianness == Endianness.LITTLE_ENDIAN && signed -> DataType.SWORD_LE
                endianness == Endianness.LITTLE_ENDIAN && !signed -> DataType.UWORD_LE
                endianness == Endianness.BIG_ENDIAN && signed -> DataType.SWORD_BE
                else -> DataType.UWORD_BE
            }
            WordWidth.DWORD_32 -> when {
                endianness == Endianness.LITTLE_ENDIAN && signed -> DataType.SDWORD_LE
                endianness == Endianness.LITTLE_ENDIAN && !signed -> DataType.UDWORD_LE
                endianness == Endianness.BIG_ENDIAN && signed -> DataType.SDWORD_BE
                else -> DataType.UDWORD_BE
            }
        }
    }

    /**
     * Formatuje surową wartość liczbową do zadanego formatu tekstowego.
     */
    fun formatValue(rawValue: Double, engineeringValue: Double? = null): String {
        if (applyFormulas && engineeringValue != null) {
            return String.format(Locale.US, "%.2f", engineeringValue)
        }

        return when (base) {
            NumberBase.HEX -> {
                when (wordWidth) {
                    WordWidth.BYTE_8 -> String.format(Locale.US, "%02X", rawValue.toInt() and 0xFF)
                    WordWidth.WORD_16 -> String.format(Locale.US, "%04X", rawValue.toInt() and 0xFFFF)
                    WordWidth.DWORD_32 -> String.format(Locale.US, "%08X", rawValue.toLong() and 0xFFFFFFFFL)
                }
            }
            NumberBase.DECIMAL_UNSIGNED -> {
                when (wordWidth) {
                    WordWidth.BYTE_8 -> (rawValue.toInt() and 0xFF).toString()
                    WordWidth.WORD_16 -> (rawValue.toInt() and 0xFFFF).toString()
                    WordWidth.DWORD_32 -> (rawValue.toLong() and 0xFFFFFFFFL).toString()
                }
            }
            NumberBase.DECIMAL_SIGNED -> {
                when (wordWidth) {
                    WordWidth.BYTE_8 -> rawValue.toInt().toByte().toString()
                    WordWidth.WORD_16 -> rawValue.toInt().toShort().toString()
                    WordWidth.DWORD_32 -> rawValue.toLong().toInt().toString()
                }
            }
        }
    }
}