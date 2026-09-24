package com.winols.app

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.winols.app.databinding.ActivityMainBinding
import com.winols.app.engine.EngineTaskState
import com.winols.app.engine.ProjectSessionManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val sessionManager = ProjectSessionManager()

    private val openFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { loadFileAsync(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeEngineState()
    }

    private fun setupListeners() {
        binding.btnOpenFile?.setOnClickListener {
            openFileLauncher.launch(arrayOf("*/*"))
        }

        binding.btnScanMaps?.setOnClickListener {
            lifecycleScope.launch {
                sessionManager.scanForMaps()
            }
        }
    }

    private fun loadFileAsync(uri: Uri) {
        lifecycleScope.launch {
            sessionManager.loadBinaryFromUri(applicationContext, uri)
                .onSuccess {
                    binding.hexGridView?.invalidate()
                }
        }
    }

    private fun observeEngineState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sessionManager.taskState.collect { state ->
                    when (state) {
                        is EngineTaskState.Idle -> {
                            binding.progressBar?.visibility = View.GONE
                        }
                        is EngineTaskState.Loading -> {
                            binding.progressBar?.apply {
                                visibility = View.VISIBLE
                                isIndeterminate = true
                            }
                            binding.txtStatus?.text = state.message
                        }
                        is EngineTaskState.Scanning -> {
                            binding.progressBar?.apply {
                                visibility = View.VISIBLE
                                isIndeterminate = false
                                max = state.totalBytes
                                progress = state.scannedBytes
                            }
                            binding.txtStatus?.text =
                                "Skanowanie: ${state.scannedBytes / 1024} KB / ${state.totalBytes / 1024} KB (Wykryto: ${state.mapsFound})"
                        }
                        is EngineTaskState.Success -> {
                            binding.progressBar?.visibility = View.GONE
                            binding.txtStatus?.text = state.info
                        }
                        is EngineTaskState.Error -> {
                            binding.progressBar?.visibility = View.GONE
                            binding.txtStatus?.text = "Błąd: ${state.throwable.localizedMessage}"
                            Toast.makeText(this@MainActivity, state.throwable.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }
}