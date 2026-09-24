package com.winols.app.engine

import com.winols.app.model.DataType
import kotlin.math.roundToLong

/**
 * Silnik obliczeniowy przekształceń liniowych dla parametrów kalibracyjnych ECU.
 *
 * Równanie bazowe:
 *   Oto implementacja przeliczania wartości RAW na wartości fizyczne oraz operacji odwrotnej (inżynieria odwrotna/edycja map) wraz z testami jednostkowymi, dopasowana do struktury projektu typu WinOLS.

### app/src/main/java/com/winols/app/model/MapDefinition.kt