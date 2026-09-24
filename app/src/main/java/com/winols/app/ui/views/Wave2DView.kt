package com.winols.app.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.max
import kotlin.math.min

class Wave2DView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var dataSeries: DoubleArray = DoubleArray(0)
    private var selectedIndex: Int = -1

    // Skalowanie i przesunięcie (Pan & Zoom)
    private var scaleFactor = 1.0f
    private var offsetX = 0.0f

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E676") // Styl WinOLS Green
        strokeWidth = 3.5f
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD600")
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
    }

    private val path = Path()

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = scaleFactor.coerceIn(0.5f, 10.0f)
            invalidate()
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            offsetX -= distanceX
            invalidate()
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            findNearestPoint(e.x)
            return true
        }
    })

    fun setData(data: DoubleArray) {
        this.dataSeries = data
        this.selectedIndex = -1
        this.offsetX = 0f
        this.scaleFactor = 1.0f
        invalidate()
    }

    private fun findNearestPoint(touchX: Float) {
        if (dataSeries.isEmpty() || width == 0) return
        val stepX = (width.toFloat() / max(1, dataSeries.size - 1)) * scaleFactor
        val effectiveX = touchX - offsetX
        val index = (effectiveX / stepX).toInt().coerceIn(0, dataSeries.size - 1)
        selectedIndex = index
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var handled = scaleDetector.onTouchEvent(event)
        handled = gestureDetector.onTouchEvent(event) || handled
        return handled || super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dataSeries.isEmpty()) return

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val padding = 50f

        var minVal = dataSeries.minOrNull() ?: 0.0
        var maxVal = dataSeries.maxOrNull() ?: 1.0
        if (minVal == maxVal) {
            maxVal += 1.0
            minVal -= 1.0
        }

        val range = maxVal - minVal
        val stepX = (viewWidth / max(1, dataSeries.size - 1)) * scaleFactor
        val drawableHeight = viewHeight - (2 * padding)

        // Rysowanie poziomych linii siatki
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = padding + (drawableHeight / gridLines) * i
            canvas.drawLine(0f, y, viewWidth, y, gridPaint)
            val labelVal = maxVal - (range / gridLines) * i
            canvas.drawText(String.format("%.1f", labelVal), 10f, y - 5f, textPaint)
        }

        path.reset()
        for (i in dataSeries.indices) {
            val x = (i * stepX) + offsetX
            val normalizedY = (dataSeries[i] - minVal) / range
            val y = viewHeight - padding - (normalizedY * drawableHeight).toFloat()

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }

            // Podgląd selekcji punktu
            if (i == selectedIndex) {
                canvas.drawCircle(x, y, 12f, pointPaint)
                canvas.drawText("[$i]: ${dataSeries[i]}", x - 30f, y - 20f, textPaint)
            }
        }
        canvas.drawPath(path, linePaint)
    }
}