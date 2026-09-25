package com.winols.app

import android.app.Activity
import android.content.Intent
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
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> readBytesFromUri(uri) }
        }
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
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            filePickerLauncher.launch(intent)
        }

        binding.btnScanMaps.setOnClickListener {
            viewModel.handleIntent(MainIntent.ScanForMaps)
        }
    }

    private fun readBytesFromUri(uri: Uri) {
        contentResolver.openInputStream(uri)?.use { stream ->
            val bytes = stream.readBytes()
            val fileName = uri.lastPathSegment ?: "ecu_dump.bin"
            viewModel.handleIntent(MainIntent.LoadBinary(fileName, bytes))
        } ?: Toast.makeText(this, "Nie udało się otworzyć pliku", Toast.LENGTH_SHORT).show()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: MainState) {
        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        binding.btnScanMaps.isEnabled = state.binary != null && !state.isLoading

        state.binary?.let {
            binding.tvFileInfo.text = "Plik: ${it.fileName} | Rozmiar: ${it.sizeInBytes / 1024} KB"
        } ?: run {
            binding.tvFileInfo.text = "Nie załadowano pliku binarnego"
        }

        binding.tvMapCount.text = "Wykryte mapy: ${state.maps.size}"
        
        state.error?.let {
            Toast.makeText(this, it, Toast.LENGTH_LONG).show()
        }
    }
}