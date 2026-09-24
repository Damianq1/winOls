package com.winols.app.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import com.winols.app.R
import com.winols.app.edit.BulkEditMode

class BulkEditDialog(
    context: Context,
    private val onApply: (mode: BulkEditMode, value: Double) -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_bulk_edit)

        val radioGroup = findViewById<RadioGroup>(R.id.rg_bulk_operation)
        val editValue = findViewById<EditText>(R.id.et_operation_value)
        val btnApply = findViewById<Button>(R.id.btn_apply)
        val btnCancel = findViewById<Button>(R.id.btn_cancel)

        btnCancel.setOnClickListener { dismiss() }

        btnApply.setOnClickListener {
            val rawText = editValue.text.toString().trim()
            val value = rawText.toDoubleOrNull() ?: 0.0

            val mode = when (radioGroup.checkedRadioButtonId) {
                R.id.rb_set_constant -> BulkEditMode.SET_CONSTANT
                R.id.rb_percentage -> BulkEditMode.PERCENTAGE
                R.id.rb_smooth -> BulkEditMode.SMOOTH
                else -> BulkEditMode.ADD_PHYSICAL
            }

            onApply(mode, value)
            dismiss()
        }
    }
}