package com.winols.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.winols.app.BinModel
import com.winols.app.model.MapDefinition
import kotlin.math.cos
import kotlin.math.sin

/**
 * Własny widok (Custom View) renderujący trójwymiarową powierzchnię (3D Surface Mesh)
 * mapy ECU z rotacją kątową wokół osi X i Z, dynamiczną siatką izometryczną oraz
 * gradientowym cieniowaniem wysokości (heatmap).
 */
class EcuSurface3DView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var binModel: BinModel? = null
    private var activeMap: MapDefinition? = null

    // Parametry kamery i rzutowania izometrycznego
    private var rotX = 35.0f // Kąt elewacji (stopnie)
    private var rotZ = -45.0f // Kąt obrotu wokół osi pionowej (stopnie)
    private var zoomScale = 1.0f

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.0f
        color = Color.parseColor("#4FC3F7")
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.0f
        color = Color.parseColor("#FFD54F")
    }

    private val polyPath = Path()

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            zoomScale *= detector.scaleFactor
            zoomScale = zoomScale.coerceIn(0.4f, 4.0f)
            invalidate()
            return true
        }
    })

    fun bind(model: BinModel, map: MapDefinition) {
        this.binModel = model
        this.activeMap = map
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        if (scaleDetector.isInProgress) return true

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                isDragging = true
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY

                    rotZ += dx * 0.4f
                    rotX = (rotX - dy * 0.4f).coerceIn(10.0f, 85.0f)

                    lastTouchX = event.x
                    lastTouchY = event.y
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val model = binModel ?: return
        val map = activeMap ?: return
        if (map.rows < 2 || map.columns < 2) return

        canvas.drawColor(Color.parseColor("#121212"))

        val centerX = width / 2.0f
        val centerY = height / 2.0f

        // Obliczenie min i max wartości inżynierskiej do normalizacji wysokości i kolorów
        var minVal = Double.MAX_VALUE
        var maxVal = -Double.MAX_VALUE

        val values = Array(map.rows) { r ->
            DoubleArray(map.columns) { c ->
                val v = model.getCellValue(map, r, c)
                if (v < minVal) minVal = v
                if (v > maxVal) maxVal = v
                v
            }
        }

        val valRange = if (maxVal > minVal) (maxVal - minVal) else 1.0
        val baseGridSize = (width.coerceAtMost(height) * 0.55f) * zoomScale
        val stepX = baseGridSize / (map.columns - 1)
        val stepY = baseGridSize / (map.rows - 1)
        val heightScale = (baseGridSize * 0.45f).toFloat()

        // Tablica rzutowanych punktów na ekran 2D
        val projectedX = Array(map.rows) { FloatArray(map.columns) }
        val projectedY = Array(map.rows) { FloatArray(map.columns) }

        val radX = Math.toRadians(rotX.toDouble())
        val radZ = Math.toRadians(rotZ.toDouble())
        val cosZ = cos(radZ).toFloat()
        val sinZ = sin(radZ).toFloat()
        val cosX = cos(radX).toFloat()
        val sinX = sin(radX).toFloat()

        for (r in 0 until map.rows) {
            val localY = (r - (map.rows - 1) / 2.0f) * stepY
            for (c in 0 until map.columns) {
                val localX = (c - (map.columns - 1) / 2.0f) * stepX
                val normalizedZ = ((values[r][c] - minVal) / valRange).toFloat()
                val localZ = normalizedZ * heightScale

                // Obrót wokół osi Z
                val xRot = localX * cosZ - localY * sinZ
                val yRot = localX * sinZ + localY * cosZ

                // Rzutowanie perspektywiczno-izometryczne (pochylenie X)
                val screenX = centerX + xRot
                val screenY = centerY + (yRot * sinX) - (localZ * cosX)

                projectedX[r][c] = screenX
                projectedY[r][c] = screenY
            }
        }

        // Rysowanie powierzchni jako siatki czworokątów (poligonów) z cieniowaniem
        for (r in 0 until map.rows - 1) {
            for (c in 0 until map.columns - 1) {
                polyPath.reset()
                polyPath.moveTo(projectedX[r][c], projectedY[r][c])
                polyPath.lineTo(projectedX[r][c + 1], projectedY[r][c + 1])
                polyPath.lineTo(projectedX[r + 1][c + 1], projectedY[r + 1][c + 1])
                polyPath.lineTo(projectedX[r + 1][c], projectedY[r + 1][c])
                polyPath.close()

                val avgNorm = ((values[r][c] + values[r][c + 1] + values[r + 1][c + 1] + values[r + 1][c]) / 4.0 - minVal) / valRange
                fillPaint.color = calculateHeatmapColor(avgNorm.coerceIn(0.0, 1.0))
                canvas.drawPath(polyPath, fillPaint)

                // Krawędzie siatki
                canvas.drawPath(polyPath, linePaint)
            }
        }
    }

    /**
     * Zwraca kolor gradientu (od niebieskiego przez cyjan, zieleń, żółć do czerwieni).
     */
    private fun calculateHeatmapColor(t: Double): Int {
        val r: Int
        val g: Int
        val b: Int

        when {
            t < 0.25 -> {
                val s = (t / 0.25).toFloat()
                r = 0
                g = (s * 255).toInt()
                b = 255
            }
            t < 0.5 -> {
                val s = ((t - 0.25) / 0.25).toFloat()
                r = 0
                g = 255
                b = ((1f - s) * 255).toInt()
            }
            t < 0.75 -> {
                val s = ((t - 0.5) / 0.25).toFloat()
                r = (s * 255).toInt()
                g = 255
                b = 0
            }
            else -> {
                val s = ((t - 0.75) / 0.25).toFloat()
                r = 255
                g = ((1f - s) * 255).toInt()
                b = 0
            }
        }
        return Color.argb(200, r, g, b)
    }
}