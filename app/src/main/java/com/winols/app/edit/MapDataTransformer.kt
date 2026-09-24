package com.winols.app.edit

import com.winols.app.model.DataRepresentation
import com.winols.app.model.PhysicalConversion
import java.nio.ByteBuffer
import kotlin.math.roundToInt
import kotlin.math.roundToLong

object MapDataTransformer {

    fun readRawValue(buffer: ByteBuffer, offset: Int, representation: DataRepresentation): Long {
        buffer.order(representation.byteOrder)
        return when (representation.bitWidth) {
            8 -> {
                val b = buffer.get(offset)
                if (representation.isSigned) b.toLong() else (b.toInt() and 0xFF).toLong()
            }
            16 -> {
                val s = buffer.getShort(offset)
                if (representation.isSigned) s.toLong() else (s.toInt() and 0xFFFF).toLong()
            }
            32 -> {
                val i = buffer.getInt(offset)
                if (representation.isSigned) i.toLong() else (i.toLong() and 0xFFFFFFFFL)
            }
            else -> throw IllegalArgumentException("Nieobsługiwana szerokość bitowa: ${representation.bitWidth}")
        }
    }

    fun writeRawValue(buffer: ByteBuffer, offset: Int, rawValue: Long, representation: DataRepresentation) {
        buffer.order(representation.byteOrder)
        when (representation.bitWidth) {
            8 -> {
                val clamped = rawValue.coerceIn(
                    if (representation.isSigned) Byte.MIN_VALUE.toLong() else 0L,
                    if (representation.isSigned) Byte.MAX_VALUE.toLong() else 255L
                )
                buffer.put(offset, clamped.toByte())
            }
            16 -> {
                val clamped = rawValue.coerceIn(
                    if (representation.isSigned) Short.MIN_VALUE.toLong() else 0L,
                    if (representation.isSigned) Short.MAX_VALUE.toLong() else 65535L
                )
                buffer.putShort(offset, clamped.toShort())
            }
            32 -> {
                val clamped = rawValue.coerceIn(
                    if (representation.isSigned) Int.MIN_VALUE.toLong() else 0L,
                    if (representation.isSigned) Int.MAX_VALUE.toLong() else 4294967295L
                )
                buffer.putInt(offset, clamped.toInt())
            }
            else -> throw IllegalArgumentException("Nieobsługiwana szerokość bitowa: ${representation.bitWidth}")
        }
    }

    fun rawToPhysical(rawValue: Long, conversion: PhysicalConversion): Double {
        return (rawValue.toDouble() * conversion.factor) + conversion.offset
    }

    fun physicalToRaw(physicalValue: Double, conversion: PhysicalConversion): Long {
        if (conversion.factor == 0.0) return 0L
        return ((physicalValue - conversion.offset) / conversion.factor).roundToLong()
    }
}