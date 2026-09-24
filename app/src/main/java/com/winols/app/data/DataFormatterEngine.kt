package com.winols.app.data

import com.winols.app.model.ConversionProfile
import com.winols.app.model.DataRepresentation
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Silnik odpowiedzialny za dwustronną translację pomiędzy surowymi bajtami w buforze,
 * a wyliczonymi wartościami fizycznymi prezentowanymi w tabelach MapGridView i HexGridView.
 */
class DataFormatterEngine {

    /**
     * Odczytuje wartość z bufora pod wskazanym adresem i przelicza na wartość fizyczną.
     */
    fun readPhysicalValue(
        buffer: ByteBuffer,
        offset: Int,
        representation: DataRepresentation,
        profile: ConversionProfile
    ): Double {
        val originalOrder = buffer.order()
        buffer.order(if (representation.isBigEndian) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN)

        val raw: Long = when (representation) {
            DataRepresentation.UBYTE -> buffer.get(offset).toUByte().toLong()
            DataRepresentation.SBYTE -> buffer.get(offset).toLong()
            DataRepresentation.UWORD_LE, DataRepresentation.UWORD_BE -> buffer.getShort(offset).toUShort().toLong()
            DataRepresentation.SWORD_LE, DataRepresentation.SWORD_BE -> buffer.getShort(offset).toLong()
            DataRepresentation.UDWORD_LE, DataRepresentation.UDWORD_BE -> buffer.getInt(offset).toUInt().toLong()
            DataRepresentation.SDWORD_LE, DataRepresentation.SDWORD_BE -> buffer.getInt(offset).toLong()
        }

        buffer.order(originalOrder)
        return profile.rawToPhysical(raw)
    }

    /**
     * Konwertuje wprowadzoną przez użytkownika wartość fizyczną do formatu binarnego i zapisuje do bufora.
     */
    fun writePhysicalValue(
        buffer: ByteBuffer,
        offset: Int,
        physicalValue: Double,
        representation: DataRepresentation,
        profile: ConversionProfile
    ) {
        val rawValue = profile.physicalToRaw(physicalValue, representation)
        val originalOrder = buffer.order()
        buffer.order(if (representation.isBigEndian) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN)

        when (representation) {
            DataRepresentation.UBYTE, DataRepresentation.SBYTE -> {
                buffer.put(offset, rawValue.toByte())
            }
            DataRepresentation.UWORD_LE, DataRepresentation.UWORD_BE,
            DataRepresentation.SWORD_LE, DataRepresentation.SWORD_BE -> {
                buffer.putShort(offset, rawValue.toShort())
            }
            DataRepresentation.UDWORD_LE, DataRepresentation.UDWORD_BE,
            DataRepresentation.SDWORD_LE, DataRepresentation.SDWORD_BE -> {
                buffer.putInt(offset, rawValue.toInt())
            }
        }

        buffer.order(originalOrder)
    }

    /**
     * Parsowanie wejścia tekstowego z maską wzoru (np. podawanego w definicji mapy).
     * Wspiera standardowe formaty parametrów factor i offset:
     * - `x * 0.01`
     * - `(x * 0.25) - 40`
     * - `x * 0.1 + 0`
     */
    fun parseFormula(expression: String, unit: String = "", decimalPlaces: Int = 2): ConversionProfile {
        val sanitized = expression.replace(" ", "").lowercase(java.util.Locale.US)
        
        // Prosty regex dla form: (x*FACTOR)+OFFSET, (x*FACTOR)-OFFSET, x*FACTOR+OFFSET, x*FACTOR
        val regex = Regex("""(?:\(?x\*([0-9]*\.?[0-9]+)\)?)?([+-][0-9]*\.?[0-9]+)?""")
        val match = regex.find(sanitized)

        if (match != null) {
            val factorStr = match.groups[1]?.value
            val offsetStr = match.groups[2]?.value

            val factor = factorStr?.toDoubleOrNull() ?: 1.0
            val offset = offsetStr?.toDoubleOrNull() ?: 0.0

            return ConversionProfile(factor = factor, offset = offset, unit = unit, decimalPlaces = decimalPlaces)
        }

        return ConversionProfile(1.0, 0.0, unit, decimalPlaces)
    }
}