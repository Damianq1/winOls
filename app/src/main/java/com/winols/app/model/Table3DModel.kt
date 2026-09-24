package com.winols.app.model

/**
 * Reprezentacja danych tabeli 3D (mapa powierzchniowa z dwiema osiami i macierzą wartości).
 */
data class Table3DModel(
    val id: String,
    val name: String,
    val startAddress: Long,
    val xAxis: AxisData,
    val yAxis: AxisData,
    val zValues: Array<DoubleArray>, // Wymiary: [yRows][xCols]
    val factor: Double = 1.0,
    val offset: Double = 0.0,
    val unit: String = ""
) {
    val xSize: Int get() = xAxis.values.size
    val ySize: Int get() = yAxis.values.size

    fun getDisplayZ(row: Int, col: Int): Double {
        return (zValues[row][col] * factor) + offset
    }

    fun setRawZ(row: Int, col: Int, rawValue: Double) {
        zValues[row][col] = rawValue
    }

    fun setDisplayZ(row: Int, col: Int, displayValue: Double) {
        if (factor != 0.0) {
            zValues[row][col] = (displayValue - offset) / factor
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Table3DModel
        if (id != other.id) return false
        if (xAxis != other.xAxis) return false
        if (yAxis != other.yAxis) return false
        if (!zValues.contentDeepEquals(other.zValues)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + xAxis.hashCode()
        result = 31 * result + yAxis.hashCode()
        result = 31 * result + zValues.contentDeepHashCode()
        return result
    }
}

data class AxisData(
    val name: String,
    val unit: String,
    val address: Long = 0L,
    val values: DoubleArray,
    val factor: Double = 1.0,
    val offset: Double = 0.0
) {
    fun getDisplayValue(index: Int): Double {
        return (values[index] * factor) + offset
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AxisData
        if (name != other.name) return false
        if (unit != other.unit) return false
        if (!values.contentEquals(other.values)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + unit.hashCode()
        result = 31 * result + values.contentHashCode()
        return result
    }
}