package com.winols.app.engine

import com.winols.app.data.BinaryBufferManager
import com.winols.app.model.AxisDefinition
import com.winols.app.model.DataType
import com.winols.app.model.Endianness
import com.winols.app.model.MapCategory
import com.winols.app.model.MapDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.abs

/**
 * Zaawansowany silnik wyszukiwania map binarnych ECU.
 * Wykrywa sekwencje nagłówków wymiarów, weryfikuje monotoniczność osi X/Y
 * oraz sprawdza ciągłość gradientu wartości (eliminacja szumu maszynowego i kodu ASM).
 */
class MapFinderEngine(private val bufferManager: BinaryBufferManager) {

    data class ScanProfile(
        val minDim: Int = 4,
        val maxDim: Int = 32,
        val checkBigEndian: Boolean = true,
        val checkLittleEndian: Boolean = true,
        val minConfidence: Double = 0.65
    )

    suspend fun scanPotentialMapsAsync(profile: ScanProfile = ScanProfile()): List<MapDefinition> =
        withContext(Dispatchers.Default) {
            val detectedMaps = mutableListOf<MapDefinition>()
            val bufferSize = bufferManager.size
            var cursor = 0

            while (cursor < bufferSize - 64) {
                var candidate: MapCandidate? = null

                if (profile.checkBigEndian) {
                    candidate = detectAtOffset(cursor, Endianness.BIG_ENDIAN, profile)
                }
                if (candidate == null && profile.checkLittleEndian) {
                    candidate = detectAtOffset(cursor, Endianness.LITTLE_ENDIAN, profile)
                }

                if (candidate != null && candidate.confidence >= profile.minConfidence) {
                    val mapDef = constructMapDefinition(candidate)
                    detectedMaps.add(mapDef)
                    // Przeskocz zidentyfikowany blok mapy, aby uniknąć duplikatów
                    cursor = candidate.dataOffset + mapDef.totalBytes
                    continue
                }

                cursor += 2 // Wyrównanie do słowa 16-bitowego
            }

            detectedMaps
        }

    private data class MapCandidate(
        val headerOffset: Int,
        val dataOffset: Int,
        val rows: Int,
        val cols: Int,
        val endianness: Endianness,
        val dataType: DataType,
        val confidence: Double,
        val xAxis: AxisDefinition?,
        val yAxis: AxisDefinition?,
        val category: MapCategory
    )

    private fun detectAtOffset(
        offset: Int,
        endianness: Endianness,
        profile: ScanProfile
    ): MapCandidate? {
        val totalSize = bufferManager.size

        // Wzorzec 1: Deskryptor Bosch standardowy (1 bajt Cols, 1 bajt Rows lub odwrotnie)
        val dim1 = bufferManager.readValue(offset, DataType.UINT8, endianness).toInt()
        val dim2 = bufferManager.readValue(offset + 1, DataType.UINT8, endianness).toInt()

        if (dim1 in profile.minDim..profile.maxDim && dim2 in profile.minDim..profile.maxDim) {
            // Test 1: Nagłówek bezpośrednio przed danymi
            val candidate = evaluateHeaderAndPayload(
                headerOffset = offset,
                dataOffset = offset + 2,
                cols = dim1,
                rows = dim2,
                endianness = endianness,
                dataType = DataType.UINT16
            )
            if (candidate != null) return candidate
        }

        // Wzorzec 2: Nagłówki 16-bitowe z prefiksami identyfikatorów osi (np. EDC16/EDC17/MED17)
        val dimWord1 = bufferManager.readValue(offset, DataType.UINT16, endianness).toInt()
        val dimWord2 = bufferManager.readValue(offset + 2, DataType.UINT16, endianness).toInt()

        if (dimWord1 in profile.minDim..profile.maxDim && dimWord2 in profile.minDim..profile.maxDim) {
            val candidate = evaluateHeaderAndPayload(
                headerOffset = offset,
                dataOffset = offset + 4,
                cols = dimWord1,
                rows = dimWord2,
                endianness = endianness,
                dataType = DataType.UINT16
            )
            if (candidate != null) return candidate
        }

        return null
    }

    private fun evaluateHeaderAndPayload(
        headerOffset: Int,
        dataOffset: Int,
        cols: Int,
        rows: Int,
        endianness: Endianness,
        dataType: DataType
    ): MapCandidate? {
        val totalCells = rows * cols
        val payloadByteLength = totalCells * dataType.byteSize
        if (dataOffset + payloadByteLength > bufferManager.size) return null

        // Sprawdzenie osi poprzedzających mapę (popularne w EDC15/EDC16)
        val (xAxis, yAxis) = resolveAxes(headerOffset, cols, rows, endianness, dataType)

        // Obliczenie wskaźników statystycznych payloadu
        val metrics = calculatePayloadMetrics(dataOffset, rows, cols, endianness, dataType)

        // Odrzucenie stałych ciągów (np. same 00 lub FF), szumu losowego i kodu wykonywalnego
        if (metrics.zeroRatio > 0.85 || metrics.entropy < 0.25 || metrics.noiseRatio > 0.55) {
            return null
        }

        var confidence = 0.5
        if (xAxis != null) confidence += 0.25
        if (yAxis != null) confidence += 0.15
        if (metrics.smoothnessScore > 0.6) confidence += 0.1

        val category = classifyMapCategory(metrics, rows, cols)

        return MapCandidate(
            headerOffset = headerOffset,
            dataOffset = dataOffset,
            rows = rows,
            cols = cols,
            endianness = endianness,
            dataType = dataType,
            confidence = confidence.coerceAtMost(1.0),
            xAxis = xAxis,
            yAxis = yAxis,
            category = category
        )
    }

    private data class PayloadMetrics(
        val zeroRatio: Double,
        val entropy: Double,
        val noiseRatio: Double,
        val smoothnessScore: Double,
        val minVal: Double,
        val maxVal: Double
    )

    private fun calculatePayloadMetrics(
        offset: Int,
        rows: Int,
        cols: Int,
        endianness: Endianness,
        dataType: DataType
    ): PayloadMetrics {
        val totalCells = rows * cols
        var zeroCount = 0
        var abruptJumps = 0
        var smoothTransitions = 0

        val matrix = DoubleArray(totalCells)
        var minVal = Double.MAX_VALUE
        var maxVal = -Double.MAX_VALUE

        for (i in 0 until totalCells) {
            val v = bufferManager.readValue(offset + (i * dataType.byteSize), dataType, endianness)
            matrix[i] = v
            if (v == 0.0) zeroCount++
            if (v < minVal) minVal = v
            if (v > maxVal) maxVal = v
        }

        val range = (maxVal - minVal).coerceAtLeast(1.0)

        // Analiza gradientu wiersz po wierszu
        for (r in 0 until rows) {
            for (c in 0 until cols - 1) {
                val idx1 = r * cols + c
                val idx2 = idx1 + 1
                val diff = abs(matrix[idx2] - matrix[idx1]) / range

                if (diff > 0.65) abruptJumps++
                if (diff in 0.001..0.25) smoothTransitions++
            }
        }

        val totalTransitions = rows * (cols - 1)
        val noiseRatio = abruptJumps.toDouble() / totalTransitions.coerceAtLeast(1)
        val smoothnessScore = smoothTransitions.toDouble() / totalTransitions.coerceAtLeast(1)
        val zeroRatio = zeroCount.toDouble() / totalCells

        // Proste oszacowanie rozrzutu wartości (entropii)
        val distinctValuesCount = matrix.distinct().size
        val entropy = distinctValuesCount.toDouble() / totalCells

        return PayloadMetrics(zeroRatio, entropy, noiseRatio, smoothnessScore, minVal, maxVal)
    }

    private fun resolveAxes(
        headerOffset: Int,
        cols: Int,
        rows: Int,
        endianness: Endianness,
        dataType: DataType
    ): Pair<AxisDefinition?, AxisDefinition?> {
        val step = dataType.byteSize
        val xAxisBytes = cols * step
        val yAxisBytes = rows * step

        // Sprawdź czy bezpośrednio przed nagłówkiem znajdują się wektory ściśle monotoniczne
        val potentialXAddress = headerOffset - xAxisBytes
        val potentialYAddress = headerOffset - xAxisBytes - yAxisBytes

        val xAxis = if (potentialXAddress >= 0) {
            extractStrictMonotonicAxis(potentialXAddress, cols, endianness, dataType, "X Axis")
        } else null

        val yAxis = if (potentialYAddress >= 0) {
            extractStrictMonotonicAxis(potentialYAddress, rows, endianness, dataType, "Y Axis")
        } else null

        return Pair(xAxis, yAxis)
    }

    private fun extractStrictMonotonicAxis(
        address: Int,
        length: Int,
        endianness: Endianness,
        dataType: DataType,
        name: String
    ): AxisDefinition? {
        val values = DoubleArray(length)
        var isAscending = true
        var isDescending = true
        var prev = bufferManager.readValue(address, dataType, endianness)
        values[0] = prev

        for (i in 1 until length) {
            val cur = bufferManager.readValue(address + (i * dataType.byteSize), dataType, endianness)
            values[i] = cur
            if (cur <= prev) isAscending = false
            if (cur >= prev) isDescending = false
            prev = cur
        }

        // Osie fizyczne (np. RPM, ciśnienie, temperatura) są niemal zawsze ściśle monotoniczne
        return if (isAscending || isDescending) {
            AxisDefinition(
                name = name,
                address = address,
                length = length,
                dataType = dataType,
                endianness = endianness,
                values = values
            )
        } else null
    }

    private fun classifyMapCategory(metrics: PayloadMetrics, rows: Int, cols: Int): MapCategory {
        return when {
            // Zakres typowy dla dawek paliwa (np. mg/hub lub ciśnienia doładowania)
            metrics.maxVal in 1500.0..6000.0 && metrics.smoothnessScore > 0.5 -> MapCategory.IGNITION_BOOST
            metrics.maxVal in 10000.0..22000.0 -> MapCategory.RAIL_PRESSURE
            rows == 1 || cols == 1 -> MapCategory.TORQUE_LIMITER
            metrics.maxVal <= 150.0 && metrics.smoothnessScore > 0.4 -> MapCategory.INJECTION
            else -> MapCategory.UNKNOWN
        }
    }

    private fun constructMapDefinition(candidate: MapCandidate): MapDefinition {
        val hexAddr = Integer.toHexString(candidate.dataOffset).uppercase().padStart(6, '0')
        return MapDefinition(
            id = UUID.randomUUID().toString(),
            name = "Map_0x${hexAddr}_${candidate.cols}x${candidate.rows}",
            startAddress = candidate.dataOffset,
            rows = candidate.rows,
            cols = candidate.cols,
            dataType = candidate.dataType,
            endianness = candidate.endianness,
            category = candidate.category,
            confidenceScore = candidate.confidence,
            xAxis = candidate.xAxis,
            yAxis = candidate.yAxis
        )
    }
}