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
import com.winols.app.presentation.MainIntent
import com.winols.app.presentation.MainState
import com.winols.app.presentation.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { readBinaryFromUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLoadFile.setOnClickListener {
            filePickerLauncher.launch(arrayOf("*/*"))
        }

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
                binding.tvHexDump.text = "No ECU dump loaded. Select a .bin file."
            }
            is MainState.Loading -> {
                binding.tvHexDump.text = "Parsing binary..."
            }
            is MainState.Loaded -> {
                binding.topAppBar.subtitle = "${state.binary.fileName} (${state.binary.sizeInBytes} bytes)"
                binding.tvHexDump.text = state.previewHex
            }
            is MainState.Error -> {
                Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                binding.tvHexDump.text = "Error loading file."
            }
        }
    }

    private fun readBinaryFromUri(uri: Uri) {
        contentResolver.openInputStream(uri)?.use { stream ->
            val bytes = stream.readBytes()
            val fileName = uri.lastPathSegment ?: "ecu_dump.bin"
            viewModel.processIntent(MainIntent.LoadBinary(bytes, fileName))
        }
    }
}