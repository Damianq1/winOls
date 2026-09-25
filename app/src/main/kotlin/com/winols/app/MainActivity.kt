package com.winols.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.winols.app.databinding.ActivityMainBinding
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { loadBinary(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnOpenFile.setOnClickListener {
            filePickerLauncher.launch(arrayOf("*/*"))
        }
    }

    private fun loadBinary(uri: Uri) {
        contentResolver.openInputStream(uri)?.use { stream: InputStream ->
            val buffer = ByteArray(512)
            val bytesRead = stream.read(buffer)
            if (bytesRead > 0) {
                binding.tvHexDump.text = buffer.take(bytesRead).joinToString(" ") { "%02X".format(it) }
            }
        }
    }
}