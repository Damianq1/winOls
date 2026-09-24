package com.winols.app.cli

import com.winols.app.data.BinaryBufferManager
import com.winols.app.edit.MapEditor
import com.winols.app.engine.ChecksumEngine
import com.winols.app.engine.MapFinderEngine
import com.winols.app.export.ExcelExporter
import com.winols.app.model.DataType
import com.winols.app.model.MapDefinition
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Lekki interfejs wiersza poleceń (CLI) do uruchamiania operacji na plikach binarnych
 * bezpośrednio w Termux lub w procesach testowych bez konieczności uruchamiania pełnego UI Androida.
 */
object TermuxCliRunner {

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isEmpty()) {
            printHelp()
            return
        }

        when (args[0].lowercase()) {
            "scan" -> {
                if (args.size < 2) {
                    println("Użycie: scan <plik.bin>")
                    return
                }
                scanMaps(File(args[1]))
            }
            "checksum" -> {
                if (args.size < 2) {
                    println("Użycie: checksum <plik.bin>")
                    return
                }
                verifyChecksums(File(args[1]))
            }
            "export-diff" -> {
                if (args.size < 4) {
                    println("Użycie: export-diff <ori.bin> <mod.bin> <output.csv>")
                    return
                }
                exportDiff(File(args[1]), File(args[2]), File(args[3]))
            }
            else -> {
                println("Nieznane polecenie: ${args[0]}")
                printHelp()
            }
        }
    }

    private fun scanMaps(binFile: File) {
        if (!binFile.exists()) {
            println("Błąd: Plik ${binFile.absolutePath} nie istnieje.")
            return
        }

        println("Wczytywanie pliku: ${binFile.name} (${binFile.length()} bajtów)...")
        val data = binFile.readBytes()
        val bufferManager = BinaryBufferManager(data)
        val finder = MapFinderEngine(bufferManager)

        println("Skanowanie struktur nagłówkowych i map...")
        runBlocking {
            val maps = finder.scanPotentialMapsAsync()
            println("Znaleziono potencjalnych map: ${maps.size}")
            maps.take(20).forEachIndexed { idx, map ->
                val hexAddr = "0x" + Integer.toHexString(map.startAddress).uppercase().padStart(6, '0')
                println("[$idx] $hexAddr | Wymiary: ${map.cols}x${map.rows} | Typ: ${map.dataType} | Kat: ${map.category}")
            }
            if (maps.size > 20) {
                println("... i ${maps.size - 20} więcej.")
            }
        }
    }

    private fun verifyChecksums(binFile: File) {
        if (!binFile.exists()) {
            println("Błąd: Plik nie istnieje.")
            return
        }
        val data = binFile.readBytes()
        val bufferManager = BinaryBufferManager(data)
        val checksumEngine = ChecksumEngine(bufferManager)

        // Przykładowy blok weryfikacyjny (do rozbudowy o profile ECU)
        println("Analiza integralności pliku...")
        val sampleBlock = ChecksumEngine.ChecksumBlock(
            id = "BLOCK_GLOBAL",
            description = "Standard 16-bit Checksum",
            startAddress = 0,
            endAddress = (data.size - 3).coerceAtLeast(0),
            checksumAddress = (data.size - 2).coerceAtLeast(0),
            algorithm = ChecksumEngine.Algorithm.ADD16_LE
        )

        val isValid = checksumEngine.verify(sampleBlock)
        println("Wynik weryfikacji sumy kontrolnej: ${if (isValid) "OK" else "BŁĘDNA / WYMAGA PRZELICZENIA"}")
    }

    private fun exportDiff(oriFile: File, modFile: File, outputFile: File) {
        if (!oriFile.exists() || !modFile.exists()) {
            println("Błąd: Brak pliku ORI lub MOD.")
            return
        }

        val oriManager = BinaryBufferManager(oriFile.readBytes())
        val modManager = BinaryBufferManager(modFile.readBytes())

        // Domyślna definicja podglądu obszaru
        val sampleMap = MapDefinition(
            id = "DIFF_VIEW",
            name = "DiffAnalysis",
            unit = "raw",
            startAddress = 0x00,
            rows = 16,
            columns = 16,
            dataType = DataType.UINT8,
            factor = 1.0,
            additionOffset = 0.0
        )

        val oriEditor = MapEditor(oriManager, sampleMap)
        val modEditor = MapEditor(modManager, sampleMap)

        ExcelExporter.exportDiffToCsv(oriEditor, modEditor, outputFile)
        println("Raport różnicowy zapisany pomyślnie do: ${outputFile.absolutePath}")
    }

    private fun printHelp() {
        println(
            """
            WinOLS Termux CLI Engine
            Dostępne polecenia:
              scan <plik.bin>                       - Automatyczne wykrywanie map
              checksum <plik.bin>                   - Weryfikacja sum kontrolnych
              export-diff <ori.bin> <mod.bin> <out> - Eksport raportu różnic do CSV
            """.trimIndent()
        )
    }
}