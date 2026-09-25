package com.winols.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.winols.app.databinding.ActivityMainBinding
import com.winols.app.domain.repository.EcuBinaryRepository
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val repository by lazy { EcuBinaryRepository(applicationContext) }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { handleSelectedFile(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnOpenFile.setOnClickListener {
            // Akceptujemy surowe pliki binarne oraz pliki tekstowe/hex
            filePickerLauncher.launch(arrayOf("*/*"))
        }
    }

    private fun handleSelectedFile(uri: Uri) {
        val fileName = getFileName(uri) ?: "ecu_dump.bin"

        // Utrwalenie uprawnień dostępu do pliku w trybie odczytu/zapisu
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Ignorujemy, jeśli dostawca URI nie wspiera persistable permissions
        }

        lifecycleScope.launch {
            val result = repository.loadFile(uri, fileName)
            result.onSuccess { buffer ->
                Toast.makeText(
                    this@MainActivity,
                    "Wczytano: $fileName (${buffer.size} B, Base: 0x${buffer.baseAddress.toString(16).uppercase()})",
                    Toast.LENGTH_LONG
                ).show()
            }.onFailure { error ->
                Toast.makeText(this@MainActivity, "Błąd: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }
}