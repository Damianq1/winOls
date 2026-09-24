package com.winols.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Typ reprezentacji danych w pamięci ECU.
 */
enum class DataWordSize(val bytesCount: Int) {
    BYTE_8(1),
    WORD_16(2),
    DWORD_32(4)
}

/**
 * Pojedynczy wpis historii zmian do obsługi granularOto kompletna, zoptymalizowana implementacja modułu zarządzania buforem binarnym (`BinaryBufferManager.kt`), zaprojektowana specjalnie pod kątem wydajnej pracy z plikami wsadów ECU (512 KB – 8 MB+) na Androidzie.

Wykorzystuje bezpośredni bufor `ByteBuffer.allocateDirect()`, kanały `FileChannel`, mechanizm migawek pamięci (Undo/Redo) oraz asynchroniczne wsparcie dla Kotlin Coroutines.

### app/src/main/java/com/winols/app/data/BinaryBufferManager.kt