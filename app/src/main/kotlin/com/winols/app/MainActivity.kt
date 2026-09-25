package com.winols.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.winols.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Mock danych ECU: mapa wtrysku 16x16
        val ecuMatrix = Array(16) { r ->
            IntArray(16) { c -> (r * 150 + c * 50) % 2500 }
        }

        binding.ecuTableView.setMapData(ecuMatrix)

        // Obsługa selekcji komórki i synchronizacja z widokiem wykresu 2D
        binding.ecuTableView.onCellSelectedListener = { row, col, value ->
            binding.tvSelectedCell.text = "Komórka: [$row, $col] = $value"
            
            // Wizualizacja wybranego rzędu jako profilu krzywej 2D
            val rowFloats = FloatArray(ecuMatrix[row].size) { i -> ecuMatrix[row][i].toFloat() }
            binding.ecuChartView.setData(rowFloats, minVal = 0f, maxVal = 3000f)
        }

        binding.btnInc.setOnClickListener {
            binding.ecuTableView.updateSelectedCellValue(10)
        }

        binding.btnDec.setOnClickListener {
            binding.ecuTableView.updateSelectedCellValue(-10)
        }
    }
}