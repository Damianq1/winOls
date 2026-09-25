package com.winols.app.data.export

import android.content.Context
import android.net.Uri
import com.winols.app.domain.model.EcuMap
import com.winols.app.domain.model.ModifiedVariable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Menadżer zarządzający operacjami wejścia/wyjścia podczas eksportu
 * przy integracji z Android Storage Access Framework (SAF).
 */
class ExportManager(private val context: Context) {

    private val csvExporter = CsvExporter(delimiter = ";")

    /**
     * Eksportuje zdefiniowane mapy i zmiany bezpośrednio do wybranego URI (np. z CreateDocument intent).
     */
    suspend fun exportToUri(
        targetUri: Uri,
        maps: List<EcuMap>,
        changes: List<ModifiedVariable>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                csvExporter.exportFullReport(outputStream, maps, changes)
            } ?: throw IllegalStateException("Nie można otworzyć strumienia zapisu do wskazanego URI.")
        }
    }
}