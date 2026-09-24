package com.winols.app.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

/**
 * Zoptymalizowany Custom View do aksonometrycznego renderowania siatki 3D mapy ECU.
 * Umożliwia płynny obrót i skalowanie gestami bez używania ciężkich bibliotek OpenGL.
 */
class MapSurface3DView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var matrixData: Array<DoubleArray>? = null
    private var minVal = 0.0
    private var maxVal = 1.0

    private var rotationAngle = 45.0
    private var elevationAngle = 30.0
    private var scaleFactor = 1.0f

    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E676")
        strokeWidth = 2.5f
        style = Paint.Style.STROKE
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    fun setMatrix(matrix: Array<DoubleArray>) {
        this.matrixData = matrix
        calculateMinMax()
        invalidate()
    }

    private fun calculateMinMax() {
        val m = matrixData ?: return
        var min = Double.MAX_VALUE
        var max = Double.MIN_VALUE
        for (r in m.indices) {
            for (c in m[r].indices) {
                val v = m[r][c]
                if (v < min) min = v
                if (v > max) max = v
            }
        }
        minVal = min
        maxVal = if (max == min) min + 1.0 else max
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY
                rotationAngle = (rotationAngle + dx * 0.5) % 360
                elevationAngle = (elevationAngle - dy * 0.5).coerceIn(10.0, 80.0)
                lastTouchX = event.x
                lastTouchY = event.y
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val m = matrixData ?: return
        val rows = m.size
        if (rows < 2) return
        val cols = m[0].size
        if (cols < 2) return

        val centerX = width / 2f
        val centerY = height / 2f
        val cellWidth = (width * 0.5f) / cols
        val cellHeight = (height * 0.5f) / rows
        val heightScale = height * 0.35f

        val radRot = Math.toRadians(rotationAngle)
        val radElev = Math.toRadians(elevationAngle)

        fun project(r: Int, c: Int): Pair<Float, Float> {
            val normH = ((m[r][c] - minVal) / (maxVal - minVal)).toFloat()
            val x = (c - cols / 2f) * cellWidth
            val y = (r - rows / 2f) * cellHeight
            val z = normH * heightScale

            val rotX = (x * cos(radRot) - y * sin(radRot)).toFloat()
            val rotY = (x * sin(radRot) + y * cos(radRot)).toFloat()

            val projX = centerX + rotX
            val projY = centerY + (rotY * sin(radElev) - z * cos(radElev)).toFloat()
            return Pair(projX, projY)
        }

        val polyPath = Path()

        // Rysowanie powierzchni siatkowej (wireframe mesh)
        for (r in 0 until rows - 1) {
            for (c in 0 until cols - 1) {
                val p1 = project(r, c)
                val p2 = project(r, c + 1)
                val p3 = project(r + 1, c + 1)
                val p4 = project(r + 1, c)

                polyPath.reset()
                polyPath.moveTo(p1.first, p1.second)
                polyPath.lineTo(p2.first, p2.second)
                polyPath.lineTo(p3.first, p3.second)
                polyPath.lineTo(p4.first, p4.second)
                polyPath.close()

                val normColor = ((m[r][c] - minVal) / (maxVal - minVal)).toFloat()
                fillPaint.color = interpolateColor(normColor)
                canvas.drawPath(polyPath, fillPaint)
                canvas.drawPath(polyPath, linePaint)
            }
        }
    }

    private fun interpolateColor(value: Float): Int {
        val clamped = value.coerceIn(0f, 1f)
        val red = (clamped * 255).toInt()
        val blue = ((1f - clamped) * 255).toInt()
        return Color.argb(180, red, 40, blue)
    }
}