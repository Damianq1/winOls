package com.winols.app.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.max
import kotlin.math.min

/**
 * Dedykowany widok wykresu 2D dla map ECU (odpowiednik widoku 2D w WinOLS).
 * Prezentuje serie danych jako krzywe łamane, obsługuje podświetlanie aktywnej
 * linii/punktu, siatkę pomocniczą oraz płynny zoom i pan (przesuwanie).
 */
class MapGraph2DView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Dane wejściowe: macierz [wiersze][kolumny] lub pojedyncza seria [kolumny]
    private var seriesData: Array<DoubleArray>? = null
    private var xAxisLabels: DoubleArray? = null

    private var globalMin = 0.0
    private var globalMax = 1.0

    // Zoom i przesunięcie (Pan & Zoom)
    private var scaleXFactor = 1.0f
    private var translateX = 0.0f
    private val contentBounds = RectF()

    // Oznaczenie wybranego punktu (do bezpośredniej interakcji/edycji)
    private var selectedRow = -1
    private var selectedCol = -1

    // Style pędzli
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        strokeWidth = 1.0f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
    }

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        strokeWidth = 2.0f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        textSize = 28f
    }

    private val activeLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E676") // Styl WinOLS: jaskrawy zielony
        strokeWidth = 3.5f
        style = Paint.Style.STROKE
    }

    private val secondaryLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#448AFF")
        strokeWidth = 2.0f
        style = Paint.Style.STROKE
    }

    private val highlightPointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.YELLOW
        style = Paint.Style.FILL
    }

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleXFactor = (scaleXFactor * detector.scaleFactor).coerceIn(0.5f, 10.0f)
            invalidate()
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            translateX -= distanceX
            invalidate()
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            findClosestPoint(e.x, e.y)
            return true
        }
    })

    fun setData(matrix: Array<DoubleArray>, xLabels: DoubleArray? = null) {
        this.seriesData = matrix
        this.xAxisLabels = xLabels
        recalculateBounds()
        invalidate()
    }

    fun setSingleSeries(data: DoubleArray) {
        this.seriesData = arrayOf(data)
        this.xAxisLabels = null
        recalculateBounds()
        invalidate()
    }

    fun selectCell(row: Int, col: Int) {
        this.selectedRow = row
        this.selectedCol = col
        invalidate()
    }

    private fun recalculateBounds() {
        val data = seriesData ?: return
        var minVal = Double.MAX_VALUE
        var maxVal = -Double.MAX_VALUE

        for (row in data) {
            for (v in row) {
                if (v < minVal) minVal = v
                if (v > maxVal) maxVal = v
            }
        }

        globalMin = if (minVal == Double.MAX_VALUE) 0.0 else minVal
        globalMax = if (maxVal == -Double.MAX_VALUE) 1.0 else maxVal
        if (globalMax == globalMin) globalMax += 1.0
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Marginesy na podpisy osi
        contentBounds.set(70f, 30f, w - 30f, h - 60f)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var ret = scaleDetector.onTouchEvent(event)
        ret = gestureDetector.onTouchEvent(event) || ret
        return ret || super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawGridAndAxes(canvas)

        val data = seriesData ?: return
        val path = Path()

        for (r in data.indices) {
            val row = data[r]
            if (row.size < 2) continue

            path.reset()
            val isSelectedRow = (r == selectedRow)
            val paint = if (isSelectedRow) activeLinePaint else secondaryLinePaint

            for (c in row.indices) {
                val pt = calculateScreenCoordinates(c, row[c], row.size)
                if (c == 0) {
                    path.moveTo(pt.first, pt.second)
                } else {
                    path.lineTo(pt.first, pt.second)
                }

                if (isSelectedRow && c == selectedCol) {
                    canvas.drawCircle(pt.first, pt.second, 7f, highlightPointPaint)
                }
            }
            canvas.drawPath(path, paint)
        }
    }

    private fun drawGridAndAxes(canvas: Canvas) {
        // Obramowanie obszaru rysowania
        canvas.drawRect(contentBounds, axisPaint)

        // Linie poziome siatki (5 poziomów)
        val stepY = contentBounds.height() / 4f
        for (i in 0..4) {
            val y = contentBounds.top + (i * stepY)
            canvas.drawLine(contentBounds.left, y, contentBounds.right, y, gridPaint)

            val valueAtLevel = globalMax - (i * (globalMax - globalMin) / 4.0)
            canvas.drawText(String.format("%.0f", valueAtLevel), 10f, y + 10f, textPaint)
        }
    }

    private fun calculateScreenCoordinates(colIndex: Int, value: Double, totalCols: Int): Pair<Float, Float> {
        val normX = colIndex.toFloat() / (totalCols - 1)
        val normY = ((value - globalMin) / (globalMax - globalMin)).toFloat()

        val rawX = contentBounds.left + normX * contentBounds.width()
        val scaledX = contentBounds.left + (rawX - contentBounds.left) * scaleXFactor + translateX
        val screenY = contentBounds.bottom - (normY * contentBounds.height())

        return Pair(scaledX, screenY)
    }

    private fun findClosestPoint(touchX: Float, touchY: Float) {
        val data = seriesData ?: return
        var closestDistSq = Float.MAX_VALUE
        var targetRow = -1
        var targetCol = -1

        for (r in data.indices) {
            val row = data[r]
            for (c in row.indices) {
                val (sx, sy) = calculateScreenCoordinates(c, row[c], row.size)
                val dx = touchX - sx
                val dy = touchY - sy
                val distSq = dx * dx + dy * dy
                if (distSq < closestDistSq && distSq < 3000f) { // próg precyzji kliknięcia
                    closestDistSq = distSq
                    targetRow = r
                    targetCol = c
                }
            }
        }

        if (targetRow != -1) {
            selectedRow = targetRow
            selectedCol = targetCol
            invalidate()
        }
    }
}