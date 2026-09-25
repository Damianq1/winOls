package com.winols.app.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.winols.app.model.EcuMap

class Map2DView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var ecuMap: EcuMap? = null

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private val path = Path()

    fun setMap(map: EcuMap) {
        this.ecuMap = map
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val map = ecuMap ?: return
        if (map.rows <= 0 || map.cols <= 0) return

        val padding = 40f
        val w = width - padding * 2
        val h = height - padding * 2
        if (w <= 0 || h <= 0) return

        // Ramka siatki
        canvas.drawRect(padding, padding, width - padding, height - padding, gridPaint)

        val valRange = (map.maxVal - map.minVal).let { if (it == 0f) 1f else it }
        val stepX = w / (map.cols - 1).coerceAtLeast(1)

        for (r in 0 until map.rows) {
            val hue = (r.toFloat() / map.rows.coerceAtLeast(1)) * 260f
            linePaint.color = Color.HSVToColor(floatArrayOf(hue, 0.85f, 0.95f))

            path.reset()
            for (c in 0 until map.cols) {
                val value = map.getValue(r, c)
                val normY = (value - map.minVal) / valRange
                val px = padding + c * stepX
                val py = (padding + h) - (normY * h)

                if (c == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            canvas.drawPath(path, linePaint)
        }
    }
}