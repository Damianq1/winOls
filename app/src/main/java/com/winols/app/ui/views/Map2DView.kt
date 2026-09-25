package com.winols.app.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.winols.app.model.EcuMapData
import kotlin.math.max

/**
 * Widok 2D renderujący mapę ECU w postaci siatki (Heatmap) z dynamiczną paletą kolorów
 * odzwierciedlającą wartości binarne (gradient niebieski -> zielony -> czerwony).
 */
class Map2DView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var mapData: EcuMapData? = null
    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 24f
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }

    fun setMapData(data: EcuMapData) {
        this.mapData = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val map = mapData ?: return

        val rows = map.rows
        val cols = map.cols
        if (rows == 0 || cols == 0) return

        val cellWidth = width.toFloat() / cols
        val cellHeight = height.toFloat() / rows
        val min = map.minValue
        val max = max(map.maxValue, min + 0.0001f)

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val value = map.getValue(r, c)
                val normalized = ((value - min) / (max - min)).coerceIn(0f, 1f)

                cellPaint.color = calculateHeatmapColor(normalized)
                val left = c * cellWidth
                val top = r * cellHeight
                val right = left + cellWidth
                val bottom = top + cellHeight

                canvas.drawRect(left, top, right, bottom, cellPaint)
                canvas.drawRect(left, top, right, bottom, gridPaint)

                // Rysuj etykietę wartości, jeśli komórka jest wystarczająco duża
                if (cellWidth > 50f && cellHeight > 30f) {
                    val textY = top + (cellHeight / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
                    canvas.drawText(String.format("%.0f", value), left + (cellWidth / 2f), textY, textPaint)
                }
            }
        }
    }

    private fun calculateHeatmapColor(value: Float): Int {
        val hsv = FloatArray(3)
        // Zakres od 240 (niebieski - min) do 0 (czerwony - max)
        hsv[0] = (1f - value) * 240f
        hsv[1] = 0.85f
        hsv[2] = 0.95f
        return Color.HSVToColor(hsv)
    }
}