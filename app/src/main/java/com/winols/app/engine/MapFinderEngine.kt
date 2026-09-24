package com.winols.app.engine

import com.winols.app.model.AxisDefinition
import com.winols.app.model.CellDataType
import com.winols.app.model.MapDefinition
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

/**
 * Silnik heurystycznego wykrywania map 2D i 3D w zrzutach pamięci ECU (Bosch EDC15/EDC16, ME7, Siemens itp.).
 */
class MapFinderEngine {

    companion object {
        private const val MIN_AXIS_DIM = 4
        private const val MAX_AXIS_DIM = 32
        private const val MIN_CONFIDENCE_THRESHOLD = 0.45f
    }

    /**
     * Skanuje asynchronicznie przekazany bufor w poszukiwaniu kandydatów na mapy.
     *
     * @param buffer Bezpośredni bufor pliku binarnego.
     * @param onProgress Callback zgłaszający postęp (0.0 .. 1.0).
     * @return Posortowana lista unikalnych map wg adresu i wskaźnika pewności.
     */
    suspend fun scanForMaps(
        buffer: ByteBuffer,
        onProgress: suspend (Float) -> Unit = {}
    ): List<MapDefinition> = withContext(Dispatchers.Default) {
        val detectedMaps = mutableListOf<MapDefinition>()
        val capacity = buffer.capacity()
        if (capacity < 128) return@withContext emptyList()

        val readOnly = buffer.asReadOnlyBuffer()
        val step = 2
        val totalSteps = capacity / step

        var lastReportedPercent = -1

        for (offset in 0 until capacity - 64 step step) {
            ensureActive()

            val currentPercent = ((offset.toFloat() / capacity) * 100).toInt()
            if (currentPercent != lastReportedPercent) {
                lastReportedPercent = currentPercent
                onProgress(offset.toFloat() / capacity)
            }

            // Sprawdzenie wzorców dla Little Endian oraz Big Endian
            val mapLE = probeHeaderAt(readOnly, offset, ByteOrder.LITTLE_ENDIAN)
            if (mapLE != null && mapLE.confidence >= MIN_CONFIDENCE_THRESHOLD) {
                detectedMaps.add(mapLE)
            }

            val mapBE = probeHeaderAt(readOnly, offset, ByteOrder.BIG_ENDIAN)
            if (mapBE != null && mapBE.confidence >= MIN_CONFIDENCE_THRESHOLD) {
                detectedMaps.add(mapBE)
            }
        }

        onProgress(1.0f)
        deduplicateAndFilter(detectedMaps)
    }

    /**
     * Weryfikuje strukturę nagłówka osi ECU w zadanym offsecie:
     * Format typowy (Bosch): [X_ID/Preambuła] [Liczba_X] [Y_ID/Preambuła] [Liczba_Y] ... Osie ... Mapa
     */
    private fun probeHeaderAt(buffer: ByteBuffer, offset: Int, order: ByteOrder): MapDefinition? {
        buffer.order(order)
        val capacity = buffer.capacity()

        // 1. Sprawdzamy wzorzec: [Cols (16-bit)] [Rows (16-bit)]
        if (offset + 4 >= capacity) return null
        val colsCandidate = buffer.getShort(offset).toInt() and 0xFFFF
        val rowsCandidate = buffer.getShort(offset + 2).toInt() and 0xFFFF

        if (isValidDimension(colsCandidate) && isValidDimension(rowsCandidate)) {
            val axisDataType = if (order == ByteOrder.LITTLE_ENDIAN) CellDataType.UWORD_LE else CellDataType.UWORD_BE
            val cellDataType = axisDataType

            val xAxisAddress = offset + 4
            val xAxisBytes = colsCandidate * axisDataType.byteSize
            val yAxisAddress = xAxisAddress + xAxisBytes
            val yAxisBytes = rowsCandidate * axisDataType.byteSize
            val dataAddress = yAxisAddress + yAxisBytes

            val totalDataBytes = colsCandidate * rowsCandidate * cellDataType.byteSize
            if (dataAddress + totalDataBytes <= capacity) {
                val xAxisValues = readRawArray(buffer, xAxisAddress, colsCandidate, axisDataType)
                val yAxisValues = readRawArray(buffer, yAxisAddress, rowsCandidate, axisDataType)

                val xMonotonic = isMonotonicIncreasing(xAxisValues)
                val yMonotonic = isMonotonicIncreasing(yAxisValues)

                if (xMonotonic && yMonotonic) {
                    val mapValues = readRawArray(buffer, dataAddress, colsCandidate * rowsCandidate, cellDataType)
                    val confidence = calculateConfidence(xAxisValues, yAxisValues, mapValues)

                    val xAxis = AxisDefinition(
                        id = "X_0x${xAxisAddress.toString(16).uppercase()}",
                        name = "X Axis",
                        startAddress = xAxisAddress,
                        length = colsCandidate,
                        dataType = axisDataType,
                        rawValues = xAxisValues
                    )

                    val yAxis = AxisDefinition(
                        id = "Y_0x${yAxisAddress.toString(16).uppercase()}",
                        name = "Y Axis",
                        startAddress = yAxisAddress,
                        length = rowsCandidate,
                        dataType = axisDataType,
                        rawValues = yAxisValues
                    )

                    return MapDefinition(
                        id = "MAP_0x${dataAddress.toString(16).uppercase()}",
                        name = "Map ${colsCandidate}x${rowsCandidate} @ 0x${dataAddress.toString(16).uppercase()}",
                        startAddress = dataAddress,
                        rows = rowsCandidate,
                        columns = colsCandidate,
                        dataType = cellDataType,
                        xAxis = xAxis,
                        yAxis = yAxis,
                        confidence = confidence
                    )
                }
            }
        }

        // 2. Sprawdzamy wzorzec osi 8-bitowej: [Cols (8-bit)] [Rows (8-bit)]
        val cols8 = buffer.get(offset).toInt() and 0xFF
        val rows8 = buffer.get(offset + 1).toInt() and 0xFF

        if (isValidDimension(cols8) && isValidDimension(rows8)) {
            val axisDataType = CellDataType.UBYTE
            val cellDataType = if (order == ByteOrder.LITTLE_ENDIAN) CellDataType.UWORD_LE else CellDataType.UWORD_BE

            val xAxisAddress = offset + 2
            val yAxisAddress = xAxisAddress + cols8
            val dataAddress = yAxisAddress + rows8

            val totalDataBytes = cols8 * rows8 * cellDataType.byteSize
            if (dataAddress + totalDataBytes <= capacity) {
                val xAxisValues = readRawArray(buffer, xAxisAddress, cols8, axisDataType)
                val yAxisValues = readRawArray(buffer, yAxisAddress, rows8, axisDataType)

                if (isMonotonicIncreasing(xAxisValues) && isMonotonicIncreasing(yAxisValues)) {
                    val mapValues = readRawArray(buffer, dataAddress, cols8 * rows8, cellDataType)
                    val confidence = calculateConfidence(xAxisValues, yAxisValues, mapValues) * 0.9f

                    return MapDefinition(
                        id = "MAP_0x${dataAddress.toString(16).uppercase()}",
                        name = "Map ${cols8}x${rows8} (8b Axis) @ 0x${dataAddress.toString(16).uppercase()}",
                        startAddress = dataAddress,
                        rows = rows8,
                        columns = cols8,
                        dataType = cellDataType,
                        xAxis = AxisDefinition("X_AXIS", "X", xAxisAddress, cols8, axisDataType, rawValues = xAxisValues),
                        yAxis = AxisDefinition("Y_AXIS", "Y", yAxisAddress, rows8, axisDataType, rawValues = yAxisValues),
                        confidence = confidence
                    )
                }
            }
        }

        return null
    }

    private fun isValidDimension(dim: Int): Boolean = dim in MIN_AXIS_DIM..MAX_AXIS_DIM

    private fun isMonotonicIncreasing(values: DoubleArray): Boolean {
        if (values.size < MIN_AXIS_DIM) return false
        var strictlyIncreasingSteps = 0

        for (i in 0 until values.size - 1) {
            val diff = values[i + 1] - values[i]
            if (diff < 0) return false // Oś nie może maleć
            if (diff > 0) strictlyIncreasingSteps++
        }
        // Przynajmniej 70% kroków musi rosnąć (eliminacja płaskich bloków zer lub powtórzonych bajtów)
        return strictlyIncreasingSteps.toDouble() / (values.size - 1) >= 0.70
    }

    private fun readRawArray(
        buffer: ByteBuffer,
        address: Int,
        count: Int,
        dataType: CellDataType
    ): DoubleArray {
        val result = DoubleArray(count)
        var cur = address

        for (i in 0 until count) {
            val v = when (dataType) {
                CellDataType.UBYTE -> (buffer.get(cur).toInt() and 0xFF).toDouble()
                CellDataType.SBYTE -> buffer.get(cur).toDouble()
                CellDataType.UWORD_LE, CellDataType.UWORD_BE -> (buffer.getShort(cur).toInt() and 0xFFFF).toDouble()
                CellDataType.SWORD_LE, CellDataType.SWORD_BE -> buffer.getShort(cur).toDouble()
                CellDataType.UDWORD_LE, CellDataType.UDWORD_BE -> (buffer.getInt(cur).toLong() and 0xFFFFFFFFL).toDouble()
                CellDataType.SDWORD_LE, CellDataType.SDWORD_BE -> buffer.getInt(cur).toDouble()
            }
            result[i] = v
            cur += dataType.byteSize
        }
        return result
    }

    /**
     * Oblicza współczynnik wiarygodności (confidence) na podstawie:
     * - braku skrajnego szumu i braku pustych bloków (0x00 lub 0xFF),
     * - ciągłości gradientu wartości macierzy,
     * - rozpiętości wartości na osiach.
     */
    private fun calculateConfidence(
        xAxis: DoubleArray,
        yAxis: DoubleArray,
        data: DoubleArray
    ): Float {
        if (data.isEmpty()) return 0.0f

        var zeroCount = 0
        var maxVal = Double.MIN_VALUE
        var minVal = Double.MAX_VALUE

        for (v in data) {
            if (v == 0.0 || v == 65535.0 || v == 255.0) zeroCount++
            if (v > maxVal) maxVal = v
            if (v < minVal) minVal = v
        }

        // Jeżeli ponad 70% komórek to puste bajty (padding/flash unprogrammed), to nie jest mapa
        val fillRatio = zeroCount.toDouble() / data.size
        if (fillRatio > 0.70) return 0.1f
        if (maxVal == minVal) return 0.05f

        var smoothTransitions = 0
        for (i in 0 until data.size - 1) {
            val delta = abs(data[i + 1] - data[i])
            val span = maxVal - minVal
            if (span > 0 && (delta / span) < 0.40) {
                smoothTransitions++
            }
        }

        val smoothnessScore = smoothTransitions.toFloat() / (data.size - 1)
        val axisScore = ((xAxis.last() > xAxis.first()) && (yAxis.last() > yAxis.first()))

        var confidence = 0.5f + (smoothnessScore * 0.35f) - (fillRatio.toFloat() * 0.2f)
        if (axisScore) confidence += 0.15f

        return confidence.coerceIn(0.0f, 1.0f)
    }

    /**
     * Usuwa nakładające się mapy kandydatów, faworyzując te o wyższym confidence.
     */
    private fun deduplicateAndFilter(candidates: List<MapDefinition>): List<MapDefinition> {
        val sorted = candidates.sortedByDescending { it.confidence }
        val finalSelection = mutableListOf<MapDefinition>()

        for (cand in sorted) {
            val overlaps = finalSelection.any { existing ->
                val candStart = cand.startAddress
                val candEnd = cand.endAddress
                val existStart = existing.startAddress
                val existEnd = existing.endAddress

                maxOf(candStart, existStart) < minOf(candEnd, existEnd)
            }

            if (!overlaps) {
                finalSelection.add(cand)
            }
        }

        return finalSelection.sortedBy { it.startAddress }
    }
}