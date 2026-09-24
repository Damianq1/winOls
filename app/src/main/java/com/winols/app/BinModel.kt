package com.winols.app

import com.winols.app.data.BinaryBufferManager
import com.winols.app.model.BitWidth
import com.winols.app.model.DataRepresentation
import com.winols.app.model.Signedness
import java.nio.ByteOrder

/**
 * Model stanu pliku binarnego łączący bufor z parametrami widoku i reprezentacją danych.
 */
data class BinModel(
    val bufferManager: BinaryBufferManager,
    var representation: DataRepresentation = DataRepresentation(
        bitWidth = BitWidth.BITS_16,
        signedness = Signedness.UNSIGNED,
        byteOrder = ByteOrder.LITTLE_ENDIAN
    ),
    var activeAddress: Int = 0,
    var columns: Int = 16
) {
    val totalBytes: Int get() = bufferManager.size

    fun toggleEndianness() {
        val newOrder = if (representation.byteOrder == ByteOrder.LITTLE_ENDIAN) {
            ByteOrder.BIG_ENDIAN
        } else {
            ByteOrder.LITTLE_ENDIAN
        }
        representation = representation.copy(byteOrder = newOrder)
    }

    fun toggleBitWidth() {
        val nextWidth = when (representation.bitWidth) {
            BitWidth.BITS_8 -> BitWidth.BITS_16
            BitWidth.BITS_16 -> BitWidth.BITS_32
            BitWidth.BITS_32 -> BitWidth.BITS_8
        }
        representation = representation.copy(bitWidth = nextWidth)
    }

    fun toggleSignedness() {
        val nextSign = if (representation.signedness == Signedness.UNSIGNED) {
            Signedness.SIGNED
        } else {
            Signedness.UNSIGNED
        }
        representation = representation.copy(signedness = nextSign)
    }
}