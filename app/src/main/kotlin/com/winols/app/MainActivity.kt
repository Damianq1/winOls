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
        binding.topAppBar.subtitle = "ECU Hex/Map Editor"
        binding.btnOpenFile.setOnClickListener {
            // Miejsce pod wywołanie ActivityResultLauncher do odczytu SAF (Storage Access Framework)
        }
    }
}