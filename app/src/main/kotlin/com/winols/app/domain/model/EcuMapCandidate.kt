package com.winols.app.domain.model

enum class DataType(val byteSize: Int) {
    UBYTE(1),
    SBYTE(1),
    UWORD_BE(2),
    UWORD_LE(2),
    SWORD_BE(2),
    SWORD_LE(2)
}

enum class MapDimension {
    ONE_D, // 1D / Wektor pojedynczy
    TWO_D  // 2D / 3D Macierz (X, Y -> Z)
}

data class AxisData(
    val offset: Int,
    val length: Int,
    val dataType: DataType,
    val rawValues: List<Int>,
    val isMonotonic: Boolean
)

data class EcuMapCandidate(
    val id: String,
    val name: String,
    val dataOffset: Int,
    val dimX: Int,
    val dimY: Int,
    val dataType: DataType,
    val dimension: MapDimension,
    val confidenceScore: Float, // 0.0 - 1.W sterownikach ECU (np. Bosch EDC15, EDC16, ME7, Siemens, Delphi) mapy kalibracyjne są zorganizowane w ustrukturyzowane bloki danych poprzedzone deskryptorami osi lub nagłówkami wymiarów.

W architekturze Clean Architecture logika wykrywania map stanowi domenowy moduł analityczny (`domain/detector`), niezależny od warstwy widoku i frameworka Androida.

---

### Zasada działania algorytmu detekcji map

1. **Wzorzec nagłówków Bosch (Header-based detection):**
   * Format dwubajtowy (16-bit Little/Big Endian) zawierający identyfikator/wymiary, np. bajty definiujące liczbę punktów osi $X$ i $Y$ (np. `0xEE 0x01` dla osi o długości 14 punktów lub bezpośrednie wartości wymiarów $M \times N$ w granicach np. $4 \le X \le 32$, $2 \le Y \le 32$).
2. **Weryfikacja monotoniczności osi (Axis candidate verification):**
   * Prawidłowe osie parametrów wejściowych (RPM, ciśnienie doładowania, temperatura, masa powietrza) charakteryzują się ścisłą monotonicznością (rosnące lub malejące ciągi wartości) oraz brakiem powtórzeń sąsiednich wartości (z wyjątkiem skrajnych punktów nasycenia).
3. **Analiza wariancji i gradientu danych (Entropy & Gradient profiling):**
   * Bloki danych map posiadają regularne gradienty powierzchniowe (brak szumu losowego i brak długich sekwencji zerowych/wypełnień `0xFF` lub `0x00`).

---

### Implementacja w projekcie

### app/src/main/java/com/winols/app/domain/model/EcuMap.kt