package com.winols.app

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.winols.app.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val openFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { loadBinaryFile(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnOpenFile.setOnClickListener {
            openFileLauncher.launch("*/*")
        }
    }

    private fun loadBinaryFile(uri: Uri) {
        binding.tvFileName.text = uri.lastPathSegment ?: "Wybrany plik"
        binding.tvHexDump.text = "Ładowanie danych binarnych..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val buffer = ByteArray(2048) // Podgląd pierwszych 2 KB pliku
                    val bytesRead = inputStream.read(buffer)
                    val hexDump = if (bytesRead > 0) {
                        formatHexDump(buffer.copyOf(bytesRead))
                    } else {
                        "Plik jest pusty."
                    }

                    withContext(Dispatchers.Main) {
                        binding.tvHexDump.text = hexDump
                    }
                } ?: withContext(Dispatchers.Main) {
                    binding.tvHexDump.text = "Nie udało się otworzyć strumienia pliku."
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Błąd odczytu: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.tvHexDump.text = "Błąd: ${e.localizedMessage}"
                }
            }
        }
    }

    private fun formatHexDump(data: ByteArray): String {
        val sb = StringBuilder()
        val rowSize = 16

        for (i in data.indices step rowSize) {
            sb.append(String.format("%08X: ", i))

            val end = minOf(i + rowSize, data.size)
            for (j in i until end) {
                sb.append(String.format("%02X ", data[j]))
            }

            if (end - i < rowSize) {
                val pad = (rowSize - (end - i)) * 3
                sb.append(" ".repeat(pad))
            }

            sb.append(" |")
            for (j in i until end) {
                val b = data[j].toInt().toChar()
                if (b in ' '..'~') {
                    sb.append(b)
                } else {
                    sb.append('.')
                }
            }
            sb.append("|\n")
        }
        return sb.toString()
    }
}