package com.winols.app.core.detector

import com.winols.app.core.binary.EcuBinaryBuffer
import com.winols.app.core.model.EcuMap

/**
 * WstProjekt **WinOls Mobile** (edytor wsadów binarnych ECU na platformę Android) wymaga architektury zorientowanej na niskopoziomowe operacje na pamięci, rendering 2D/3D dla map silnika oraz moduł automatycznej detekcji struktur.

---

### 1. Krytyczne problemy bieżącej struktury
* **Duplikacja konfiguracji Gradle:** W projekcie istnieją jednocześnie pliki Groovy (`build.gradle`, `settings.gradle`) oraz Kotlin DSL (`build.gradle.kts`, `settings.gradle.kts`). Należy usunąć wersje `.gradle` i standaryzować na `.kts`.
* **Błędne umiejscowienie kodu źródłowego:** W katalogu głównym istnieje `src/main/kotlin/` obok dedykowanego modułu `app/src/main/`. Kod domenowy musi trafić w całości do modułu `:app` lub podmodułów `:core-binary`, `:feature-map-editor`.
* **Skrypty AI i orkiestratora:** Pliki Pythona (`agent_supervisor.py`, `gemini_core.py`, itp.) w katalogu głównym powinny zostać wyizolowane do katalogu `tools/` lub `scripts/`, by nie zanieczyszczać przestrzeni projektu Android Studio.

---

### 2. Proponowane kluczowe moduły i funkcjonalności

| Moduł / Pakiet | Kluczowe Odpowiedzialności i Klasy |
| :--- | :--- |
| **`core:binary`** | Bezpośrednia praca na buforze bajtowym wsadu ECU (np. Bosch EDC15/EDC16/EDC17, Siemens SID). Użycie `ByteBuffer` / `MemoryMappedFile` z obsługą Endianness (Big/Little Endian), interpolacji, sum kontrolnych (Checksum Checkers) i operacji 8-bit / 16-bit / 32-bit. |
| **`core:map-finder`** | Heurystyczna detekcja tabel: wyszukiwanie preambuł osi X/Y, gradientów, monotoniczności osi (RPM, ciśnienie doładowania, dawka paliwa), automatyczne rozpoznawanie map 2D ($1 \times N$, $N \times 1$) i 3D ($N \times M$). |
| **`feature:hex-editor`** | Wirtualizowany edytor Hex View (np. `RecyclerView` renderujący tylko widoczne wiersze offsetów), podświetlanie różnic (Delta View vs Original), konwersje hex/dec/ascii. |
| **`feature:visualizer`** | Wizualizacja map powierzchniowych 3D (OpenGL ES lub SurfaceView z wykorzystaniem Canvas) oraz wykresów 2D z możliwością manipulacji punktami siatki. |
| **`feature:checksum`** | Wykrywanie i rekalkulacja sum kontrolnych (CRC16/32, modular 16-bit, algorytmy Bosch/VAG). |

---

### 3. Wdrożenie warstwy binarnej (Clean Architecture + MVI)

#### A. Agregat Danych Binarnych ECU

### app/src/main/java/com/winols/app/core/binary/EcuBinaryBuffer.kt