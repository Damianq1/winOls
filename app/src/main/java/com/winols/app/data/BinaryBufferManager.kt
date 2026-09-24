package com.winols.app.data

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * Zoptymalizowany menedżer pamięci binarnej dla plików wsadów ECU (512 KB - 8 MB).
 * Wykorzystuje pamięć bezpośrednią (Direct ByteBuffer) poza stosem JVM,
 * minimalizując narzut Garbage Collectora podczas intensywnych operacji heksadecymalnych i edycji map.
 */
class BinaryBufferManager(
    initialCapacity: Int = DEFAULT_CAPACITY,
    var defaultEndianness: ByteOrder = ByteOrder.BIG_ENDIAN
) {
    companion object {
        const val DEFAULT_CAPACITY = 2 * 1024 * 1024 // 2 MB
        const val MIN_SUPPORTED_SIZE = 512 * 1024     // 512 KB
        const val MAX_SUPPORTED_SIZE = 8 * 1024 * 1024 // 8 MB
    }

    private var buffer: ByteBuffer = ByteBuffer.allocateDirect(initialCapacity).order(defaultEndianness)
    private var fileSize: Int = 0

    val size: Int
        get() = fileSize

    val capacity: Int
        get() = buffer.capacity()

    var byteOrder: ByteOrder
        get() = buffer.order()
        set(value) {
            buffer.order(value)
        }

    /**
     * Wczytuje plik binarny bezpośrednio do bufora natywnego za pomocą kanału FileChannel.
     */
    @Synchronized
    fun loadFile(file: File) {
        require(file.exists() && file.isFile) { "Plik nie istnieje lub jest nieprawidłowy: ${file.path}" }
        val length = file.length()
        require(length in MIN_SUPPORTED_SIZE..MAX_SUPPORTED_SIZE) {
            "Nieprawidłowy rozmiar pliku: $length bajtów. Wspierany zakres: 512 KB - 8 MB."
        }

        val intLength = length.toInt()
        ensureCapacity(intLength)

        RandomAccessFile(file, "r").use { raf ->
            raf.channel.use { channel ->
                buffer.clear()
                buffer.limit(intLength)
                var bytesRead = 0
                while (bytesRead < intLength) {
                    val read = channel.read(buffer)
                    if (read == -1) break
                    bytesRead += read
                }
                buffer.flip()
                fileSize = intLength
            }
        }
    }

    /**
     * Zapisuje zawartość aktywnego bufora do wskazanego pliku.
     */
    @Synchronized
    fun saveFile(file: File) {
        check(fileSize > 0) { "Bufor jest pusty, brak danych do zapisu." }

        RandomAccessFile(file, "rw").use { raf ->
            raf.channel.use { channel ->
                channel.truncate(0)
                buffer.position(0)
                buffer.limit(fileSize)
                while (buffer.hasRemaining()) {
                    channel.write(buffer)
                }
            }
        }
    }

    /**
     * Zapewnia odpowiednią alokację pamięci bezpośredniej w przypadku załadowania większego pliku.
     */
    @Synchronized
    fun ensureCapacity(requiredCapacity: Int) {
        if (buffer.capacity() < requiredCapacity)Oto implementacja menedżera pamięci binarnej `BinaryBufferManager.kt`, zaprojektowana z myślą o minimalizacji narzutu pamięciowego (zero-copy / low GC pressure) oraz maksymalnej wydajności I/O przy operacjach na plikach ECU/binarnych (512 KB do 8 MB lub większych).

Implementacja wykorzystuje `ByteBuffer.allocateDirect` oraz kanały `FileChannel`, a także wspiera operacje zgodne z endianowością procesorów motoryzacyjnych (Little Endian / Big Endian) oraz typowe konwersje formatów danych 8-bit, 16-bit i 32-bit (signed/unsigned).

### app/src/main/java/com/winols/app/data/BinaryBufferManager.kt