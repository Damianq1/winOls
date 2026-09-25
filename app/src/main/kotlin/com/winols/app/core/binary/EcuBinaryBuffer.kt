package com.winols.app.core.binary

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * Zoptymalizowany bufor pamięci bezpośredniej (Direct Buffer) dla plików binarnych ECU (512 KB - 8 MB).
 * Obsługuje operacje odczytu/zapisu bez alokacji na stercie JVM (zero-copy / low-GC).
 */
class EcuBinaryBuffer private constructor(
    private val buffer: ByteBuffer,
    val size: Int
) {
    var byteOrder: ByteOrder
        get() = buffer.order()
        set(value) {
            buffer.order(value)
        }

    init {
        buffer.order(ByteOrder.BIG_ENDIAN) // Domyślny format większości sterowników ECU
    }

    companion object {
        fun allocateDirect(size: Int): EcuBinaryBuffer {
            require(size > 0) { "Rozmiar bufora musi być większy niż 0" }
            val directBuf = ByteBuffer.allocateDirect(size)
            return EcuBinaryBuffer(directBuf, size)
        }

        /**
         * Szybki odczyt pliku binarnego do bezpośredniego bufora pamięci.
         */
        fun loadFromFile(file: File): EcuBinaryBuffer {
            require(file.exists() && file.isFile) { "Plik nie istnieje lub jest niepoprawny: ${file.path}" }
            val fileSize = file.length().toInt()

            RandomAccessFile(file, "r").use { raf ->
                raf.channel.use { channel ->
                    val directBuf = ByteBuffer.allocateDirect(fileSize)
                    while (directBuf.hasRemaining()) {
                        if (channel.read(directBuf) == -1) break
                    }
                    directBuf.flip()
                    return EcuBinaryBuffer(directBuf, fileSize)
                }
            }
        }
    }

    // --- OPERACJE ODCZYTU ---

    fun readByte(offset: Int): Byte {
        checkBounds(offset, 1)
        return buffer.get(offset)
    }

    fun readUByte(offset: Int): Int {
        return readByte(offset).toInt() and 0xFF
    }

    fun readShort(offset: Int): Short {
        checkBounds(offset, 2)
        return buffer.getShort(offset)
    }

    fun readUShort(offset: Int): Int {
        return readShort(offset).toInt() and 0xFFFF
    }

    fun readInt(offset: Int): Int {
        checkBounds(offset, 4)
        return buffer.getInt(offset)
    }

    fun readBytes(offset: Int, length: Int): ByteArray {
        checkBounds(offset, length)
        val result = ByteArray(length)
        val slice = buffer.duplicate()
        slice.position(offset)
        slice.get(result, 0, length)
        return result
    }

    // --- OPERACJE ZAPISU / EDYCJI MAP ---

    fun writeByte(offset: Int, value: Byte) {
        checkBounds(offset, 1)
        buffer.put(offset, value)
    }

    fun writeShort(offset: Int, value: Short) {
        checkBounds(offset, 2)
        buffer.putShort(offset, value)
    }

    fun writeInt(offset: Int, value: Int) {
        checkBounds(offset, 4)
        buffer.putInt(offset, value)
    }

    fun writeBytes(offset: Int, data: ByteArray) {
        checkBounds(offset, data.size)
        val slice = buffer.duplicate()
        slice.position(offset)
        slice.put(data)
    }

    /**
     * Bezpośredni zrzut bufora pamięci do pliku wyjściowego za pomocą FileChannel.
     */
    fun saveToFile(targetFile: File) {
        RandomAccessFile(targetFile, "rw").use { raf ->
            raf.channel.use { channel ->
                channel.truncate(0) // Wyczyszczenie istniejącej zawartości
                val slice = buffer.duplicate()
                slice.position(0)
                while (slice.hasRemaining()) {
                    channel.write(slice)
                }
                channel.force(true)
            }
        }
    }

    private fun checkBounds(offset: Int, length: Int) {
        require(offset >= 0 && offset + length <= size) {
            "Przekroczenie zakresu bufora: offset=$offset, długość=$length, rozmiar bufora=$size"
        }
    }
}