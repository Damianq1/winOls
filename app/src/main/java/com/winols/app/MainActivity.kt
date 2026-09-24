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
import com.winols.app.model.Endianness
import com.winols.app.model.MapDefinition
import com.winols.app.model.NumberBase
import com.winols.app.model.WordWidth
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
        setupRepresentationToolbar()
        observeModel()
    }

    private fun setupRepresentationToolbar() {
        binding.btnQuickHexDec.setOnClickListener {
            binding.ecuGridView.toggleHexDec()
            updateToolbarLabels()
        }

        binding.btnQuickWidth.setOnClickListener {
            binding.ecuGridView.toggleBitWidth()
            updateToolbarLabels()
        }

        binding.btnQuickEndian.setOnClickListener {
            binding.ecuGridView.toggleEndianness()
            updateToolbarLabels()
        }

        binding.btnQuickFormula.setOnClickListener {
            binding.ecuGridView.toggleEngineeringFormula()
            updateToolbarLabels()
        }

        updateToolbarLabels()
    }

    private fun updateToolbarLabels() {
        val cfg = binding.ecuGridView.displayConfig

        binding.btnQuickHexDec.text = when (cfg.base) {
            NumberBase.HEX -> "HEX"
            NumberBase.DECIMAL_UNSIGNED -> "DEC (Unsigned)"
            NumberBase.DECIMAL_SIGNED -> "DEC (Signed)"
        }

        binding.btnQuickWidth.text = "${cfg.wordWidth.byteSize * 8}-Bit"

        binding.btnQuickEndian.text = if (cfg.endianness == Endianness.LITTLE_ENDIAN) {
            "Lo/Hi (LE)"
        } else {
            "Hi/Lo (BE)"
        }

        binding.btnQuickFormula.text = if (cfg.applyFormulas) "Wzór (Fakt/Off)" else "Wart. Surowe"
    }

    private fun observeModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    binModel.loadingState.collect { state ->
                        when (state) {
                            is BinLoadingState.Loading -> {
                                binding.btnLoadBin.isEnabled = false
                                Toast.makeText(this@MainActivity, "Wczytywanie i analiza...", Toast.LENGTH_SHORT).show()
                            }
                            is BinLoadingState.Success -> {
                                binding.btnLoadBin.isEnabled = true
                                Toast.makeText(
                                    this@MainActivity,
                                    "Wczytano: ${state.byteCount} B. Znaleziono map: ${state.mapCount}",
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

            val cfg = binding.ecuGridView.displayConfig
            val activeType = cfg.toDataType()
            val step = activeType.byteSize
            val cellAddress = map.startAddress + ((cell.first * map.columns + cell.second) * step)

            val rawVal = binModel.bufferManager.readValue(cellAddress,Oto kompletna implementacja obsługi formatowania i dekodowania danych binarnych dla edytora HEX/Map (8-bit, 16-bit, signed/unsigned oraz porządku bajtów Little Endian / Big Endian).

### app/src/main/java/com/winols/app/data/HexFormatManager.kt