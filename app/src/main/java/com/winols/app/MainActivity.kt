package com.winols.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.winols.app.databinding.ActivityMainBinding
import com.winols.app.engine.BinaryProject
import com.winols.app.engine.ByteOrderType
import com.winols.app.engine.DataWidth
import com.winols.app.engine.EcuMapDefinition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var binaryProject: BinaryProject? = null

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                loadBinaryFromUri(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLoadBinary.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            filePickerLauncher.launch(intent)
        }
    }

    private fun loadBinaryFromUri(uri: android.net.Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes = stream.readBytes()
                    val project = BinaryProject(bytes)
                    binaryProject = project

                    val previewHex = generateHexPreview(bytes, 512)
                    withContext(Dispatchers.Main) {
                        binding.tvProjectInfo.text = "Rozmiar: ${bytes.size / 1024} KB"
                        binding.tvHexDump.text = previewHex
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Błąd wczytywania: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun generateHexPreview(bytes: ByteArray, limit: Int): String {
        val max = minOf(bytes.size, limit)
        val sb = StringBuilder()
        for (i in 0 until max step 16) {
            sb.append(String.format("%08X: ", i))
            val lineBytes = bytes.sliceArray(i until minOf(i + 16, max))
            for (b in lineBytes) {
                sb.append(String.format("%02X ", b))
            }
            sb.append("\n")
        }
        return sb.toString()
    }
}