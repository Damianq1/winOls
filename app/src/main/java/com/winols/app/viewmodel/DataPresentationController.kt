package com.winols.app.viewmodel

import com.winols.app.data.DataDecoder
import com.winols.app.model.BitWidth
import com.winols.app.model.DataFormatConfig
import com.winols.app.model.DisplayRadix
import com.winols.app.model.MapData3D
import com.winols.app.model.Point2D
import com.winols.app.model.Signedness
import com.winols.app.model.ViewMode
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Kontroler zarządzający stanem prezentacji widoku, przełączaniem trybów oraz formatowaniem.
 */
class DataPresentationController(
    private var buffer: ByteBuffer
) {
    var viewMode: ViewMode = ViewMode.HEX_VIEW
        private set

    var formatConfig: DataFormatConfig = DataFormatConfig()
        private set

    fun updateBuffer(newBuffer: ByteBuffer) {
        this.buffer = newBuffer
    }

    // --- Szybkie przełączniki widoków i trybów ---

    fun setViewMode(mode: ViewMode) {
        this.viewMode = mode
    }

    fun toggleBitWidth() {
        val nextWidth = when (formatConfig.bitWidth) {
            BitWidth.BIT_8 -> BitWidth.BIT_16
            BitWidth.BIT_16 -> BitWidth.BIT_32
            BitWidth.BIT_32 -> BitWidth.BIT_8
        }
        formatConfig = formatConfig.copy(bitWidth = nextWidth)
    }

    fun toggleSignedness() {
        val nextSign = if (formatConfig.signedness == Signedness.SIGNED) {
            Signedness.UNSIGNED
        } else {
            Signedness.SIGNED
        }
        formatConfig = formatConfig.copy(signedness = nextSign)
    }

    fun toggleByteOrder() {
        val nextOrder = if (formatConfig.byteOrder == ByteOrder.BIG_ENDIAN) {
            ByteOrder.LITTLE_ENDIAN
        } else {
            ByteOrder.BIG_ENDIAN
        }
        formatConfig = formatConfig.copy(byteOrder = nextOrder)
    }

    fun toggleRadix() {
        val nextRadix = when (formatConfig.radix) {
            DisplayRadix.HEX -> DisplayRadix.DECIMAL
            DisplayRadix.DECIMAL -> DisplayRadix.BINARY
            DisplayRadix.BINARY -> DisplayRadix.HEX
        }
        formatConfig = formatConfig.copy(radix = nextRadix)
    }

    fun setFormat(bitWidth: BitWidth, signedness: Signedness, byteOrder: ByteOrder, radix: DisplayRadix) {
        formatConfig = formatConfig.copy(
            bitWidth = bitWidth,
            signedness = signedness,
            byteOrder = byteOrder,
            radix = radix
        )
    }

    // --- Pobieranie danych do wizualizacji ---

    /**
     * Zwraca dane dla podglądu HEX/tekstowego dla danej linijki bajtów.
     */
    fun getFormattedHexRow(startOffset: Int, bytesPerRow: Int = 16): List<String> {
        val values = mutableListOf<String>()
        val step = formatConfig.bitWidth.bytes
        var current = startOffset

        while (current < startOffset + bytesPerRow && current + step <= buffer.capacity()) {
            val raw = DataDecoder.readRawValue(buffer, current, formatConfig)
            values.add(DataDecoder.formatToString(raw, formatConfig))
            current += step
        }
        return values
    }

    /**
     * Generuje serię punktów dla wykresu fali 2D w zadanym zakresie.
     */
    fun extract2DProfile(startOffset: Int, lengthInBytes: Int): List<Point2D> {
        val points = mutableListOf<Point2D>()
        val step = formatConfig.bitWidth.bytes
        val limit = (startOffset + lengthInBytes).coerceAtMost(buffer.capacity())

        for (offset in startOffset until limit step step) {
            if (offset + step > buffer.capacity()) break
            val raw = DataDecoder.readRawValue(buffer, offset, formatConfig)
            val physical = DataDecoder.toPhysicalValue(raw, formatConfig)
            points.add(Point2D(offset = offset, rawValue = raw, physicalValue = physical))
        }
        return points
    }

    /**
     * Generuje siatkę 3D o zadanych wymiarach rows x cols począwszy od startOffset.
     */
    fun extract3DMatrix(startOffset: Int, rows: Int, cols: Int): MapData3D {
        val matrix = Array(rows) { DoubleArray(cols) }
        val step = formatConfig.bitWidth.bytes
        var currentOffset = startOffset

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (currentOffset + step <= buffer.capacity()) {
                    val raw = DataDecoder.readRawValue(buffer, currentOffset, formatConfig)
                    matrix[r][c] = DataDecoder.toPhysicalValue(raw, formatConfig)
                    currentOffset += step
                } else {
                    matrix[r][c] = 0.0
                }
            }
        }

        val rowHeaders = (0 until rows).map { it.toDouble() }
        val colHeaders = (0 until cols).map { it.toDouble() }

        return MapData3D(
            rows = rows,
            cols = cols,
            rowHeaders = rowHeaders,
            colHeaders = colHeaders,
            data = matrix
        )
    }
}