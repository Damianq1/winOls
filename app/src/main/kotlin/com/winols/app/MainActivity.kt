package com.winols.app

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.winols.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ecuTableView.onSelectionChangedListener = { selRows, selCols ->
            binding.selectionInfo.text = "Zaznaczono: $selRows wierszy × $selCols kolumn (${selRows * selCols} komórek)"
        }

        binding.btnBulkEdit.setOnClickListener {
            if (!binding.ecuTableView.hasSelection()) {
                Toast.makeText(this, "Najpierw zaznacz komórki na mapie!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showBulkEditDialog()
        }
    }

    private fun showBulkEditDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_bulk_edit, null)
        val etValue = dialogView.findViewById<TextInputEditText>(R.id.etValue)
        val inputLayout = dialogView.findViewById<TextInputLayout>(R.id.inputLayoutValue)
        val rbSmooth = dialogView.findViewById<RadioButton>(R.id.rbSmooth)
        val rbOffset = dialogView.findViewById<RadioButton>(R.id.rbOffset)
        val rbPercent = dialogView.findViewById<RadioButton>(R.id.rbPercent)
        val rbSetValue = dialogView.findViewById<RadioButton>(R.id.rbSetValue)

        rbSmooth.setOnCheckedChangeListener { _, isChecked ->
            inputLayout.isEnabled = !isChecked
            if (isChecked) etValue.setText("")
        }

        MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton("Zastosuj") { _, _ ->
                val text = etValue.text.toString().trim()

                when {
                    rbSmooth.isChecked -> {
                        binding.ecuTableView.applySmoothing()
                    }
                    rbOffset.isChecked -> {
                        text.toIntOrNull()?.let { binding.ecuTableView.applyOffset(it) }
                            ?: showToastError()
                    }
                    rbPercent.isChecked -> {
                        text.toDoubleOrNull()?.let { binding.ecuTableView.applyPercentage(it) }
                            ?: showToastError()
                    }
                    rbSetValue.isChecked -> {
                        text.toIntOrNull()?.let { binding.ecuTableView.applyFixedValue(it) }
                            ?: showToastError()
                    }
                }
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    private fun showToastError() {
        Toast.makeText(this, "Podaj poprawną wartość liczbową", Toast.LENGTH_SHORT).show()
    }
}