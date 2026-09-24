package com.winols.app.model

import java.nio.ByteOrder

enum class BitDepth(val bytes: Int) {
    BIT_8(1),
    BIT_16(2),
    BIT_32(4)
Oto implementacja modułów odpowiedzialnych za szybkie przełączanie reprezentacji danych binarnych (HEX z pełną konfiguracją formatowania, widok 2D – profil/fala oraz reprezentacja 3D/tabela mapy), zoptymalizowana pod kątem wydajności pracy z dużymi buforami ECU.

### app/src/main/java/com/winols/app/model/DataRepresentation.kt