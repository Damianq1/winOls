package com.winols.app.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.winols.app.model.MapDefinition

class MapGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var mapDefinition: MapDefinition? = null
    private var gridData: Array<DoubleArray> = Array(0) { DoubleArray(0) }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    private val cellBorderPaint = Paint().apply {
        color = Color.DKGRAY
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    fun setMap(def: MapDefinition, data: Array<DoubleArray>) {
        this.mapDefinition = def
        this.gridData = data
        requestLayout()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val def = mapDefinition ?: return
        if (gridData.isEmpty() || gridData[0].isEmpty()) return

        val cellWidth = width.toFloat() / def.columns
        val cellHeight = height.toFloat() / def.rows

        for (r in 0 until def.rows) {
            for (c in 0 until def.columns) {
                val left = c * cellWidth
                val top = r * cellHeight
                val right = left + cellWidth
                val bottom = top + cellHeight

                canvas.drawRect(left, top, right, bottom, cellBorderPaint)

                val value = gridData.getOrNull(r)?.getOrNull(c) ?: 0.0
                val text = String.format("%.1f", value)
                val textY = top + (cellHeight / 2) - ((textPaint.descent() + textPaint.ascent()) / 2)
                canvas.drawText(text, left + (cellWidth / 2), textY, textPaint)
            }
        }
    }
}
