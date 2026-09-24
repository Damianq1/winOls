package com.winols.app.model

data class PhysicalConversion(
    val factor: Double = 1.0,
    val offset: Double = 0.0,
    val unit: String = "",
    val precision: Int = 2
) {
    fun rawToPhysical(raw: Long): Double {
        return (raw * factor) + offset
    }

    fun physicalToRaw(physical: Double): Long {
        if (factor == 0.0) return 0L
        return Math.round((physical - offset) / factor)
    }

    fun format(physicalValue: Double): String {
        return String.format("%.${precision}f", physicalValue)
    }
}