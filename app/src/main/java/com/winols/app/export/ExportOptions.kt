package com.winols.app.export

enum class ExportFormat {
    CSV,
    XLSX
}

enum class ValueMode {
    PHYSICAL,   // Po przeliczeniu (Factor / Offset)
    RAW         // Surowe bajty / liczby całkowite
}

data class ExportConfig(
    val format: ExportFormat = ExportFormat.CSV,
    val valueMode: ValueMode = ValueMode.PHYSICAL,
    val includeDelta: Boolean = true,
    val decimalPlaces: Int = 2,
    val csvDelimiter: String = ";"
)