package com.winols.app

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.winols.app.databinding.ActivityMainBinding
import com.winols.app.presentation.mvi.EcuIntent
import com.winols.app.presentation.mvi.EcuSingleEvent
import com.winols.app.presentation.mvi.EcuViewState
import com.winols.app.presentation.viewmodel.EcuViewModel
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: EcuViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeState()
    }

    private fun setupListeners() {
        binding.btnLoadDummy.setOnClickListener {
            val dummyBytes = ByteArray(1024)
            Random.nextBytes(dummyBytes)
            viewModel.handleIntent(
                EcuIntent.LoadBinaryData(
                    name = "EDC16_Demo_Dump.bin",
                    bytes = dummyBytes
                )
            )
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect { render(it) } }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is EcuSingleEvent.ShowToast -> Toast.makeText(
                                this@MainActivity,
                                event.message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }

    private fun render(state: EcuViewState) {
        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        
        state.errorMessage?.let {
            binding.tvStatus.text = "Błąd: $it"
            binding.tvStatus.setTextColor(getColor(android.R.color.holo_red_light))
            return
        }

        if (state.binary != null) {
            binding.tvStatus.text = "Plik: ${state.binary.fileName} (${state.binary.size} B)"
            binding.tvStatus.setTextColor(getColor(android.R.color.white))
            binding.tvHexContent.text = state.hexDump
        } else {
            binding.tvStatus.text = "Brak załadowanego wsadu binarnego."
            binding.tvHexContent.text = ""
        }
    }
}