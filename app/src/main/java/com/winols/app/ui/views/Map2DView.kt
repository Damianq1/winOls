package com.winols.app.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.math.min

class Map2DView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var rows = 1
    private var cols = 1
    private var data: IntArray = IntArray(0)

    private val bgPaint = Paint().apply { color = 0xFF151515.toInt() }
    private val axisPaint = Paint().apply {
        color = 0xFF555555.toInt()
        strokeWidth = 2f
    }
    private val gridPaint = Paint().apply {
        color = 0xFF262626.toInt()
        strokeWidth = 1f
    }
    private val linePaints = listOf(
        0xFF00E5FF.toInt(),
        0xFFFF3D00.toInt(),
        0xFF00E676.toInt(),
        0xFFFFEA00.toInt(),
        0xFFD500F9.toInt()
    ).map { c ->
        Paint().apply {
            color = c
            strokeWidth = 3f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
    }

    fun setMapData(numRows: Int, numCols: Int, values: IntArray) {
        this.rows = max(1, numRows)
        this.cols = max(1, numCols)
        this.data = values
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        if (data.isEmpty() || cols <= 1) return

        val paddingLeft = 60f
        val paddingBottom = 60f
        val paddingTop = 40f
        val paddingRight = 40f

        val plotW = width - paddingLeft - paddingRight
        val plotH = height - paddingTop - paddingBottom

        // Grid lines
        for (i in 0..4) {
            val y = paddingTop + plotH * (i / 4f)
            canvas.drawLine(paddingLeft, y, width - paddingRight, y, gridPaint)
        }

        // Axes
        canvas.drawLine(paddingLeft, paddingTop, paddingLeft, height - paddingBottom, axisPaint)
        canvas.drawLine(paddingLeft, height - paddingBottom, width - paddingRight, height - paddingBottom, axisPaint)

        var minVal = Int.MAX_VALUE
        var maxVal = Int.MIN_VALUE
        for (v in data) {
            if (v < minVal) minVal = v
            if (v > maxVal) maxVal = v
        }
        if (minVal == maxVal) maxVal += 1
        val valRange = (maxVal - minVal).toFloat()

        val stepX = plotW / (cols - 1)
        val path = Path()

        for (r in 0 until rows) {
            path.reset()
            val paint = linePaints[r % linePaints.size]

            for (c in 0 until cols) {
                val idx = r * cols + c
                val value = data.getOrElse(idx) { 0 }
                val normY = (value - minVal) / valRange
                val x = paddingLeft + c * stepX
                val y = (height - paddingBottom) - (normY * plotH)

                if (c == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            canvas.drawPath(path, paint)
        }
    }
}