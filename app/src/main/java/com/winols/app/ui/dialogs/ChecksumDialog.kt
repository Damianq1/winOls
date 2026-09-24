package com.winols.app.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.winols.app.R
import com.winols.app.databinding.DialogChecksumBinding
import com.winols.app.engine.ChecksumVerificationResult

class ChecksumDialog(
    private val context: Context,
    private val results: List<ChecksumVerificationResult>,
    private val onApplyAndSave: () -> Unit
) {

    fun show(): Dialog {
        val binding = DialogChecksumBinding.inflate(LayoutInflater.from(context))
        val builder = AlertDialog.Builder(context)
        builder.setView(binding.root)

        val hasErrors = results.any { !it.isValid }

        // Renderowanie podsumowania tekstowego
        val statusText = buildString {
            if (results.isEmpty()) {
                append("Brak zdefiniowanych bloków sum kontrolnych dla wybranego sterownika.")
            } else {
                results.forEach { res ->
                    val status = if (res.isValid) "[OK]" else "[NIEPOPRAWNA]"
                    val storedHex = "0x" + res.storedValue.toString(16).uppercase()
                    val calcHex = "0x" + res.calculatedValue.toString(16).uppercase()

                    append("$status ${res.block.name} (${res.block.algorithm})\n")
                    append("  Zakres: 0x${res.block.startAddress.toString(16).uppercase()} - 0x${res.block.endAddress.toString(16).uppercase()}\n")
                    append("  Zapisana: $storedHex | Obliczona: $calcHex\n\n")
                }
            }
        }

        binding.root.findViewById<TextView>(R.id.tvChecksumDetails)?.text = statusText

        builder.setTitle(if (hasErrors) "Wykryto niezgodność sum kontrolnych" else "Sumy kontrolne poprawne")
        builder.setIcon(if (hasErrors) android.R.drawable.ic_dialog_alert else android.R.drawable.ic_dialog_info)

        builder.setPositiveButton(if (hasErrors) "Przelicz i Zapisz" else "Kontynuuj zapis") { dialog, _ ->
            onApplyAndSave()
            dialog.dismiss()
        }

        builder.setNegativeButton("Anuluj") { dialog, _ ->
            dialog.dismiss()
        }

        return builder.show()
    }
}