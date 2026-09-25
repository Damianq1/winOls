package com.winols.app.domain.repository

import android.content.Context
import android.net.Uri
import com.winols.app.domain.model.EcuMemoryBuffer
import com.winols.app.domain.parser.IntelHexParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class EcuBinaryRepository(private val context: Context) {

    private val hexParser = IntelHexParser()

    suspend fun loadFile(uri: Uri, fileName: String): Result<EcuMemoryBuffer> = withContext(Dispatchers.IO) {
        runCatching {
            val extension = fileName.substringAfterLast('.', "").lowercase()

            val inputStream: InputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Cannot open stream for URI: $uri")

            inputStream.use { stream ->
                when (extension) {
                    "hex" -> hexParser.parse(stream)
                    "bin", "ori", "mod" -> {
                        val bytes = stream.readBytes()
                        EcuMemoryBuffer(bytes, baseAddress = 0x00000000L)
                    }
                    else -> {
                        // Fallback: próba detekcji formatu po pierwszym bajcie
                        val bufferedStream = stream.buffered()
                        bufferedStream.mark(2)
                        val firstChar = bufferedStream.read().toChar()
                        bufferedStream.reset()

                        if (firstChar == ':') {
                            hexParser.parse(bufferedStream)
                        } else {
                            EcuMemoryBuffer(bufferedStream.readBytes(), baseAddress = 0x00000000L)
                        }
                    }
                }
            }
        }
    }

    suspend fun saveBinary(uri: Uri, buffer: EcuMemoryBuffer): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(buffer.rawBytes)
                outputStream.flush()
            } ?: throw IllegalStateException("Failed to open output stream")
        }
    }
}