package com.winols.app

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.winols.app.databinding.ActivityMainBinding
import com.winols.app.domain.model.EcuBinaryBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var ecuBuffer: EcuBinaryBuffer? = null

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { loadBinaryFromUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLoadFile.setOnClickListener {
            openDocumentLauncher.launch(arrayOf("*/*"))
        }
    }

    private fun loadBinaryFromUri(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes = stream.readBytes()
                    val buffer = EcuBinaryBuffer(bytes)
                    ecuBuffer = buffer

                    val preview = buffer.getHexDumpPreview(512)
                    withContext(Dispatchers.Main) {
                        binding.tvFileInfo.text = "Rozmiar: ${bytes.size} bajtów (${bytes.size / 1024} KB)"
                        binding.tvHexDump.text = preview
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Błąd wczytywania: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}