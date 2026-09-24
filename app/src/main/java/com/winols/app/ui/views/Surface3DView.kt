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
import kotlin.math.min
import kotlin.math.sin

class Surface3DView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Dane mapy: rows (Y), cols (X), wartości (Z)
    private var matrix: Array<DoubleArray> = Array(0) { DoubleArray(0) }
    private var rows = 0
    private var cols = 0

    // Kąty obrotu (w radianach)
    private var angleX = Math.toRadians(35.0)
    private var angleZ = Math.toRadians(45.0)

    private var scale = 1.0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private val polygonPath = Path()
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = Color.argb(180, 0, 0, 0)
    }

    private val surfacePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scale *= detector.scaleFactor
            scale = scale.coerceIn(0.3f, 4.0f)
            invalidate()
            return true
        }
    })

    fun setMapData(data: Array<DoubleArray>) {
        this.matrix = data
        this.rows = data.size
        this.cols = if (rows > 0) data[0].size else 0
        invalidate()
    }

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

                    // Obrót wokół Z i pochylenie X
                    angleZ += dx * 0.008
                    angleX = (angleX + dy * 0.008).coerceIn(Math.toRadians(5.0), Math.toRadians(85.0))

                    invalidate()
                }
                lastTouchX = event.x
                lastTouchY = event.y
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (rows < 2 || cols < 2) return

        val centerX = width / 2f
        val centerY = height / 2f

        var minZ = Double.MAX_VALUE
        var maxZ = -Double.MAX_VALUE
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val v = matrix[r][c]
                if (v < minZ) minZ = v
                if (v > maxZ) maxZ = v
            }
        }
        val rangeZ = if (maxZ == minZ) 1.0 else maxZ - minZ

        val baseStep = min(width, height) / max(rows, cols).toFloat() * 0.65f * scale
        val heightMultiplier = 140f * scale

        // Prekalkulacja wierzchołków rzutowanych na przestrzeń 2D Canvasa
        val projectedX = Array(rows) { FloatArray(cols) }
        val projectedY = Array(rows) { FloatArray(cols) }

        val cosZ = cos(angleZ)
        val sinZ = sin(angleZ)
        val cosX = cos(angleX)
        val sinX = sin(angleX)

        val halfR = rows / 2f
        val halfC = cols / 2f

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val origX = (c - halfC) * baseStep
                val origY = (r - halfR) * baseStep
                val normalizedZ = ((matrix[r][c] - minZ) / rangeZ).toFloat()
                val origZ = normalizedZ * heightMultiplier

                // Rotacja Z
                val rotX = origX * cosZ - origY * sinZ
                val rotY = origX * sinZ + origY * cosZ

                // Rzut aksonometryczny z pochyleniem X
                projectedX[r][c] = (centerX + rotX).toFloat()
                projectedY[r][c] = (centerY + rotY * sinX - origZ * cosX).toFloat()
            }
        }

        // Renderowanie płatów powierzchni (quaid patches) w kolejności depth-sorting
        val rRange = if (sinZ > 0) (0 until rows - 1) else (rows - 2 downTo 0)
        val cRange = if (cosZ > 0) (0 until cols - 1) else (cols - 2 downTo 0)

        for (r in rRange) {
            for (c in cRange) {
                val avgVal = (matrix[r][c] + matrix[r + 1][c] + matrix[r][c + 1] + matrix[r + 1][c + 1]) / 4.0
                val ratio = ((avgVal - minZ) / rangeZ).toFloat().coerceIn(0f, 1f)

                surfacePaint.color = evaluateColor(ratio)

                polygonPath.reset()
                polygonPath.moveTo(projectedX[r][c], projectedY[r][c])
                polygonPath.lineTo(projectedX[r + 1][c], projectedY[r + 1][c])
                polygonPath.lineTo(projectedX[r + 1][c + 1], projectedY[r + 1][c + 1])
                polygonPath.lineTo(projectedX[r][c + 1], projectedY[r][c + 1])
                polygonPath.close()

                canvas.drawPath(polygonPath, surfacePaint)
                canvas.drawPath(polygonPath, edgePaint)
            }
        }
    }

    /**
     * Termiczna mapa kolorów WinOLS: Niebieski (nisko) -> Cyjan -> Żółty -> Czerwony (wysoko)
     */
    private fun evaluateColor(fraction: Float): Int {
        val hsv = floatArrayOf((1.0f - fraction) * 240f, 0.9f, 0.9f)
        return Color.HSVToColor(190, hsv)
    }
}