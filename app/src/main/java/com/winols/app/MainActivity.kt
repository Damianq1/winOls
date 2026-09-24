package com.winols.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.winols.app.edit.MapEditor
import com.winols.app.engine.ChecksumFamily
import com.winols.app.model.MapDefinition
import com.winols.app.ui.EcuGridView
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private val binModel = BinModel()
    private lateinit var mapEditor: MapEditor
    private lateinit var ecuGridView: EcuGridView
    private var currentSelectedMap: MapDefinition? = null

    companion object {
        private const val REQUEST_CODE_OPEN_BIN = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapEditor = MapEditor(binModel)
        ecuGridView = findViewById(R.id.ecuGridView)

        setupUI()
    }

    private fun setupUI() {
        findViewById<android.view.View>(R.id.btnLoadBin)?.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            startActivityForResult(intent, REQUEST_CODE_OPEN_BIN)
        }

        findViewById<android.view.View>(R.id.btnSelectMap)?.setOnClickListener {
            if (binModel.maps.isEmpty()) {
                Toast.makeText(this, "Brak zidentyfikowanych map", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val mapNames = binModel.maps.map { it.name }.toTypedArray()
            AlertDialog.Builder(this)
                .setTitle("Wybierz mapę")
                .setItems(mapNames) { _, which ->
                    val chosen = binModel.maps[which]
                    currentSelectedMap = chosen
                    ecuGridView.bind(binModel, chosen)
                }
                .show()
        }

        findViewById<android.view.View>(R.id.btnModifyCell)?.setOnClickListener {
            val map = currentSelectedMap ?: return@setOnClickListener
            val cell = ecuGridView.selectedCell ?: return@setOnClickListener

            val input = android.widget.EditText(this)
            input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            input.setText(binModel.getCellValue(map, cell.first, cell.second).toString())

            AlertDialog.Builder(this)
                .setTitle("Edycja komórki [${cell.first}, ${cell.second}]")
                .setView(input)
                .setPositiveButton("Zapisz") { _, _ ->
                    val newVal = input.text.toString().toDoubleOrNull()
                    if (newVal != null) {
                        mapEditor.modifySingleCell(map, cell.first, cell.second, newVal)
                        ecuGridView.invalidate()
                    }
                }
                .setNegativeButton("Anuluj", null)
                .show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OPEN_BIN && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri -> loadBinaryUri(uri) }
        }
    }

    private fun loadBinaryUri(uri: Uri) {
        contentResolver.openInputStream(uri)?.use { stream ->
            val bytes = stream.readBytes()
            binModel.loadBytes(bytes)
            Toast.makeText(
                this,
                "Załadowano ${bytes.size} bajtów. Wykryte mapy: ${binModel.maps.size}",
                Toast.LENGTH_LONG
            ).show()

            if (binModel.maps.isNotEmpty()) {
                currentSelectedMap = binModel.maps[0]
                ecuGridView.bind(binModel, binModel.maps[0])
            }
        }
    }
}