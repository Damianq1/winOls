package com.winols.app

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.winols.app.databinding.ActivityMainBinding
import com.winols.app.presentation.main.MainIntent
import com.winols.app.presentation.main.MainState
import com.winols.app.presentation.main.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.handleIntent(MainIntent.LoadBinary(it)) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeState()
    }

    private fun setupListeners() {
        binding.btnLoadBinary.setOnClickListener {
            filePickerLauncher.launch("*/*")
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    render(state)
                }
            }
        }
    }

    private fun render(state: MainState) {
        when (state) {
            is MainState.Idle -> {
                binding.progressBar.visibility = View.GONE
                binding.statusText.text = "Wybierz plik mapy ECU (.bin) do analizy"
                binding.hexPreviewText.text = ""
            }
            is MainState.Loading -> {
                binding.progressBar.visibility = View.VISIBLE
                binding.statusText.text = "Odczytywanie danych binarnych..."
            }
            is MainState.Loaded -> {
                binding.progressBar.visibility = View.GONE
                val sizeKb = state.ecuBinary.sizeBytes / 1024.0
                binding.statusText.text = String.format(
                    "Plik: %s (%.2f KB) | CRC32: 0x%08X",
                    state.ecuBinary.fileName,
                    sizeKb,
                    state.ecuBinary.checksum
                )
                binding.hexPreviewText.text = state.previewHex
            }
            is MainState.Error -> {
                binding.progressBar.visibility = View.GONE
                binding.statusText.text = "Wystąpił błąd podczas wczytywania."
                Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}