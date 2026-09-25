package com.winols.app.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot

/**
 * Widok wektorowy 2D renderujący krzywe kalibracyjne z możliwością
 * przeciągania węzłów wartości bezpośrednio na wykresie.
 */
class Ecu2dChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var points: FloatArray = floatArrayOf()
    private var draggedIndex: Int = -1
    private var maxValue: Float = 1000f
    private var minValue: Float = 0f

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E676")
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD600")
        style = Paint.Style.FILL
    }

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val path = Path()

    fun setData(newPoints: FloatArray, minVal: Float = 0f, maxVal: Float = 1000f) {
        this.points = newPoints
        this.minValue = minVal
        this.maxValue = maxVal
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (points.isEmpty()) return false

        val paddingLeft = 50f
        val paddingRight = 50f
        val paddingTop = 50f
        val paddingBottom = 50f

        val stepX = (width - paddingLeft - paddingRight) / (points.size - 1)
        val chartHeight = height - paddingTop - paddingBottom

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                draggedIndex = -1
                for (i in points.indices) {
                    val cx = paddingLeft + i * stepX
                    val normalizedY = (points[i] - minValue) / (maxValue - minValue)
                    val cy = height - paddingBottom - (normalizedY * chartHeight)

                    if (hypot(event.x - cx, event.y - cy) < 40f) {
                        draggedIndex = i
                        parent.requestDisallowInterceptTouchEvent(true)
                        return true
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (draggedIndex != -1) {
                    val clampedY = event.y.coerceIn(paddingTop, height - paddingBottom)
                    val ratio = 1f - ((clampedY - paddingTop) / chartHeight)
                    points[draggedIndex] = minValue + ratio * (maxValue - minValue)
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                draggedIndex = -1
                parent.requestDisallowInterceptTouchEvent(false)
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.isEmpty()) return

        val paddingLeft = 50f
        val paddingRight = 50f
        val paddingTop = 50f
        val paddingBottom = 50f

        val usableWidth = width - paddingLeft - paddingRight
        val usableHeight = height - paddingTop - paddingBottom
        val stepX = usableWidth / (points.size - 1)

        // Rysowanie osi
        canvas.drawLine(paddingLeft, height - paddingBottom, width - paddingRight, height - paddingBottom, axisPaint)
        canvas.drawLine(paddingLeft, paddingTop, paddingLeft, height - paddingBottom, axisPaint)

        path.reset()
        for (i in points.indices) {
            val cx = paddingLeft + i * stepX
            val normalizedY = (points[i] - minValue) / (maxValue - minValue)
            val cy = height - paddingBottom - (normalizedY * usableHeight)

            if (i == 0) path.moveTo(cx, cy) else path.lineTo(cx, cy)
        }
        canvas.drawPath(path, linePaint)

        // Rysowanie węzłów
        for (i in points.indices) {
            val cx = paddingLeft + i * stepX
            val normalizedY = (points[i] - minValue) / (maxValue - minValue)
            val cy = height - paddingBottom - (normalizedY * usableHeight)

            val radius = if (i == draggedIndex) 16f else 10f
            canvas.drawCircle(cx, cy, radius, pointPaint)
        }
    }
}