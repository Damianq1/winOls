package com.winols.app

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.winols.app.databinding.ActivityMainBinding
import com.winols.app.model.EcuMapData
import kotlin.math.sin

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sampleMap = generateSampleEcuMap()

        binding.map2DView.setMapData(sampleMap)
        binding.map3DView.setMapData(sampleMap)

        binding.viewModeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnMode2D -> {
                        binding.map2DView.visibility = View.VISIBLE
                        binding.map3DView.visibility = View.GONE
                    }
                    R.id.btnMode3D -> {
                        binding.map2DView.visibility = View.GONE
                        binding.map3DView.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun generateSampleEcuMap(): EcuMapData {
        val rows = 16
        val cols = 16
        val data = FloatArray(rows * cols)

        // Generowanie przykładowej charakterystyki turbodoładowania / zapłonu
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val x = c / (cols - 1.0) * Math.PI
                val y = r / (rows - 1.0) * Math.PI
                data[r * cols + c] = ((sin(x) * sin(y)) * 250.0 + 50.0).toFloat()
            }
        }

        return EcuMapData(
            name = "Turbo Boost Target (16x16)",
            rows = rows,
            cols = cols,
            data = data
        )
    }
}