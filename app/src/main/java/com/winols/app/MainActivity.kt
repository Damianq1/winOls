package com.winols.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.winols.app.databinding.ActivityMainBinding
import com.winols.app.edit.MapEditor
import com.winols.app.model.MapDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class EditorViewMode {
    TABLE_GRID,
    CURVE_2D,
    SURFACE_3D
}

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val binModel = BinModel()
    private lateinit var mapEditor: MapEditor
    private var currentSelectedMap: MapDefinition? = null
    private var currentMode = EditorViewMode.TABLE_GRID

    companion object {
        private const val REQUEST_CODE_OPEN_BIN = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapEditor = MapEditor(binModel)
        setupListeners()
        observeModel()
    }

    private fun observeModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    binModel.loadingState.collect { state ->
                        when (state) {
                            is BinLoadingState.Loading -> {
                                binding.btnLoadBin.isEnabled = false
                                Toast.makeText(this@MainActivity, "Wczytywanie i skanowanie w tle...", Toast.LENGTH_SHORT).show()
                            }
                            is BinLoadingState.Success -> {
                                binding.btnLoadBin.isEnabled = true
                                Toast.makeText(
                                    this@MainActivity,
                                    "Załadowano ${state.byteCount} bajtów. Znaleziono map: ${state.mapCount}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            is BinLoadingState.Error -> {
                                binding.btnLoadBin.isEnabled = true
                                Toast.makeText(this@MainActivity, "Błąd: ${state.message}", Toast.LENGTH_LONG).show()
                            }
                            BinLoadingState.Idle -> {
                                binding.btnLoadBin.isEnabled = true
                            }
                        }
                    }
                }

                launch {
                    binModel.mapsFlow.collect { maps ->
                        if (maps.isNotEmpty() && currentSelectedMap == null) {
                            currentSelectedMap = maps[0]
                            bindActiveViews(maps[0])
                        }
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnLoadBin.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            startActivityForResult(intent, REQUEST_CODE_OPEN_BIN)
        }

        binding.btnSelectMap.setOnClickListener {
            val maps = binModel.maps
            if (maps.isEmpty()) {
                Toast.makeText(this, "Brak zidentyfikowanych map w pliku", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val mapNames = maps.map { it.name }.toTypedArray()
            AlertDialog.Builder(this)
                .setTitle("Wybierz mapę ECU")
                .setItems(mapNames) { _, which ->
                    val chosen = maps[which]
                    currentSelectedMap = chosen
                    bindActiveViews(chosen)
                }
                .show()
        }

        binding.btnToggleViewMode.setOnClickListener {
            currentMode = when (currentMode) {
                EditorViewMode.TABLE_GRID -> EditorViewMode.CURVE_2D
                EditorViewMode.CURVE_2D -> EditorViewMode.SURFACE_3D
                EditorViewMode.SURFACE_3D -> EditorViewMode.TABLE_GRID
            }
            updateViewModeUI()
        }

        binding.btnModifyCell.setOnClickListener {
            val map = currentSelectedMap ?: return@setOnClickListener
            val cell = binding.ecuGridView.selectedCell ?: return@setOnClickListener

            val input = EditText(this).apply {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                setText(binModel.getCellValue(map, cell.first, cell.second).toString())
            }

            AlertDialog.Builder(this)
                .setTitle("Edycja komórki [R:${cell.first}, C:${cell.second}]")
                .setView(input)
                .setPositiveButton("Zapisz") { _, _ ->
                    val newVal = input.text.toString().toDoubleOrNull()
                    if (newVal != null) {
                        mapEditor.modifySingleCell(map, cell.first, cell.second, newVal)
                        bindActiveViews(map)
                    }
                }
                .setNegativeButton("Anuluj", null)
                .show()
        }
    }

    private fun updateViewModeUI() {
        binding.btnToggleViewMode.text = when (currentMode) {
            EditorViewMode.TABLE_GRID -> "Tryb: Tabela"
            EditorViewMode.CURVE_2D -> "Tryb: Wykres 2D"
            EditorViewMode.SURFACE_3D -> "Tryb: Siatka 3D"
        }

        binding.ecuGridView.visibility = if (currentMode == EditorViewMode.TABLE_GRID) View.VISIBLE else View.GONE
        binding.ecuCurve2DView.visibility = if (currentMode == EditorViewMode.CURVE_2D) View.VISIBLE else View.GONE
        binding.ecuSurface3DView.visibility = if (currentMode == EditorViewMode.SURFACE_3D) View.VISIBLE else View.GONE

        currentSelectedMap?.let { bindActiveViews(it) }
    }

    private fun bindActiveViews(map: MapDefinition) {
        when (currentMode) {
            EditorViewMode.TABLE_GRID -> binding.ecuGridView.bind(binModel, map)
            EditorViewMode.CURVE_2D -> binding.ecuCurve2DView.bind(binModel, map, binding.ecuGridView.selectedCell?.first ?: 0)
            EditorViewMode.SURFACE_3D -> binding.ecuSurface3DView.bind(binModel, map)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OPEN_BIN && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri -> loadBinaryUriAsync(uri) }
        }
    }

    private fun loadBinaryUriAsync(uri: Uri) {
        lifecycleScope.launch {
            try {
                val stream = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)
                }
                if (stream != null) {
                    binModel.loadStreamAsync(stream)
                } else {
                    Toast.makeText(this@MainActivity, "Nie udało się otworzyć strumienia pliku", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Błąd odczytu: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}