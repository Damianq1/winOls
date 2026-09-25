package com.winols.app

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.winols.app.databinding.ActivityMainBinding
import com.winols.app.presentation.EcuUiState
import com.winols.app.presentation.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { handleSelectedFile(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeState()
    }

    private fun setupListeners() {
        binding.btnOpenFile.setOnClickListener {
            openDocumentLauncher.launch(arrayOf("application/octet-stream", "*/*"))
        }
    }

    private fun handleSelectedFile(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes = inputStream.readBytes()
                val name = uri.lastPathSegment ?: "ecu_dump.bin"
                viewModel.loadBinary(name, bytes)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Błąd odczytu: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is EcuUiState.Idle -> {
                            binding.tvFileInfo.text = "Wybierz plik *.bin / *.ori do edycji."
                            binding.btnExportFile.isEnabled = false
                        }
                        is EcuUiState.Loading -> {
                            binding.tvFileInfo.text = "Ładowanie wsadu ECU..."
                        }
                        is EcuUiState.Loaded -> {
                            val info = "Plik: ${state.fileName} | Rozmiar: ${state.buffer.size} B | Checksum: ${state.checksum}"
                            binding.tvFileInfo.text = info
                            binding.btnExportFile.isEnabled = true
                        }
                        is EcuUiState.Error -> {
                            binding.tvFileInfo.text = "Błąd: ${state.message}"
                            binding.btnExportFile.isEnabled = false
                        }
                    }
                }
            }
        }
    }
}