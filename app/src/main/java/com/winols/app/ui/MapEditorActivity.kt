package com.winols.app.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import com.winols.app.databinding.ActivityMapEditorBinding

class MapEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapEditorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTabs()
        setupListeners()
        loadMapDataMock()
    }

    private fun setupTabs() {
        binding.viewModeTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> switchView(table = true, curve2D = false, surface3D = false)
                    1 -> switchView(table = false, curve2D = true, surface3D = false)
                    2 -> switchView(table = false, curve2D = false, surface3D = true)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun switchView(table: Boolean, curve2D: Boolean, surface3D: Boolean) {
        binding.mapTableView.visibility = if (table) View.VISIBLE else View.GONE
        binding.map2DView.visibility = if (curve2D) View.VISIBLE else View.GONE
        binding.map3DSurfaceView.visibility = if (surface3D) View.VISIBLE else View.GONE
    }

    private fun setupListeners() {
        binding.mapTableView.onCellSelected = { row, col, value ->
            binding.tvCursorValue.text = "Val: %d (0x%04X)".format(value, value)
            binding.tvStatusInfo.text = "Row: %d, Col: %d | Idx: 0x%04X".format(row, col, row * 16 + col)
        }
    }

    private fun loadMapDataMock() {
        val rows = 16
        val cols = 16
        val buffer = IntArray(rows * cols) { i ->
            val r = i / cols
            val c = i % cols
            // Syntetyczna powierzchnia krzywej momentu/doładowania
            ((r * r * 1.5) + (c * 12) + (r * c * 2.2)).toInt() and 0xFFFF
        }

        binding.mapTableView.setMapData(rows, cols, buffer)
        binding.map2DView.setMapData(rows, cols, buffer)
        binding.map3DSurfaceView.setMapData(rows, cols, buffer)
    }
}