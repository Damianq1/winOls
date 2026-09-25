package com.winols.app.data.export

import com.winols.app.domain.model.EcuMap
import com.winols.app.domain.model.ModifiedVariable
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * Eksporter struktur map oraz modyfikacji do pliku CSV zoptymalizowanego
 * pod otwieranie w Microsoft Excel (UTF-8 BOM, separator średnikowy ';').
 */
class CsvExporter(private val delimiter: String = ";") {

    /**
     * Zapisuje do strumienia pełny raport: zdefiniowane mapy oraz listę zmienionych zmiennych.
     */
    fun exportFullReport(
        outputStream: OutputStream,
        maps: List<EcuMap>,
        changes: List<ModifiedVariable>
    ) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8))
        
        // Zapis BOM (Byte Order Mark), aby Excel poprawnie rozpoznał kodowanie UTF-8
        outputStream.write(0xEF)
        outputStream.write(0xBB)
        outputStream.write(0xBF)

        writer.use { out ->
            // --- SEKCJA 1: ZDEFINIOWANE MAPY ---
            out.write("=== ZDEFINIOWANE MAPY ECU ===")
            out.newLine()
            val mapHeaders = listOf(
                "ID", "Nazwa Mapy", "Adres Poczatkowy (HEX)", "Wiersze",
                "Kolumny", "Typ Danych", "Wspolczynnik (Factor)", "Offset", "Jednostka", "Opis"
            )
            out.write(mapHeaders.joinToString(delimiter) { escape(it) })
            out.newLine()

            for (map in maps) {
                val row = listOf(
                    map.id,
                    map.name,
                    String.format(Locale.US, "0x%06X", map.startAddress),
                    map.rows.toString(),
                    map.columns.toString(),
                    map.dataType.name,
                    String.format(Locale.US, "%.6f", map.factor),
                    String.format(Locale.US, "%.4f", map.offset),
                    map.unit,
                    map.description
                )
                out.write(row.joinToString(delimiter) { escape(it) })
                out.newLine()
            }

            out.newLine()
            out.newLine()

            // --- SEKCJA 2: ZMIENIONE ZMIENNE / PUNKTY MAP ---
            out.write("=== ZMIENIONE WARTOSCI / ZMIENNE ===")
            out.newLine()
            val changeHeaders = listOf(
                "Adres (HEX)", "Nazwa Mapy", "Oryginalna Wartosc (Raw)", "Nowa Wartosc (Raw)",
                "Delta (Raw)", "Oryginalna Fizyczna", "Nowa Fizyczna", "Delta Fizyczna", "Zmiana (%)", "Jednostka"
            )
            out.write(changeHeaders.joinToString(delimiter) { escape(it) })
            out.newLine()

            for (change in changes) {
                val row = listOf(
                    change.formattedAddress(),
                    change.mapName ?: "Nieprzypisana",
                    change.originalRawValue.toString(),
                    change.modifiedRawValue.toString(),
                    change.deltaRaw.toString(),
                    String.format(Locale.US, "%.4f", change.originalPhysicalValue),
                    String.format(Locale.US, "%.4f", change.modifiedPhysicalValue),
                    String.format(Locale.US, "%.4f", change.deltaPhysical),
                    String.format(Locale.US, "%.2f%%", change.percentChange),
                    change.unit
                )
                out.write(row.joinToString(delimiter) { escape(it) })
                out.newLine()
            }
        }
    }

    /**
     * Eksport pojedynczej macierzy mapy w układzie 2D (tabela wiersze x kolumny).
     */
    fun exportMapMatrix(
        outputStream: OutputStream,
        map: EcuMap,
        matrixValues: Array<DoubleArray>
    ) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8))
        outputStream.write(0xEF)
        outputStream.write(0xBB)
        outputStream.write(0xBF)

        writer.use { out ->
            out.write("Mapa: ${map.name} (${String.format(Locale.US, "0x%06X", map.startAddress)}) [${map.unit}]")
            out.newLine()

            for (r in 0 until map.rows) {
                val rowValues = mutableListOf<String>()
                for (c in 0 until map.columns) {
                    val value = matrixValues.getOrNull(r)?.getOrNull(c) ?: 0.0
                    rowValues.add(String.format(Locale.US, "%.3f", value))
                }
                out.write(rowValues.joinToString(delimiter))
                out.newLine()
            }
        }
    }

    private fun escape(data: String): String {
        var escaped = data.replace("\"", "\"\"")
        if (escaped.contains(delimiter) || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            escaped = "\"$escaped\""
        }
        return escaped
    }
}