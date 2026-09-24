package com.winols.app.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.max
import kotlin.math.min

/**
 * Interaktywny wykres 2D fali/profilu mapy (dla wybranego wiersza, kolumny lub spłaszczonych danych RAW/fizycznych).
 * Obsługuje:
 * - Płynny Pinch-to-Zoom i Pan (przesuwanie poziome/pionowe)
 * - Dotykowe wskazywanie i zaznaczanie punktu z HUDem (wartość, indeks)
 * - Rysowanie dynamicznej siatki, linii zerowej i gradientu pod wykresem
 */
class Wave2DView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface OnPointSelectedListener {
        fun onPointSelected(index: Int, rawValue: Double, physicalValue: Double)
    }

    var pointSelectedListener: OnPointSelectedListener? = null

    // Dane do wyświetlenia
    private var rawData: DoubleArray = doubleArrayOf()
    private var physicalData: DoubleArray = doubleArrayOf()

    // Zakresy danych
    private var minVal: Double = 0.0
    private var maxVal: Double = 1.0

    // Transformacje widoku (Zoom & Pan)
    private var scaleXFactor = 1.0f
    private var scaleYFactor = 1.0f
    private var translationXOffset = 0.0f
    private var translationYOffset = 0.0f

    // Wskaźnik i selekcja
    private var selectedIndex = -1
    private val selectedPointPos = PointF()

    // Narzędzia do rysowania (recykling obiektów, brak alokacji w onDraw)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF") // Akcent cyjan/neon
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2600E5FF") // Przezroczyste wypełnienie pod falą
        style = Paint.Style.FILL
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD600")
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#22FFFFFF")
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
    }

    private val axisTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80FFFFFF")
        textSize = 24f
    }

    private val cursorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF5252")
        strokeWidth = 2f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private val hudBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CC1E1E1E")
        style = Paint.Style.FILL
    }

    private val hudTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        isFakeBoldText = true
    }

    private val wavePath = Path()
    private val fillPath = Path()

    // Detektory gestów
    private val scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleXFactor = (scaleXFactor * detector.scaleFactor).coerceIn(0.5f, 20f)
            invalidate()
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            translationXOffset -= distanceX
            translationYOffset -= distanceY
            clampOffsets()
            invalidate()
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            findClosestPoint(e.x, e.y)
            invalidate()
            return true
        }
    })

    fun setData(raw: DoubleArray, physical: DoubleArray = raw) {
        this.rawData = raw
        this.physicalData = physical

        if (raw.isNotEmpty()) {
            var min = raw[0]
            var max = raw[0]
            for (v in raw) {
                if (v < min) min = v
                if (v > max) max = v
            }
            if (min == max) {
                min -= 1.0
                max += 1.0
            }
            this.minVal = min
            this.maxVal = max
        } else {
            this.minVal = 0.0
            this.maxVal = 1.0
        }

        selectedIndex = -1
        resetView()
        invalidate()
    }

    fun resetView() {
        scaleXFactor = 1.0f
        scaleYFactor = 1.0f
        translationXOffset = 0.0f
        translationYOffset = 0.0f
        clampOffsets()
    }

    private fun clampOffsets() {
        // Ograniczenie przesuwania
        val maxScrollX = width * (scaleXFactor - 1f)
        if (scaleXFactor <= 1.0f) {
            translationXOffset = 0f
        } else {
            translationXOffset = translationXOffset.coerceIn(-maxScrollX, 0f)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var handled = scaleGestureDetector.onTouchEvent(event)
        handled = gestureDetector.onTouchEvent(event) || handled

        if (event.actionMasked == MotionEvent.ACTION_MOVE && selectedIndex != -1) {
            findClosestPoint(event.x, event.y)
            invalidate()
            handled = true
        }

        return handled || super.onTouchEvent(event)
    }

    private fun findClosestPoint(touchX: Float, touchY: Float) {
        if (rawData.isEmpty()) return

        val paddingLeft = paddingLeft.toFloat()
        val paddingRight = paddingRight.toFloat()
        val contentW = (width - paddingLeft - paddingRight) * scaleXFactor
        val stepX = contentW / (rawData.size - 1).coerceAtLeast(1)

        val localX = touchX - paddingLeft - translationXOffset
        val idx = (localX / stepX).toInt().coerceIn(0, rawData.size - 1)

        selectedIndex = idx
        val raw = rawData[idx]
        val phys = if (idx < physicalData.size) physicalData[idx] else raw
        pointSelectedListener?.onPointSelected(idx, raw, phys)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val pLeft = paddingLeft.toFloat()
        val pRight = paddingRight.toFloat()
        val pTop = paddingTop.toFloat()
        val pBottom = paddingBottom.toFloat()

        val graphW = w - pLeft - pRight
        val graphH = h - pTop - pBottom

        if (graphW <= 0 || graphH <= 0) return

        // Rysowanie tła / siatki
        val gridLinesH = 5
        for (i in 0..gridLinesH) {
            val y = pTop + (graphH / gridLinesH) * i
            canvas.drawLine(pLeft, y, w - pRight, y, gridPaint)
            val valStep = maxVal - (i.toDouble() / gridLinesH) * (maxVal - minVal)
            canvas.drawText(String.format("%.1f", valStep), pLeft + 10f, y - 8f, axisTextPaint)
        }

        if (rawData.size < 2) return

        canvas.save()
        canvas.clipRect(pLeft, pTop, w - pRight, h - pBottom)
        canvas.translate(translationXOffset, translationYOffset)

        val totalWidth = graphW * scaleXFactor
        val stepX = totalWidth / (rawData.size - 1)
        val range = (maxVal - minVal).coerceAtLeast(0.0001)

        wavePath.reset()
        fillPath.reset()

        var firstX = 0f
        var firstY = 0f
        var lastX = 0f

        for (i in rawData.indices) {
            val x = pLeft + i * stepX
            val normalizedY = ((rawData[i] - minVal) / range).toFloat()
            val y = pTop + graphH - (normalizedY * graphH)

            if (i == 0) {
                wavePath.moveTo(x, y)
                fillPath.moveTo(x, pTop + graphH)
                fillPath.lineTo(x, y)
                firstX = x
                firstY = y
            } else {
                wavePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }

            if (i == rawData.size - 1) {
                lastX = x
            }

            // Zapamiętanie współrzędnych zaznaczonego punktu
            if (i == selectedIndex) {
                selectedPointPos.set(x, y)
            }
        }

        fillPath.lineTo(lastX, pTop + graphH)
        fillPath.close()

        // Rysowanie fali i jej wypełnienia
        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(wavePath, linePaint)

        // Rysowanie kursorów i punktu wybranego
        if (selectedIndex in rawData.indices) {
            // Linie pomocnicze kursora
            canvas.drawLine(selectedPointPos.x, pTop, selectedPointPos.x, pTop + graphH, cursorPaint)
            canvas.drawLine(pLeft, selectedPointPos.y, pLeft + totalWidth, selectedPointPos.y, cursorPaint)

            // Aktywny punkt
            canvas.drawCircle(selectedPointPos.x, selectedPointPos.y, 10f, pointPaint)
        }

        canvas.restore()

        // Rysowanie HUD z informacją o zaznaczonym punkcie (poza macierzą przesunięcia)
        if (selectedIndex in rawData.indices) {
            drawHud(canvas, w, pTop)
        }
    }

    private fun drawHud(canvas: Canvas, w: Float, top: Float) {
        val raw = rawData[selectedIndex]
        val phys = if (selectedIndex < physicalData.size) physicalData[selectedIndex] else raw
        val text = "Index: $selectedIndex | RAW: ${raw.toLong()} | Phys: ${String.format("%.2f", phys)}"
        val textW = hudTextPaint.measureText(text)
        val hudRectLeft = (w - textW) / 2f - 24f
        val hudRectRight = hudRectLeft + textW + 48f
        val hudRectTop = top + 16f
        val hudRectBottom = hudRectTop + 56f

        canvas.drawRoundRect(hudRectLeft, hudRectTop, hudRectRight, hudRectBottom, 12f, 12f, hudBgPaint)
        canvas.drawText(text, hudRectLeft + 24f, hudRectTop + 38f, hudTextPaint)
    }
}