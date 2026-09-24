package com.winols.app.export

enum class ExportFormat {
    CSV,
    XLSX
}

enum class ValueMode {
    PHYSICAL, // Przeliczone: RAW * factor + offset
    RAW       // Surowe bity z bufora
}

data class ExportOptions(
    val format: ExportFormat = ExportFormat.XLSX,
    val valueMode: ValueMode = ValueMode.PHYSICAL,
    val includeDifferenceTable: Boolean = true,
    val includeDeltaPercentage: Boolean = true,
    val decimalPrecision: Int = 2
)