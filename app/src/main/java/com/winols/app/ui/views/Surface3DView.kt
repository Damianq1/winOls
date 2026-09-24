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

class Surface3DView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var data: Array<DoubleArray> = emptyArray()
    private var rows = 0
    private var cols = 0

    private var angleX = 35.0
    private var angleY = -45.0
    private var prevX = 0f
    private var prevY = 0f

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.CYAN
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private val polyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    fun setMapData(matrix: Array<DoubleArray>) {
        this.data = matrix
        this.rows = matrix.size
        this.cols = if (matrix.isNotEmpty()) matrix[0].size else 0
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                prevX = event.x
                prevY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - prevX
                val dy = event.y - prevY
                angleY += dx * 0.5
                angleX -= dy * 0.5
                prevX = event.x
                prevY = event.y
                invalidate()
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (rows < 2 || cols < 2) return

        val centerX = width / 2f
        val centerY = height / 2f
        val scale = minOf(width, height) / 3f

        var minVal = Double.MAX_VALUE
        var maxVal = Double.MIN_VALUE
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val v = data[r][c]
                if (v < minVal) minVal = v
                if (v > maxVal) maxVal = v
            }
        }
        val range = if (maxVal == minVal) 1.0 else (maxVal - minVal)

        val radX = Math.toRadians(angleX)
        val radY = Math.toRadians(angleY)

        fun project(r: Int, c: Int): Pair<Float, Float> {
            val normX = (c.toFloat() / (cols - 1)) * 2f - 1f
            val normZ = (r.toFloat() / (rows - 1)) * 2f - 1f
            val normY = (((data[r][c] - minVal) / range).toFloat() * 2f - 1f) * 0.8f

            val rotY_x = normX * cos(radY) + normZ * sin(radY)
            val rotY_z = -normX * sin(radY) + normZ * cos(radY)

            val rotX_y = normY * cos(radX) - rotY_z * sin(radX)

            val screenX = centerX + (rotY_x * scale).toFloat()
            val screenY = centerY - (rotX_y * scale).toFloat()
            return Pair(screenX, screenY)
        }

        val path = Path()
        for (r in 0 until rows - 1) {
            for (c in 0 until cols - 1) {
                val p1 = project(r, c)
                val p2 = project(r, c + 1)
                val p3 = project(r + 1, c + 1)
                val p4 = project(r + 1, c)

                path.reset()
                path.moveTo(p1.first, p1.second)
                path.lineTo(p2.first, p2.second)
                path.lineTo(p3.first, p3.second)
                path.lineTo(p4.first, p4.second)
                path.close()

                val avgVal = (data[r][c] + data[r][c + 1] + data[r + 1][c + 1] + data[r + 1][c]) / 4.0
                val normAvg = ((avgVal - minVal) / range).toFloat().coerceIn(0f, 1f)
                polyPaint.color = Color.argb(160, (normAvg * 255).toInt(), 50, ((1f - normAvg) * 255).toInt())

                canvas.drawPath(path, polyPaint)
                canvas.drawPath(path, linePaint)
            }
        }
    }
}
