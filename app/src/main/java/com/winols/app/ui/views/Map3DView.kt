package com.winols.app.ui.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.winols.app.model.EcuMap
import kotlin.math.cos
import kotlin.math.sin

class Map3DView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var ecuMap: EcuMap? = null

    private var rotX = 35f
    private var rotZ = -45f
    private var scaleFactor = 1.0f

    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private val wirePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val linePath = Path()

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor = (scaleFactor * detector.scaleFactor).coerceIn(0.4f, 3.5f)
            invalidate()
            return true
        }
    })

    fun setMap(map: EcuMap) {
        this.ecuMap = map
        invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                if (!scaleDetector.isInProgress) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY

                    rotZ += dx * 0.4f
                    rotX = (rotX + dy * 0.4f).coerceIn(5f, 85f)

                    lastTouchX = event.x
                    lastTouchY = event.y
                    invalidate()
                }
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val map = ecuMap ?: return
        if (map.rows < 2 || map.cols < 2) return

        val cx = width / 2f
        val cy = height / 2f
        val baseSize = minOf(width, height) * 0.45f * scaleFactor
        val valRange = (map.maxVal - map.minVal).let { if (it == 0f) 1f else it }

        val radX = Math.toRadians(rotX.toDouble())
        val radZ = Math.toRadians(rotZ.toDouble())

        fun project(r: Int, c: Int): Pair<Float, Float> {
            val nx = (c.toFloat() / (map.cols - 1) - 0.5f) * baseSize
            val ny = (r.toFloat() / (map.rows - 1) - 0.5f) * baseSize
            val nz = ((map.getValue(r, c) - map.minVal) / valRange - 0.5f) * (baseSize * 0.6f)

            // Rotacja Z
            val x1 = (nx * cos(radZ) - ny * sin(radZ)).toFloat()
            val y1 = (nx * sin(radZ) + ny * cos(radZ)).toFloat()

            // Rotacja X
            val y2 = (y1 * cos(radX) - nz * sin(radX)).toFloat()

            return Pair(cx + x1, cy + y2)
        }

        // Linie wzdłuż kolumn (X)
        for (r in 0 until map.rows) {
            linePath.reset()
            for (c in 0 until map.cols) {
                val (px, py) = project(r, c)
                if (c == 0) linePath.moveTo(px, py) else linePath.lineTo(px, py)
            }
            val normHeight = (map.getValue(r, map.cols / 2) - map.minVal) / valRange
            val hue = (1f - normHeight) * 240f
            wirePaint.color = Color.HSVToColor(floatArrayOf(hue, 0.9f, 1f))
            canvas.drawPath(linePath, wirePaint)
        }

        // Linie wzdłuż wierszy (Y)
        for (c in 0 until map.cols) {
            linePath.reset()
            for (r in 0 until map.rows) {
                val (px, py) = project(r, c)
                if (r == 0) linePath.moveTo(px, py) else linePath.lineTo(px, py)
            }
            wirePaint.color = Color.argb(120, 200, 200, 200)
            canvas.drawPath(linePath, wirePaint)
        }
    }
}