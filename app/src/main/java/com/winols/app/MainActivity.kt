package com.winols.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.winols.app.databinding.ActivityMainBinding
import com.winols.app.model.EcuMap

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val demoMap = generateSampleEcuMap()
        binding.mapTitle.text = "${demoMap.name} (${demoMap.rows}x${demoMap.cols})"

        binding.map2DView.setMap(demoMap)
        binding.map3DView.setMap(demoMap)
    }

    private fun generateSampleEcuMap(): EcuMap {
        val rows = 12
        val cols = 16
        val data = FloatArray(rows * cols)

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val factorR = r.toFloat() / (rows - 1)
                val factorC = c.toFloat() / (cols - 1)
                data[r * cols + c] = (kotlin.math.sin(factorC * Math.PI) * kotlin.math.cos(factorR * (Math.PI / 2)) * 1200f).toFloat() + 200f
            }
        }

        return EcuMap(
            id = "MAP_BOOST_TARGET",
            name = "Boost Pressure Target (mbar)",
            rows = rows,
            cols = cols,
            data = data
        )
    }
}