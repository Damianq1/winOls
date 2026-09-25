package com.winols.app

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.winols.app.databinding.ActivityMainBinding
import com.winols.app.presentation.mvi.EditorIntent
import com.winols.app.presentation.viewmodel.EcuEditorViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: EcuEditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                binding.tvBinaryTitle.text = state.binaryFile?.let { 
                    "${it.fileName} (${it.size / 1024} KB)" 
                } ?: "Brak załadowanego pliku"
            }
        }
    }
}