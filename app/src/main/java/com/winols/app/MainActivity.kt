package com.winols.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.winols.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        binding.btnOpenFile.setOnClickListener {
            binding.tvStatus.text = "Oczekiwanie na wybór pliku bin..."
            // TODO: Integracja Storage Access Framework (SAF) / FilePicker
        }
    }
}