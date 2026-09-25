package com.winols.app.core.binary

import com.winols.app.core.dispatcher.AppDispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

data class MapDefinition(
    val offset: Int,
    val rows: Int,
    val cols: Int,
    val is16Bit: Boolean = true,
    val name: String
)

data class ScanProgress(
    val processedBytes: Long,
    val totalBytes: Long,
    val percentage: Int
)

class EcuBinaryProcessor(
    private val dispatchers: AppDispatchers = AppDispatchers()W celu zapewnienia płynnego działania interfejsu (60/120 FPS) przy przetwarzaniu wielomegabajtowych plików binarnych ECU (.bin, .hex, .ori), cała warstwa obliczeniowa musi być oddelegowana poza wątek główny (`Dispatchers.Main`). 

Standardowe coroutines z domyślnym `Dispatchers.IO` są zoptymalizowane pod operacje I/O (blokowanie gniazd/dysku), natomiast intensywne przeliczanie sum kontrolnych, dezasemblacja czy wyszukiwanie wzorców map 2D/3D to operacje mocno obciążające procesor (**CPU-bound**), które wymagają `Dispatchers.Default` lub dedykowanego poola wątków.

Poniżej znajduje się implementacja architektury asynchronicznego silnika binarnego opartego o Kotlin Coroutines, wzorzec MVI/Clean Architecture oraz bezpieczne wstrzykiwanie dyspozytorów.

---

### app/src/main/java/com/winols/app/core/dispatcher/CoroutineDispatchers.kt