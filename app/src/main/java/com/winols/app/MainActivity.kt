package com.winols.app

import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.winols.app.databinding.ActivityMainBinding
import com.winols.app.domain.model.MapTable
import com.winols.app.domain.model.SelectionArea

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mapTable: MapTable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initSampleMap()
        setupListeners()
    }

    private fun initSampleMap() {
        val rows = 16
        val cols = 16
        val initialData = IntArray(rows * cols) { idx ->
            val r = idx / cols
            val c = idx % cols
            // Przykładowa bazowa płaszczyzna momentu / dawki paliwa
            (800 + r * 150 + c * 80).coerceAtMost(0xFFFF)
        }

        mapTable = MapTable(rows, cols, initialData, is16Bit = true, isSigned = false)
        binding.tableMapView.mapTable = mapTable
    }

    private fun setupListeners() {
        binding.tableMapView.onSelectionChanged = { selection ->
            updateActionButtonsState(selection)
            updateSelectionStatus(selection)
        }

        binding.btnSelectAll.setOnClickListener {
            binding.tableMapView.selectAll()
        }

        binding.btnOffset.setOnClickListener {
            val sel = binding.tableMapView.selection ?: return@setOnClickListener
            showNumericDialog(
                title = "Dodaj/Odejmij wartość",
                hint = "np. 50 lub -100",
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
            ) { input ->
                val delta = input.toIntOrNull() ?: return@showNumericDialog
                mapTable.applyOffset(sel, delta)
                binding.tableMapView.invalidate()
            }
        }

        binding.btnPercent.setOnClickListener {
            val sel = binding.tableMapView.selection ?: return@setOnClickListener
            showNumericDialog(
                title = "Zmiana procentowa",
                hint = "np. 5.5 lub -10.0",
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
            ) { input ->
                val pct = input.toDoubleOrNull() ?: return@showNumericDialog
                mapTable.applyPercentage(sel, pct)
                binding.tableMapView.invalidate()
            }
        }

        binding.btnSetValue.setOnClickListener {
            val sel = binding.tableMapView.selection ?: return@setOnClickListener
            showNumericDialog(
                title = "Wklej stałą wartość",
                hint = "np. 2000",
                inputType = InputType.TYPE_CLASS_NUMBER
            ) { input ->
                val value = input.toIntOrNull() ?: return@showNumericDialog
                mapTable.setValue(sel, value)
                binding.tableMapView.invalidate()
            }
        }

        binding.btnSmooth.setOnClickListener {
            val sel = binding.tableMapView.selection ?: return@setOnClickListener
            mapTable.applySmoothing(sel)
            binding.tableMapView.invalidate()
        }

        updateActionButtonsState(null)
    }

    private fun updateActionButtonsState(selection: SelectionArea?) {
        val hasSelection = selection != null
        binding.btnOffset.isEnabled = hasSelection
        binding.btnPercent.isEnabled = hasSelection
        binding.btnSetValue.isEnabled = hasSelection
        binding.btnSmooth.isEnabled = hasSelection
    }

    private fun updateSelectionStatus(selection: SelectionArea?) {
        if (selection == null) {
            binding.tvSelectionStatus.text = "Brak zaznaczenia"
        } else {
            val count = (selection.maxRow - selection.minRow + 1) * (selection.maxCol - selection.minCol + 1)
            binding.tvSelectionStatus.text =
                "Zaznaczenie: R[${selection.minRow}..${selection.maxRow}], C[${selection.minCol}..${selection.maxCol}] ($count komórek)"
        }
    }

    private fun showNumericDialog(
        title: String,
        hint: String,
        inputType: Int,
        onConfirmed: (String) -> Unit
    ) {
        val input = EditText(this).apply {
            this.inputType = inputType
            this.hint = hint
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(input)
            .setPositiveButton("Zastosuj") { _, _ ->
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) {
                    onConfirmed(text)
                }
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }
}