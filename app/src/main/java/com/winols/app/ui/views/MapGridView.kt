package com.winols.app.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.winols.app.data.BinaryBufferManager
import com.winols.app.model.MapDefinition
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Zoptymalizowany pod kątem 60 FPS widok tabeli mapy ECU.
 * Obsługuje gesty przesunięcia (Pan), powiększania (Pinch-to-zoom)
 * oraz precyzyjne zaznaczanie obszaru komórek do edycji masowej.
 */
class MapGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var mapDef: MapDefinition? = null
    private var bufferManager: BinaryBufferManager? = null

    // Konfiguracja wymiarów bazowych
    private val baseCellWidth = 180f
    private val baseCellHeight = 80f
    private val headerSize = 90f

    // Transformacje widoku
    private var scaleFactor = 1.0f
    private var scrollXOffset = 0f
    private var scrollYOffset = 0f

    // Zaznaczony obszar (indeksy macierzy)
    private var selStartRow = -1
    private var selStartCol = -1
    private var selEndRow = -1
    private var selEndCol = -1

    var onSelectionChangedListener: ((selectedRows: IntRange, selectedCols: IntRange) -> Unit)? = null

    // Paleta pędzli
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E0E0")
        textSize = 30f
        textAlign = Paint.Align.CENTER
    }

    private val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#90CAF9")
        textSize = 28f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    private val gridLinePaint = Paint().apply {
        color = Color.parseColor("#334155")
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val headerBgPaint = Paint().apply {
        color = Color.parseColor("#1E293B")
        style = Paint.Style.FILL
    }

    private val cellBgPaint = Paint().apply {
        color = Color.parseColor("#0F172A")
        style = Paint.Style.FILL
    }

    private val selectionFillPaint = Paint().apply {
        color = Color.parseColor("#4D3B82F6") // Półprzezroczysty błękit
        style = Paint.Style.FILL
    }

    private val selectionStrokePaint = Paint().apply {
        color = Color.parseColor("#60A5FA")
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = scaleFactor.coerceIn(0.5f, 2.5f)
            invalidate()
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            scrollXOffset -= distanceX
            scrollYOffset -= distanceY
            clampScroll()
            invalidate()
            return true
        }

        override fun onDown(e: MotionEvent): Boolean = true
    })

    fun setDataSource(map: MapDefinition, buffer: BinaryBufferManager) {
        this.mapDef = map
        this.bufferManager = buffer
        clearSelection()
        invalidate()
    }

    fun getSelectedRows(): IntRange {
        if (selStartRow == -1 || selEndRow == -1) return IntRange.EMPTY
        return min(selStartRow, selEndRow)..max(selStartRow, selEndRow)
    }

    fun getSelectedCols(): IntRange {
        if (selStartCol == -1 || selEndCol == -1) return IntRange.EMPTY
        return min(selStartCol, selEndCol)..max(selStartCol, selEndCol)
    }

    fun selectAll() {
        val map = mapDef ?: return
        selStartRow = 0
        selStartCol = 0
        selEndRow = map.rows - 1
        selEndCol = map.cols - 1
        onSelectionChangedListener?.invoke(getSelectedRows(), getSelectedCols())
        invalidate()
    }

    fun clearSelection() {
        selStartRow = -1
        selStartCol = -1
        selEndRow = -1
        selEndCol = -1
        invalidate()
    }

    private fun clampScroll() {
        val map = mapDef ?: return
        val totalWidth = headerSize + (map.cols * baseCellWidth * scaleFactor)
        val totalHeight = headerSize + (map.rows * baseCellHeight * scaleFactor)

        val minScrollX = min(0f, width - totalWidth)
        val minScrollY = min(0f, height - totalHeight)

        scrollXOffset = scrollXOffset.coerceIn(minScrollX, 0f)
        scrollYOffset = scrollYOffset.coerceIn(minScrollY, 0f)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val scaleHandled = scaleDetector.onTouchEvent(event)
        val gestureHandled = gestureDetector.onTouchEvent(event)

        if (event.pointerCount == 1) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val hit = findCellIndex(event.x, event.y)
                    if (hit != null) {
                        selStartRow = hit.first
                        selStartCol = hit.second
                        selEndRow = hit.first
                        selEndCol = hit.second
                        onSelectionChangedListener?.invoke(getSelectedRows(), getSelectedCols())
                        invalidate()
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    val hit = findCellIndex(event.x, event.y)
                    if (hit != null && (hit.first != selEndRow || hit.second != selEndCol)) {
                        selEndRow = hit.first
                        selEndCol = hit.second
                        onSelectionChangedListener?.invoke(getSelectedRows(), getSelectedCols())
                        invalidate()
                    }
                }
            }
        }
        return scaleHandled || gestureHandled || super.onTouchEvent(event)
    }

    private fun findCellIndex(touchX: Float, touchY: Float): Pair<Int, Int>? {
        val map = mapDef ?: return null
        val cellW = baseCellWidth * scaleFactor
        val cellH = baseCellHeight * scaleFactor

        val relativeX = touchX - scrollXOffset - headerSize
        val relativeY = touchY - scrollYOffset - headerSize

        if (relativeX < 0 || relativeY < 0) return null

        val col = (relativeX / cellW).toInt()
        val row = (relativeY / cellH).toInt()

        return if (row in 0 until map.rows && col in 0 until map.cols) {
            Pair(row, col)
        } else null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val map = mapDef ?: return
        val buffer = bufferManager ?: return

        val cellW = baseCellWidth * scaleFactor
        val cellH = baseCellHeight * scaleFactor
        textPaint.textSize = 28f * scaleFactor
        headerPaint.textSize = 24f * scaleFactor

        canvas.save()
        canvas.translate(scrollXOffset, scrollYOffset)

        // 1. Rysowanie siatki i komórek z danymi mapy
        val selectedRows = getSelectedRows()
        val selectedCols = getSelectedCols()

        for (r in 0 until map.rows) {
            val top = headerSize + r * cellH
            val bottom = top + cellH

            for (c in 0 until map.cols) {
                val left = headerSize + c * cellW
                val right = left + cellW

                // Tło komórki
                canvas.drawRect(left, top, right, bottom, cellBgPaint)

                // Podświetlenie selekcji
                if (r in selectedRows && c in selectedCols) {
                    canvas.drawRect(left, top, right, bottom, selectionFillPaint)
                }

                // Granice komórki
                canvas.drawRect(left, top, right, bottom, gridLinePaint)

                // Wartość przeliczona
                val byteOffset = (r * map.cols + c) * map.dataType.byteCount
                val rawVal = buffer.readRawValue(map.address + byteOffset, map.dataType, map.isBigEndian)
                val physVal = (rawVal * (if (map.factor == 0.0) 1.0 else map.factor)) + map.offset

                val textVal = if (physVal % 1.0 == 0.0) {
                    physVal.toLong().toString()
                } else {
                    String.format(Locale.US, "%.2f", physVal)
                }

                val textY = top + (cellH / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
                canvas.drawText(textVal, left + (cellW / 2f), textY, textPaint)
            }
        }

        // 2. Ramka wokół zaznaczonego zakresu
        if (!selectedRows.isEmpty() && !selectedCols.isEmpty()) {
            val selLeft = headerSize + selectedCols.first * cellW
            val selTop = headerSize + selectedRows.first * cellH
            val selRight = headerSize + (selectedCols.last + 1) * cellW
            val selBottom = headerSize + (selectedRows.last + 1) * cellH
            canvas.drawRect(selLeft, selTop, selRight, selBottom, selectionStrokePaint)
        }

        // 3. Nagłówki kolumn (Oś X)
        for (c in 0 until map.cols) {
            val left = headerSize + c * cellW
            val right = left + cellW
            canvas.drawRect(left, 0f, right, headerSize, headerBgPaint)
            canvas.drawRect(left, 0f, right, headerSize, gridLinePaint)

            val label = "C$c"
            val textY = (headerSize / 2f) - ((headerPaint.descent() + headerPaint.ascent()) / 2f)
            canvas.drawText(label, left + (cellW / 2f), textY, headerPaint)
        }

        // 4. Nagłówki wierszy (Oś Y)
        for (r in 0 until map.rows) {
            val top = headerSize + r * cellH
            val bottom = top + cellH
            canvas.drawRect(0f, top, headerSize, bottom, headerBgPaint)
            canvas.drawRect(0f, top, headerSize, bottom, gridLinePaint)

            val label = "R$r"
            val textY = top + (cellH / 2f) - ((headerPaint.descent() + headerPaint.ascent()) / 2f)
            canvas.drawText(label, headerSize / 2f, textY, headerPaint)
        }

        // 5. Lewy górny róg łączący osie
        canvas.drawRect(0f, 0f, headerSize, headerSize, headerBgPaint)
        canvas.drawRect(0f, 0f, headerSize, headerSize, gridLinePaint)

        canvas.restore()
    }
}