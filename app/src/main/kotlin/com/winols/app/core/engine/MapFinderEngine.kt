package com.winols.app.core.engine

import com.winols.app.core.model.AxisDefinition
import com.winols.app.core.model.DataType
import com.winols.app.core.model.MapDefinition
import com.winols.app.core.model.MapDimension
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MapFinderEngine(
    private val buffer: ByteArray,
    private val byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN
) {
    private val wrappedBuffer = ByteBuffer.wrap(buffer).order(byteOrder)

    /**
     * Skanuje plik binarny w poszukiwaniu typowych struktur nagłówkowych map (np. Bosch EDC15/EDC16/Siemens):
     * Format nagłówka osi: [ID/Cols (16-bit)] [Rows (16-bit)] lub wprost markery rozmiaru 0xXX 0xYY.
     */
    fun findPotentialMaps(
        minDimension: Int = 4,
        maxDimension: Int = 32
    ): List<MapDefinition> {
        val detectedMaps = mutableListOf<MapDefinition>()
        val maxOffset = buffer.size - 64

        var offset = 0
        while (offset < maxOffset) {
            val colsCandidate = readUInt16(offset)
            val rowsCandidate = readUInt16(offset + 2)

            // Sprawdzanie potencjalnych wymiarów mapy 2D/3D
            if (isValidDimension(colsCandidate, minDimension, maxDimension) &&
                isValidDimension(rowsCandidate, minDimension, maxDimension)
            ) {
                val dataOffset = offset + 4
                val mapSize = colsCandidate * rowsCandidate * 2 // 16-bit word default

                if (dataOffset + mapSize <= buffer.size) {
                    val entropy = calculateEntropy(dataOffset, mapSize)
                    // Filtrowanie pustych sekcji i szumu losowego
                    if (entropy in 1.5..7.5) {
                        val xAxis = AxisDefinition(
                            description = "X-Axis",
                            offset = offset - (colsCandidate * 2),
                            length = colsCandidate,
                            dataType = if (byteOrder == ByteOrder.BIG_ENDIAN) DataType.UWORD_BE else DataType.UWORD_LE
                        )
                        val yAxis = AxisDefinition(
                            description = "Y-Axis",
                            offset = offset - (colsCandidate * 2) - (rowsCandidate * 2),
                            length = rowsCandidate,
                            dataType = if (byteOrder == ByteOrder.BIG_ENDIAN) DataType.UWORD_BE else DataType.UWORD_LE
                        )

                        detectedMaps.add(
                            MapDefinition(
                                id = "MAP_0x${offset.toString(16).uppercase()}",
                                name = "Map 3D ${colsCandidate}x${rowsCandidate} @ 0x${offset.toString(16).uppercase()}",
                                offset = dataOffset,
                                rows = rowsCandidate,
                                cols = colsCandidate,
                                dataType = if (byteOrder == ByteOrder.BIG_ENDIAN) DataType.UWORD_BE else DataType.UWORD_LE,
                                dimension = MapDimension.THREE_D,
                                xAxis = if (xAxis.offset >= 0) xAxis else null,
                                yAxis = if (yAxis.offset >= 0) yAxis else null
                            )
                        )
                        offset += mapSize + 4
                        continue
                    }
                }
            }
            offset += 2
        }

        return detectedMaps
    }

    /**
     * Odczyt surowych znormalizowanych wartości z bufora dla danej definicji mapy.
     */
    fun readMapValues(mapDef: MapDefinition): Array<DoubleArray> {
        val result = Array(mapDef.rows) { DoubleArray(mapDef.cols) }
        var currentOffset = mapDef.offset

        for (r in 0 until mapDef.rows) {
            for (c in 0 until mapDef.cols) {
                if (currentOffset + mapDef.dataType.byteSize > buffer.size) break
                val rawValue = readRawValue(currentOffset, mapDef.dataType)
                result[r][c] = (rawValue * mapDef.factor) + mapDef.offsetVal
                currentOffset += mapDef.dataType.byteSize
            }
        }
        return result
    }

    private fun readUInt16(pos: Int): Int {
        if (pos + 2 > buffer.size) return 0
        return wrappedBuffer.getShort(pos).toInt() and 0xFFFF
    }

    private fun readRawValue(pos: Int, type: DataType): Double {
        return when (type) {
            DataType.UBYTE -> (buffer[pos].toInt() and 0xFF).toDouble()
            DataType.SBYTE -> buffer[pos].toDouble()
            DataType.UWORD_BE -> (ByteBuffer.wrap(buffer, pos, 2).order(ByteOrder.BIG_ENDIAN).short.toInt() and 0xFFFF).toDouble()
            DataType.UWORD_LE -> (ByteBuffer.wrap(buffer, pos, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF).toDouble()
            DataType.SWORD_BE -> ByteBuffer.wrap(buffer, pos, 2).order(ByteOrder.BIG_ENDIAN).short.toDouble()
            DataType.SWORD_LE -> ByteBuffer.wrap(buffer, pos, 2).order(ByteOrder.LITTLE_ENDIAN).short.toDouble()
        }
    }

    private fun isValidDimension(value: Int, min: Int, max: Int): Boolean {
        return value in min..max
    }

    private fun calculateEntropy(start: Int, length: Int): Double {
        val frequency = IntArray(256)
        val end = minOf(start + length, buffer.size)
        val count = end - start
        if (count <= 0) return 0.0

        for (i in start until end) {
            frequency[buffer[i].toInt() and 0xFF]++
        }

        var entropy = 0.0
        for (f in frequency) {
            if (f > 0) {
                val p = f.toDouble() / count
                entropy -= p * (Math.log(p) / Math.log(2.0))
            }
        }
        return entropy
    }
}