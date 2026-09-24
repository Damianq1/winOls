package com.winols.app.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.winols.app.model.MapDefinition
import kotlin.math.max
import kotlin.math.min

/**
 * Custom View do wizualizacji danych binarnych oraz map w trybie 2D (wykres fali / profil krzywej).
 * Zoptymalizowany pod kątem braku alokacji pamięci w cyklu onDraw.
 */
class Wave2DView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Dane źródłowe
    private var rawValues: FloatArray = FloatArray(0)
    private var baseOffsetAddress: Long = 0L
    private var factor: Double = 1.0
    private var offset: Double = 0.0

    // Skrajne wartości do skalowania Y
    private var minY: Float = 0f
    private var maxY: Float = 1f

    // Transformacje widoku (Pan / Zoom)
    private var scaleX: Float = 1.0f
    private var translateX: Float = 0f
    private val minScaleX = 0.5f
    private val maxScaleX = 50.0f

    // Stan kursora
    private var selectedIndex: Int = -1
    private var isDraggingCursor: Boolean = false

    // Zasoby graficzne (reusable - zero allocation w onDraw)
    private val wavePath = Path()
    private val fillPath = Path()
    private val textBounds = Rect()

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#263238")
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#455A64")
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val baselinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#37474F")
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private val waveLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E676") // Styl WinOLS green wave
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private val wavePointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B9F6CA")
        strokeWidth = 6f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val waveFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1500E676")
        style = Paint.Style.FILL
    }

    private val cursorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF5252")
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CFD8DC")
        textSize = 28f
    }

    private val tooltipBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CC102027")
        style = Paint.Style.FILL
    }

    private val tooltipTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 30f
        isFakeBoldText = true
    }

    // Detektory gestów
    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val prevScale = scaleX
            scaleX = (scaleX * detector.scaleFactor).coerceIn(minScaleX, maxScaleX)

            // Centrowanie zoomu wokół punktu skupienia palców
            val focusX = detector.focusX
            translateX = focusX - (focusX - translateX) * (scaleX / prevScale)
            clampTranslation()
            invalidate()
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if (!isDraggingCursor) {
                translateX -= distanceX
                clampTranslation()
                invalidate()
                return true
            }
            return false
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            resolveCursorIndex(e.x)
            invalidate()
            return true
        }
    })

    /**
     * Wczytuje dane surowe z bufora z opcjonalnym adresem bazowym oraz formułą fizyczną.
     */
    fun setData(
        data: FloatArray,
        baseAddress: Long = 0L,
        factor: Double = 1.0,
        offset: Double = 0.0
    ) {
        this.rawValues = data.copyOf()
        this.baseOffsetAddress = baseAddress
        this.factor = factor
        this.offset = offset
        this.selectedIndex = -1

        calculateMinMax()
        resetViewport()
        invalidate()
    }

    /**
     * Alternatywne wczytanie z modelu MapDefinition i bufora przetłumaczonego na float.
     */
    fun setMap(mapDef: MapDefinition, rawPoints: FloatArray) {
        setData(
            data = rawPoints,
            baseAddress = mapDef.address.toLong(),
            factor = mapDef.factor,
            offset = mapDef.offset
        )
    }

    private fun calculateMinMax() {
        if (rawValues.isEmpty()) {
            minY = 0f
            maxY = 1f
            return
        }
        var min = rawValues[0]
        var max = rawValues[0]
        for (v in rawValues) {
            if (v < min) min = v
            if (v > max) max = v
        }
        if (min == max) {
            min -= 1f
            max += 1f
        }
        minY = min
        maxY = max
    }

    private fun resetViewport() {
        scaleX = 1.0f
        translateX = 0f
    }

    private fun clampTranslation() {
        if (rawValues.size <= 1) {
            translateX = 0f
            return
        }
        val totalContentWidth = (rawValues.size - 1) * getStepX() * scaleX
        val viewWidth = width.toFloat()

        if (totalContentWidth <= viewWidth) {
            translateX = 0f
        } else {
            val minTranslate = viewWidth - totalContentWidth
            val maxTranslate = 0f
            translateX = translateX.coerceIn(minTranslate, maxTranslate)
        }
    }

    private fun getStepX(): Float {
        val usableWidth = width - paddingLeft - paddingRight
        return if (rawValues.size > 1) {
            usableWidth.toFloat() / (rawValues.size - 1)
        } else {
            usableWidth.toFloat()
        }
    }

    private fun dataYToScreenY(value: Float): Float {
        val usableHeight = height - paddingTop - paddingBottom
        val normalized = (value - minY) / (maxY - minY)
        return height - paddingBottom - (normalized * usableHeight)
    }

    private fun dataIndexToScreenX(index: Int): Float {
        return paddingLeft + translateX + (index * getStepX() * scaleX)
    }

    private fun resolveCursorIndex(screenX: Float) {
        if (rawValues.isEmpty()) return
        val step = getStepX() * scaleX
        val relativeX = screenX - paddingLeft - translateX
        val index = (relativeX / step + 0.5f).toInt()
        selectedIndex = index.coerceIn(0, rawValues.size - 1)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (selectedIndex != -1) {
                    val cursorX = dataIndexToScreenX(selectedIndex)
                    if (kotlin.math.abs(event.x - cursorX) < 40f) {
                        isDraggingCursor = true
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDraggingCursor) {
                    resolveCursorIndex(event.x)
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDraggingCursor = false
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#121212"))

        drawGridAndAxes(canvas)

        if (rawValues.isEmpty()) {
            drawEmptyState(canvas)
            return
        }

        drawWave(canvas)
        drawCursor(canvas)
    }

    private fun drawGridAndAxes(canvas: Canvas) {
        val h = height.toFloat()
        val w = width.toFloat()

        // 4 linie podziału poziomego
        val stepsY = 4
        for (i in 0..stepsY) {
            val y = paddingTop + (h - paddingTop - paddingBottom) * (i.toFloat() / stepsY)
            canvas.drawLine(0f, y, w, y, gridPaint)

            val valueAtY = maxY - (i.toFloat() / stepsY) * (maxY - minY)
            val physicalVal = valueAtY * factor + offset
            val label = String.format("%.1f", physicalVal)
            canvas.drawText(label, 16f, y - 6f, labelPaint)
        }

        // Linia osi 0 jeśli mieści się w zakresie
        if (minY <= 0f && maxY >= 0f) {
            val zeroY = dataYToScreenY(0f)
            canvas.drawLine(0f, zeroY, w, zeroY, baselinePaint)
        }

        // Ramka
        canvas.drawRect(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            w - paddingRight,
            h - paddingBottom,
            axisPaint
        )
    }

    private fun drawWave(canvas: Canvas) {
        wavePath.reset()
        fillPath.reset()

        val bottomY = height.toFloat() - paddingBottom
        var firstX = 0f
        var lastX = 0f

        for (i in rawValues.indices) {
            val x = dataIndexToScreenX(i)
            val y = dataYToScreenY(rawValues[i])

            if (i == 0) {
                wavePath.moveTo(x, y)
                fillPath.moveTo(x, bottomY)
                fillPath.lineTo(x, y)
                firstX = x
            } else {
                wavePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            lastX = x

            // Rysowanie punktów danych przy odpowiednim przybliżeniu
            if (scaleX > 2.5f) {
                canvas.drawPoint(x, y, wavePointPaint)
            }
        }

        fillPath.lineTo(lastX, bottomY)
        fillPath.close()

        canvas.drawPath(fillPath, waveFillPaint)
        canvas.drawPath(wavePath, waveLinePaint)
    }

    private fun drawCursor(canvas: Canvas) {
        if (selectedIndex !in rawValues.indices) return

        val cursorX = dataIndexToScreenX(selectedIndex)
        val rawVal = rawValues[selectedIndex]
        val cursorY = dataYToScreenY(rawVal)
        val physicalVal = rawVal * factor + offset
        val pointAddress = baseOffsetAddress + (selectedIndex * 2) // domyślny skok słowa 16-bit

        // Pionowa linia wskaźnika
        canvas.drawLine(cursorX, paddingTop.toFloat(), cursorX, height - paddingBottom.toFloat(), cursorPaint)

        // Marker na fali
        canvas.drawCircle(cursorX, cursorY, 8f, cursorPaint)

        // Tooltip z informacjami
        val infoText = String.format(
            "[%d] 0x%X: RAW: %.0f | PHY: %.2f",
            selectedIndex,
            pointAddress,
            rawVal,
            physicalVal
        )
        tooltipTextPaint.getTextBounds(infoText, 0, infoText.length, textBounds)

        val padding = 16f
        val boxWidth = textBounds.width() + (padding * 2)
        val boxHeight = textBounds.height() + (padding * 2)

        var tooltipLeft = cursorX + 20f
        if (tooltipLeft + boxWidth > width - paddingRight) {
            tooltipLeft = cursorX - boxWidth - 20f
        }
        val tooltipTop = (cursorY - boxHeight - 20f).coerceAtLeast(paddingTop + 10f)

        canvas.drawRoundRect(
            tooltipLeft,
            tooltipTop,
            tooltipLeft + boxWidth,
            tooltipTop + boxHeight,
            8f,
            8f,
            tooltipBgPaint
        )

        canvas.drawText(
            infoText,
            tooltipLeft + padding,
            tooltipTop + boxHeight - padding - 4f,
            tooltipTextPaint
        )
    }

    private fun drawEmptyState(canvas: Canvas) {
        val msg = "Brak załadowanych danych osi/krzywej 2D"
        labelPaint.getTextBounds(msg, 0, msg.length, textBounds)
        val x = (width - textBounds.width()) / 2f
        val y = (height + textBounds.height()) / 2f
        canvas.drawText(msg, x, y, labelPaint)
    }
}