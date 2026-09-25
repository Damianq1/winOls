package com.winols.app.domain.processor

import com.winols.app.core.dispatcher.CoroutineDispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

data class MapCandidate(
    val offset: Int,
    val rows: Int,
    val columns: Int,
    val name: String
)

class EcuBinaryProcessor(
    private val dispatchers: CoroutineDispatchers
) {
    /**
     * Obliczenia CPU-bound (np. CRC32 / Checksum) wykonywane na puli Dispatchers.Default.
     */
    suspend fun calculateChecksum(data: ByteArray): Long = withContext(dispatchers.default) {
        var checksum: Long = 0L
        for (i in data.indices) {
            if (i % 10_000 == 0) ensureActive() // Kooperacyjne przerywanie coroutine
            checksum = (checksum + (data[i].toLong() and 0xFF)) and 0xFFFFFFFFL
        }
        checksum
    }

    /**
     * Asynchroniczny skaner map ECU emitujący postęp operacji do UI za pomocą Flow.
     * Wykonuje się w tle na Dispatchers.Default.
     */
    fun scanMaps(data: ByteArray): Flow<Pair<Float, List<MapCandidate>>> = flow {
        val detected = mutableListOf<MapCandidate>()
        val totalSize = data.size
        val step = 16

        for (i in 0 until (totalSize - step) step step) {
            // Emisja postępu co określony interwał, aby nie przeciążać kolejki UI
            if (i % 32768 == 0) {
                val progress = i.toFloat() / totalSize.toFloat()
                emit(progress to detected.toList())
            }

            // Przykładowa heurystyka wykrywania nagłówków/gradientów map 2D
            if (data[i] == 0x3C.toByte() && data[i + 1] == 0x08.toByte()) {
                detected.add(
                    MapCandidate(
                        offset = i,
                        rows = 8,
                        columns = 8,
                        name = "Potential Map @ 0x${Integer.toHexString(i).uppercase()}"
                    )
                )
            }
        }
        emit(1.0f to detected)
    }.flowOn(dispatchers.default)
}