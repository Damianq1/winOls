package com.winols.app.data

import com.winols.app.model.DataType
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer

/**
 * Zaawansowany menedżer bufora binarnego ECU.
 * Obsługuje bezpośrednie bufory pamięci (ByteBuffer.allocateDirect) dla plików binarnych (.bin)
 * oraz import/eksport rekordów Intel HEX (.hex).
 */
class BinaryBufferManager(initialCapacity: Int = 0) {

    private var directBuffer: ByteBuffer = ByteBuffer.allocateDirect(initialCapacity.coerceAtLeast(0))
    private var originalBackup: ByteBuffer = ByteBuffer.allocateDirect(initialCapacity.coerceAtLeast(0))

    private val hexCodec = IntelHexCodec()
    private val valueCache = HashMap<Long, Double>(1024)

    var fileBaseAddress: Long = 0L
        private set

    val size: Int
        get() = directBuffer.capacity()

    val rawBuffer: ByteArray
        get() {
            val arr = ByteArray(directBuffer.capacity())
            val dup = directBuffer.duplicate()
            dup.position(0)
            dup.get(arr)
            return arr
        }

    @Synchronized
    fun loadFromFile(file: File) {
        val nameLower = file.name.lowercase()
        if (nameLower.endsWith(".hex") || nameLower.endsWith(".ihex")) {
            file.inputStream().use { loadFromHexStream(it) }
        } else {
            val fileLength = file.length().toInt()
            reallocateIfNeeded(fileLength)

            RandomAccessFile(file, "r").use { raf ->
                val channel = raf.channel
                directBuffer.clear()
                channel.read(directBuffer)
                directBuffer.flip()

                originalBackup.clear()
                val dup = directBuffer.duplicate()
                dup.position(0)
                originalBackup.put(dup)
                originalBackup.flip()
            }
            fileBaseAddress = 0L
            invalidateCache()
        }
    }

    @Synchronized
    fun loadFromBytes(bytes: ByteArray) {
        // Autodetekcja: czy to plik Intel HEX przekazany jako bajty
        if (bytes.size > 11 && bytes[0].toInt().toChar() == ':') {
            loadFromHexStream(ByteArrayInputStream(bytes))
            return
        }

        reallocateIfNeeded(bytes.size)

        directBuffer.clear()
        directBuffer.put(bytes)
        directBuffer.flip()

        originalBackup.clear()
        originalBackup.put(bytes)
        originalBackup.flip()

        fileBaseAddress = 0L
        invalidateCache()
    }

    @Synchronized
    fun loadFromHexStream(stream: InputStream) {
        val parsed = hexCodec.decodeHex(stream)
        fileBaseAddress = parsed.baseAddress

        reallocateIfNeeded(parsed.binaryData.size)

        directBuffer.clear()
        directBuffer.put(parsed.binaryData)
        directBuffer.flip()

        originalBackup.clear()
        originalBackup.put(parsed.binaryData)
        originalBackup.flip()

        invalidateCache()
    }

    private fun reallocateIfNeeded(requiredSize: Int) {
        if (directBuffer.capacity() != requiredSize) {
            directBuffer = ByteBuffer.allocateDirect(requiredSize)
            originalBackup = ByteBuffer.allocateDirect(requiredSize)
        }
    }

    fun invalidateCache() {
        valueCache.clear()
    }

    fun readValue(address: Long, type: DataType): Double {
        val relAddr = address - fileBaseAddress
        val cacheKey = (relAddr shl 8) or type.ordinal.toLong()
        val cached = valueCache[cacheKey]
        if (cached != null) return cached

        val idx = relAddr.toInt()
        if (idx < 0 || idx + type.byteSize > directBuffer.capacity()) {
            return 0.0
        }

        val result = when (type) {
            DataType.UBYTE -> (directBuffer.get(idx).toInt() and 0xFF).toDouble()
            DataType.SBYTE -> directBuffer.get(idx).toDouble()
            DataType.UWORD_LE -> {
                val b0 = directBuffer.get(idx).toInt() and 0xFF
                val b1 = directBuffer.get(idx + 1).toInt() and 0xFF
                ((b1 shl 8) or b0).toDouble()
            }
            DataType.SWORD_LE -> {
                val b0 = directBuffer.get(idx).toInt() and 0xFF
                val b1 = directBuffer.get(idx + 1).toInt()
                ((b1 shl 8) or b0).toShort().toDouble()
            }
            DataType.UWORD_BE -> {
                val b0 = directBuffer.get(idx).toInt() and 0xFF
                val b1 = directBuffer.get(idx + 1).toInt() and 0xFF
                ((b0 shl 8) or b1).toDouble()
            }
            DataType.SWORD_BE -> {
                val b0 = directBuffer.get(idx).toInt()
                val b1 = directBuffer.get(idx + 1).toInt() and 0xFF
                ((b0 shl 8) or b1).toShort().toDouble()
            }
            DataType.UDWORD_LE -> {
                val b0 = directBuffer.get(idx).toLong() and 0xFFL
                val b1 = directBuffer.get(idx + 1).toLong() and 0xFFL
                val b2 = directBuffer.get(idx + 2).toLong() and 0xFFL
                val b3 = directBuffer.get(idx + 3).toLong() and 0xFFL
                ((b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0).toDouble()
            }
            DataType.SDWORD_LE -> {
                val b0 = directBuffer.get(idx).toInt() and 0xFF
                val b1 = directBuffer.get(idx + 1).toInt() and 0xFF
                val b2 = directBuffer.get(idx + 2).toInt() and 0xFF
                val b3 = directBuffer.get(idx + 3).toInt()
                ((b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0).toDouble()
            }
            DataType.UDWORD_BE -> {
                val b0 = directBuffer.get(idx).toLong() and 0xFFL
                val b1 = directBuffer.get(idx + 1).toLong() and 0xFFL
                val b2 = directBuffer.get(idx + 2).toLong() and 0xFFL
                val b3 = directBuffer.get(idx + 3).toLong() and 0xFFL
                ((b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3).toDouble()
            }
            DataType.SDWORD_BE -> {
                val b0 = directBuffer.get(idx).toInt()
                val b1 = directBuffer.get(idx + 1).toInt() and 0xFF
                val b2 = directBuffer.get(idx + 2).toInt() and 0xFF
                val b3 = directBuffer.get(idx + 3).toInt() and 0xFF
                ((b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3).toDouble()
            }
        }

        if (valueCache.size < 4096) {
            valueCache[cacheKey] = result
        }
        return result
    }

    @Synchronized
    fun writeValue(address: Long, type: DataType, rawValue: Double) {
        val relAddr = address - fileBaseAddress
        val idx = relAddr.toInt()
        if (idx < 0 || idx + type.byteSize > directBuffer.capacity()) return

        when (type) {
            DataType.UBYTE, DataType.SBYTE -> {
                directBuffer.put(idx, rawValue.toInt().toByte())
            }
            DataType.UWORD_LE, DataType.SWORD_LE -> {
                val s = rawValue.toInt()
                directBuffer.put(idx, (s and 0xFF).toByte())
                directBuffer.put(idx + 1, ((s shr 8) and 0xFF).toByte())
            }
            DataType.UWORD_BE, DataType.SWORD_BE -> {
                val s = rawValue.toInt()
                directBuffer.put(idx, ((s shr 8) and 0xFF).toByte())
                directBuffer.put(idx + 1, (s and 0xFF).toByte())
            }
            DataType.UDWORD_LE, DataType.SDWORD_LE -> {
                val v = rawValue.toLong()
                directBuffer.put(idx, (v and 0xFF).toByte())
                directBuffer.put(idx + 1, ((v shr 8) and 0xFF).toByte())
                directBuffer.put(idx + 2, ((v shr 16) and 0xFF).toByte())
                directBuffer.put(idx + 3, ((v shr 24) and 0xFF).toByte())
            }
            DataType.UDWORD_BE, DataType.SDWORD_BE -> {
                val v = rawValue.toLong()
                directBuffer.put(idx, ((v shr 24) and 0xFF).toByte())
                directBuffer.put(idx + 1, ((v shr 16) and 0xFF).toByte())
                directBuffer.put(idx + 2, ((v shr 8) and 0xFF).toByte())
                directBuffer.put(idx + 3, (v and 0xFF).toByte())
            }
        }

        val cacheKey = (relAddr shl 8) or type.ordinal.toLong()
        valueCache.remove(cacheKey)
    }

    fun getDifferences(): List<Long> {
        val diffList = mutableListOf<Long>()
        val cap = directBuffer.capacity()
        for (i in 0 until cap) {
            if (directBuffer.get(i) != originalBackup.get(i)) {
                diffList.add(fileBaseAddress + i)
            }
        }
        return diffList
    }

    fun isModifiedAt(address: Long): Boolean {
        val relAddr = address - fileBaseAddress
        val idx = relAddr.toInt()
        if (idx < 0 || idx >= directBuffer.capacity()) return false
        return directBuffer.get(idx) != originalBackup.get(idx)
    }

    @Synchronized
    fun saveToFile(destination: File) {
        val nameLower = destination.name.lowercase()
        if (nameLower.endsWith(".hex") || nameLower.endsWith(".ihex")) {
            val hexContent = hexCodec.encodeToHex(rawBuffer, fileBaseAddress)
            destination.writeText(hexContent, Charsets.US_ASCII)
        } else {
            RandomAccessFile(destination, "rw").use { raf ->
                val channel = raf.channel
                val dup = directBuffer.duplicate()
                dup.position(0)
                channel.write(dup)
            }
        }
    }
}