package com.winols.app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.winols.app.data.BinaryBufferManager
import com.winols.app.edit.MapEditor
import com.winols.app.engine.ChecksumEngine
import com.winols.app.engine.MapFinderEngine
import com.winols.app.model.BitDepth
import com.winols.app.model.DataRepresentation
import com.winols.app.model.MapDefinition
import com.winols.app.model.ValueType
import com.winols.app.ui.views.HexGridView
import com.winols.app.ui.views.Surface3DView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteOrder

class MainActivity : AppCompatActivity() {

    private val bufferManager = BinaryBufferManager()
    private val mapEditor = MapEditor(bufferManager)
    private val mapFinder = MapFinderEngine(bufferManager)
    private val checksumEngine = ChecksumEngine(bufferManager)

    private lateinit var hexView: HexGridView
    private lateinit var surface3DView: Surface3DView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        hexView = findViewById(R.id.hexGridView)
        surface3DView = findViewById(R.id.surface3DView)

        hexView.setBufferManager(bufferManager)

        initSession()
    }

    private fun initSession() {
        lifecycleScope.launch {
            val syntheticData = ByteArray(64 * 1024)
            for (i in syntheticData.indices) {
                syntheticData[i] = (i % 256).toByte()
            }
            bufferManager.loadBytes(syntheticData)
            hexView.invalidate()

            val detectedMaps = withContext(Dispatchers.Default) {
                mapFinder.scanPotentialMaps()
            }

            if (detectedMaps.isNotEmpty()) {
                val firstMap = detectedMaps[0]
                surface3DView.setMap(bufferManager, firstMap)
                
                mapEditor.applyOperation(
                    mapDef = firstMap,
                    type = MapEditor.OperationType.MULTIPLY_PERCENT,
                    value = 10.0
                )
                hexView.invalidate()
                surface3DView.invalidate()
            }
        }
    }

    fun executeChecksumValidation(start: Int, end: Int, target: Int) {
        lifecycleScope.launch {
            val result = checksumEngine.verifyAndPatch(
                startAddress = start,
                endAddress = end,
                checksumAddress = target,
                algorithm = ChecksumEngine.Algorithm.CRC32,
                patch = true
            )
            Toast.makeText(
                this@MainActivity,
                "Checksum Valid: ${result.isValid} -> Patched: 0x${java.lang.Long.toHexString(result.calculated)}",
                Toast.LENGTH_LONG
            ).show()
            hexView.invalidate()
        }
    }
}