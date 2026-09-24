package com.winols.app.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import com.winols.app.databinding.DialogBulkEditBinding
import com.winols.app.edit.MapEditor
import com.winols.app.model.MapDefinition

/**
 * Dialog obsługujący masową edycję mapy (w tym zmianę procentową +/- %).
 */
class BulkEditDialog(
    context: Context,
    private val mapDefinition: MapDefinition,
    private val selectedCells: List<Pair<Int, Int>>,
    private val mapEditor: MapEditor,
    private val onApplied: () -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogBulkEditBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogBulkEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        val modeOptions = listOf(
            "Zmiana procentowa (+/- %)",
            "Dodaj / Odejmij stałą (+/-)",
            "Ustaw stałą wartość (=)",
            "Wygładzanie (Smoothing)"
        )

        binding.spinnerMode.adapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_dropdown_item,
            modeOptions
        )

        val cellCount = if (selectedCells.isEmpty()) {
            mapDefinition.rows * mapDefinition.columns
        } else {
            selectedCells.size
        }
        binding.tvAffectedCells.text = "Wybrane komórki: $cellCount"

        binding.btnApply.setOnClickListener {
            val rawInput = binding.etValue.text?.toString()?.trim()
            val value = rawInput?.toDoubleOrNull()

            if (value == null) {
                Toast.makeText(context, "Podaj prawidłową wartość liczbową", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            when (binding.spinnerMode.selectedItemPosition) {
                0 -> { // Zmiana procentowa
                    val applyOnPhysical = binding.cbPhysicalScale.isChecked
                    mapEditor.applyPercentageChange(
                        map = mapDefinition,
                        selectedCells = selectedCells,
                        percentDelta = value,
                        applyOnPhysical = applyOnPhysical
                    )
                    onApplied()
                    dismiss()
                }
                // Pozostałe tryby edycji wywoływane analogicznie
            }
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }
}