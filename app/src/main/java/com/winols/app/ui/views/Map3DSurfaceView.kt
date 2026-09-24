package com.winols.app.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

class Map3DSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var rows = 1
    private var cols = 1
    private var data: IntArray = IntArray(0)

    private var angleX = 35f
    private var angleZ = -45f
    private var scale = 1.0f

    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private val bgPaint = Paint().apply { color = 0xFF101010.toInt() }
    private val wirePaint = Paint().apply {
        strokeWidth = 2f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val polyPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scale *= detector.scaleFactor
            scale = scale.coerceIn(0.4f, 3.0f)
            invalidate()
            return true
        }
    })

    fun setMapData(numRows: Int, numCols: Int, values: IntArray) {
        this.rows = max(1, numRows)
        this.cols = max(1, numCols)
        this.data = values
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        if (scaleDetector.isInProgress) return true

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY
                angleZ += dx * 0.4f
                angleX = (angleX + dy * 0.4f).coerceIn(5f, 85f)
                lastTouchX = event.x
                lastTouchY = event.y
                invalidate()
            }
        }
        return true
    }

    private data class Point3D(val x: Float, val y: Float, val z: Float)
    private data class Point2D(val x: Float, val y: Float, val depth: Float)

    private fun project(p: Point3D, cx: Float, cy: Float): Point2D {
        val radZ = Math.toRadians(angleZ.toDouble())
        val radX = Math.toRadians(angleX.toDouble())

        // Rotate Z
        val x1 = p.x * cos(radZ) - p.y * sin(radZ)
        val y1 = p.x * sin(radZ) + p.y * cos(radZ)
        val z1 = p.z

        // Rotate X
        val y2 = y1 * cos(radX) - z1 * sin(radX)
        val z2 = y1 * sin(radX) + z1 * cos(radX)

        val px = cx + (x1 * scale).toFloat()
        val py = cy - (z2 * scale).toFloat()
        return Point2D(px, py, y2.toFloat())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        if (data.isEmpty() || rows < 2 || cols < 2) return

        var minVal = Int.MAX_VALUE
        var maxVal = Int.MIN_VALUE
        for (v in data) {
            if (v < minVal) minVal = v
            if (v > maxVal) maxVal = v
        }
        if (minVal == maxVal) maxVal += 1
        val valRange = (maxVal - minVal).toFloat()

        val cx = width / 2f
        val cy = height / 2f
        val extent = min(width, height) * 0.45f
        val stepX = (extent * 2f) / (cols - 1)
        val stepY = (extent * 2f) / (rows - 1)
        val heightScale = extent * 0.8f

        val pts = Array(rows) { r ->
            Array(cols) { c ->
                val v = data.getOrElse(r * cols + c) { 0 }
                val normZ = (v - minVal) / valRange
                val px = -extent + c * stepX
                val py = -extent + r * stepY
                val pz = normZ * heightScale
                project(Point3D(px, py, pz), cx, cy)
            }
        }

        val polyPath = Path()

        for (r in 0 until rows - 1) {
            for (c in 0 until cols - 1) {
                val p00 = pts[r][c]
                val p10 = pts[r + 1][c]
                val p11 = pts[r + 1][c + 1]
                val p01 = pts[r][c + 1]

                polyPath.reset()
                polyPath.moveTo(p00.x, p00.y)
                polyPath.lineTo(p10.x, p10.y)
                polyPath.lineTo(p11.x, p11.y)
                polyPath.lineTo(p01.x, p01.y)
                polyPath.close()

                val vAvg = (data[r * cols + c] + data[(r + 1) * cols + c] +
                        data[(r + 1) * cols + c + 1] + data[r * cols + c + 1]) / 4f
                val factor = ((vAvg - minVal) / valRange).coerceIn(0f, 1f)

                // Color gradient: Blue (low) -> Green -> Red (high)
                val red = (factor * 255).toInt()
                val blue = ((1f - factor) * 255).toInt()
                val green = ((1f - kotlin.math.abs(factor - 0.5f) * 2f) * 255).toInt()

                polyPaint.color = Color.rgb(red, green, blue)
                canvas.drawPath(polyPath, polyPaint)

                wirePaint.color = 0x55FFFFFF
                canvas.drawPath(polyPath, wirePaint)
            }
        }
    }
}