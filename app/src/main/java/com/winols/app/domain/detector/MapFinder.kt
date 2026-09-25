package com.winols.app.domain.detector

import com.winols.app.domain.model.EcuBinary
import com.winols.app.domain.model.EcuMapCandidate

class MapFinder {

    /**
     * Wyszukuje potencjalne matryce map 2D/3D w zadanym zakresie danych binarnych.
     */
    fun detectCandidates(binary: EcuBinary, maxCandidates: Int = 50): List<EcuMapCandidate> {
        val candidates = mutableListOf<EcuMapCandidate>()
        val data = binary.data
        var i = 0

        while (i < data.size - 64 && candidates.size < maxCandidates) {
            // Analiza nagłówków tablic (np. Bosch 16-bit oś X/Y)
            val rows = binary.readByte(i)
            val cols = binary.readByte(i + 1)

            if (rows in 4..32 && cols in 4..32 && (rows * cols * 2) < (data.size - i)) {
                val candidateLength = rows * cols * 2
                // Prosta weryfikacja entropii/wariancji danych
                if (hasValidMapVariance(data, i + 2, candidateLength)) {
                    candidates.add(
                        EcuMapCandidate(
                            id = "MAP_${candidates.size + 1}",
                            startOffset = i + 2,
                            rows = rows,
                            cols = cols,
                            bitResolution = 16
                        )
                    )
                    i += candidateLength
                }
            }
            i += 2
        }
        return candidates
    }

    private fun hasValidMapVariance(data: ByteArray, offset: Int, length: Int): Boolean {
        var min = 255
        var max = 0
        for (idx in offset until (offset + length).coerceAtMost(data.size)) {
            val v = data[idx].toInt() and 0xFF
            if (v < min) min = v
            if (v > max) max = v
        }
        return (max - min) > 15 && min != 0 && max != 255
    }
}