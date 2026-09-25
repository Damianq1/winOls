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

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { loadBinary(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnOpenFile.setOnClickListener {
            openDocumentLauncher.launch(arrayOf("*/*"))
        }
    }

    private fun loadBinary(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bytes = contentResolver.openInputStream(uri)?.use { inputStream: InputStream ->
                    val buffer = ByteArray(256)
                    val read = inputStream.read(buffer)
                    if (read > 0) buffer.copyOf(read) else ByteArray(0)
                } ?: ByteArray(0)

                val hexString = bytes.joinToString(" ") { byte -> "%02X".format(byte) }

                withContext(Dispatchers.Main) {
                    binding.tvStatus.text = "Załadowano: ${uri.lastPathSegment ?: "Plik"}"
                    binding.tvHexDump.text = if (hexString.isNotEmpty()) {
                        "Pierwsze ${bytes.size} bajtów:\n\n$hexString"
                    } else {
                        "Plik jest pusty."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Błąd odczytu: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}