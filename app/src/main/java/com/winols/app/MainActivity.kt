package com.winols.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.winols.app.core.binary.BinaryBuffer
import com.winols.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var loadedBinary: BinaryBuffer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
    }

    private fun initViews() {
        // Mock init: Bufor 512KB reprezentujący typowy sterownik EDC15/EDC16
        val emptyRom = ByteArray(512 * 1024)
        loadedBinary = BinaryBuffer.fromBytes(emptyRom)

        binding.statusTextView.text = getString(
            R.string.status_loaded,
            loadedBinary?.size?.div(1024) ?: 0
        )
    }
}