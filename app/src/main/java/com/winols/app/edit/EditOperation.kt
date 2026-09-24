package com.winols.app.edit

/**
 * Typy operacji edycyjnych na macierzy komórek mapy ECU.
 */
enum class EditOperationType {
    SET_ABSOLUTE,
    ADD_OFFSET,
    MULTIPLY_PERCENT,
    INTERPOLATE_HORIZONTAL,
    INTERPOLATE_VERTICAL,
    INTERPOLATE_2D
}

/**
 * Pojedyncza zmiana komórki na potrzeby historii operacji (Undo/Redo).
 */
data class CellChange(
    val row: Int,
    val col: Int,
    val oldRawValue: Double,
    val newRawValue: Double,
    val oldPhysicalValue: Double,
    val newPhysicalValue: Double
)

/**
 * Zestaw zmian wykonanych w ramach jednej akcji użytkownika.
 */
data class MatrixEditTransaction(
    val type: EditOperationType,
    val timestamp: Long = System.currentTimeMillis(),
    val changes: List<CellChange>
)