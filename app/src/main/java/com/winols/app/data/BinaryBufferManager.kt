package com.winols.app.data

import com.winols.app.model.DataType
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * Wysokowydajny silnik operacji na buforach binarnych Flash / EEPROM.
 * Zoptymalizowany pod kątem zero-allocation dla operacji komórkowych
 * oraz wektorowego sprawdzania różnic ORI vs MOD.
 */
class BinaryBufferManager(initialCapacity: Int = 0) {

    private var directBuffer: ByteBuffer = ByteBuffer.allocateDirect(initialCapacity.coerceAtLeast(0))
    private var originalBackup: ByteBuffer = ByteBuffer.allocateDirect(initialCapacity.coerceAtLeast(0))

    // Dedykowane widoki bez alokacji z ustaloną kolejnością bajtów
    private var leBuffer: ByteBuffer = directBuffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
    private var beBuffer: ByteBuffer = directBuffer.duplicate().order(ByteOrder.BIG_ENDIAN)
    private var leBackup: ByteBuffer = originalBackup.duplicate().order(ByteOrder.LITTLE_ENDIAN)

    private val hexCodec = IntelHexCodec()
    private val valueCache = HashMap<Long, Double>(2048)

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
            return
        }

        val fileLength = file.length().toInt()
        reallocateIfNeeded(fileLength)

        RandomAccessFile(file, "r").use { raf ->
            val channel = raf.channel
            directBuffer.clear()
            while (directBuffer.hasRemaining()) {
                if (channel.read(directBuffer) == -1) break
            }
            directBuffer.flip()

            originalBackup.clear()
            val dup = directBuffer.duplicate()
            dup.position(0)
            originalBackup.put(dup)
            originalBackup.flip()
        }

        refreshBufferViews()
        fileBaseAddress = 0L
        invalidateCache()
    }

    @Synchronized
    fun loadFromBytes(bytes: ByteArray) {
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

        refreshBufferViews()
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

        refreshBufferViews()
        invalidateCache()
    }

    private fun reallocateIfNeeded(requiredSize: Int) {
        if (directBuffer.capacity() != requiredSize) {
            directBuffer = ByteBuffer.allocateDirect(requiredSize)
            originalBackup = ByteBuffer.allocateDirect(requiredSize)
            refreshBufferViews()
        }
    }

    private fun refreshBufferViews() {
        leBuffer = directBuffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        beBuffer = directBuffer.duplicate().order(ByteOrder.BIG_ENDIAN)
        leBackup = originalBackup.duplicate().order(ByteOrder.LITTLE_ENDIAN)
    }

    fun invalidateCache() {
        valueCache.clear()
    }

    /**
     * Błyskawiczny, bezpośredni odczyt wartości bez alokowania obiektów pośrednich.
     */
    fun readValue(address: Long, type: DataType): Double {
        val relAddr = address - fileBaseAddress
        val cacheKey = (relAddr shl 8) or type.ordinal.toLong()
        val cached = valueCache[cacheKey]
        if (cached != null) return cached

        val idx = relAddr.toInt()
        val cap = directBuffer.capacity()
        if (idx < 0 || idx + type.byteSize > cap) {
            return 0.0
        }

        val result = when (type) {
            DataType.UBYTE -> (directBuffer.get(idx).toInt() and 0xFF).toDouble()
            DataType.SBYTE -> directBuffer.get(idx).toDouble()
            DataType.UWORD_LE -> (leBuffer.getShort(idx).toInt() and 0xFFFF).toDouble()
            DataType.SWORD_LE -> leBuffer.getShort(idx).toDouble()
            DataType.UWORD_BE -> (beBuffer.getShort(idx).toInt() and 0xFFFF).toDouble()
            DataType.SWORD_BE -> beBuffer.getShort(idx).toDouble()
            DataType.UDWORD_LE -> (leBuffer.getInt(idx).toLong() and 0xFFFFFFFFL).toDouble()
            DataType.SDWORD_LE -> leBuffer.getInt(idx).toDouble()
            DataType.UDWORD_BE -> (beBuffer.getInt(idx).toLong() and 0xFFFFFFFFL).toDouble()
            DataType.SDWORD_BE -> beBuffer.getInt(idx).toDouble()
        }

        if (valueCache.size < 8192) {
            valueCache[cacheKey] = result
        }
        return result
    }

    /**
     * Zoptymalizowany zapis wartości do bufora bezpośredniego.
     */
    @Synchronized
    fun writeValue(address: Long, type: DataType, rawValue: Double) {
        val relAddr = address - fileBaseAddress
        val idx = relAddr.toInt()
        val cap = directBuffer.capacity()
        if (idx < 0 || idx + type.byteSize > cap) return

        when (type) {
            DataType.UBYTE, DataType.SBYTE -> {
                directBuffer.put(idx, rawValue.toInt().toByte())
            }
            DataType.UWORD_LE, DataType.SWORD_LE -> {
                leBuffer.putShort(idx, rawValue.toInt().toShort())
            }
            DataType.UWORD_BE, DataType.SWORD_BE -> {
                beBuffer.putShort(idx, rawValue.toInt().toShort())
            }
            DataType.UDWORD_LE, DataType.SDWORD_LE -> {
                leBuffer.putInt(idx, rawValue.toLong().toInt())
            }
            DataType.UDWORD_BE, DataType.SDWORD_BE -> {
                beBuffer.putInt(idx, rawValue.toLong().toInt())
            }
        }

        val cacheKey = (relAddr shl 8) or type.ordinal.toLong()
        valueCache.remove(cacheKey)
    }

    /**
     * Przyspieszone porównywanie bufora MOD z buforem ORI z użyciem słów 64-bitowych (Long).
     */
    fun getDifferences(): List<Long> {
        val diffList = mutableListOf<Long>()
        val cap = directBuffer.capacity()
        val longLimit = cap - (cap % 8)

        var i = 0
        while (i < longLimit) {
            if (leBuffer.getLong(i) != leBackup.getLong(i)) {
                // Precyzyjne sprawdzenie 8 bajtów w zmienionym słowie
                for (b in 0 until 8) {
                    val targetIdx = i + b
                    if (directBuffer.get(targetIdx) != originalBackup.get(targetIdx)) {
                        diffList.add(fileBaseAddress + targetIdx)
                    }
                }
            }
            i += 8
        }

        // Dokończenie pozostałych bajtów
        while (i < cap) {
            if (directBuffer.get(i) != originalBackup.get(i)) {
                diffList.add(fileBaseAddress + i)
            }
            i++
        }

        return diffList
    }

    fun isModifiedAt(address: Long): Boolean {
        val relAddr = address - fileBaseAddress
        val idx = relAddr.toInt()
        if (idx < 0 || idx >= directBuffer.capacity()) return false
        return directBuffer.get(idx) != originalBackup.get(idx)
    }

    /**
     * Szybki odczyt blokowy (Bulk Copy) do zewnętrznej tablicy bajtów.
     */
    fun readBulk(address: Long, destination: ByteArray, offset: Int, length: Int) {
        val relAddr = (address - fileBaseAddress).toInt()
        if (relAddr < 0 || relAddr + length > directBuffer.capacity()) return
        val dup = directBuffer.duplicate()
        dup.position(relAddr)
        dup.get(destination, offset, length)
    }

    /**
     * Szybki zapis blokowy (Bulk Write) z zewnętrznej tablicy bajtów.
     */
    @Synchronized
    fun writeBulk(address: Long, source: ByteArray, offset: Int, length: Int) {
        val relAddr = (address - fileBaseAddress).toInt()
        if (relAddr < 0 || relAddr + length > directBuffer.capacity()) return
        val dup = directBuffer.duplicate()
        dup.position(relAddr)
        dup.put(source, offset, length)
        invalidateCache()
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
                channel.truncate(directBuffer.capacity().toLong())
            }
        }
    }
}