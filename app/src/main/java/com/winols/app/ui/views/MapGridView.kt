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
import com.winols.app.edit.CellCoordinate
import com.winols.app.edit.CellSelection
import com.winols.app.edit.MapEditor
import com.winols.app.model.MapDefinition
import java.util.Locale
import kotlin.math.abs

class MapGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var mapDefinition: MapDefinition? = null
    private var mapEditor: MapEditor? = null

    private var currentSelection: CellSelection? = null
    private var isBoxSelecting = false

    private val baseCellWidth = 140f
    private val baseCellHeight = 70f
    private val headerSize = 90f

    private var scaleFactor = 1.0f
    private var scrollX = 0f
    private var scrollY = 0f

    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }
    private val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(35, 39, 46)
        style = Paint.Style.FILL
    }
    private val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(209, 154, 102)
        textSize = 26f
        textAlign = Paint.Align.CENTER
    }
    private val selectionOverlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(90, 97, 175, 239)
        style = Paint.Style.FILL
    }
    private val selectionBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(97, 175, 239)
        style = Paint.Style.STROKE
        strokeWidth = 4f
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
            if (!isBoxSelecting) {
                scrollX -= distanceX
                scrollY -= distanceY
                clampScroll()
                invalidate()
                return true
            }
            return false
        }
    })

    fun bind(mapDef: MapDefinition, editor: MapEditor) {
        this.mapDefinition = mapDef
        this.mapEditor = editor
        this.currentSelection = null
        this.scrollX = 0f
        this.scrollY = 0f
        invalidate()
    }

    fun getSelection(): CellSelection? = currentSelection

    fun clearSelection() {
        currentSelection = null
        invalidate()
    }

    fun selectAll() {
        val map = mapDefinition ?: return
        currentSelection = CellSelection(0, 0, map.rows - 1, map.columns - 1)
        invalidate()
    }

    private fun clampScroll() {
        val map = mapDefinition ?: return
        val cellW = baseCellWidth * scaleFactor
        val cellH = baseCellHeight * scaleFactor

        val contentWidth = headerSize + map.columns * cellW
        val contentHeight = headerSize + map.rows * cellH

        val minScrollX = (width - contentWidth).coerceAtMost(0f)
        val minScrollY = (height - contentHeight).coerceAtMost(0f)

        scrollX = scrollX.coerceIn(minScrollX, 0f)
        scrollY = scrollY.coerceIn(minScrollY, 0f)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        if (scaleDetector.isInProgress) return true

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val coord = screenToCell(event.x, event.y)
                if (coord != null) {
                    isBoxSelecting = true
                    currentSelection = CellSelection(coord.row, coord.col, coord.row, coord.col)
                    invalidate()
                    return true
                } else {
                    isBoxSelecting = false
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isBoxSelecting) {
                    val coord = screenToCell(event.x, event.y)
                    if (coord != null && currentSelection != null) {
                        currentSelection = currentSelection!!.copy(endRow = coord.row, endCol = coord.col)
                        invalidate()
                        return true
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isBoxSelecting = false
            }
        }

        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    private fun screenToCell(x: Float, y: Float): CellCoordinate? {
        val map = mapDefinition ?: return null
        val cellW = baseCellWidth * scaleFactor
        val cellH = baseCellHeight * scaleFactor

        val adjustedX = x - scrollX - headerSize
        val adjustedY = y - scrollY - headerSize

        if (adjustedX < 0 || adjustedY < 0) return null

        val col = (adjustedX / cellW).toInt()
        val row = (adjustedY / cellH).toInt()

        return if (row in 0 until map.rows && col in 0 until map.columns) {
            CellCoordinate(row, col)
        } else {
            null
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val map = mapDefinition ?: return
        val editor = mapEditor ?: return

        canvas.drawColor(Color.rgb(24, 26, 31))

        val cellW = baseCellWidth * scaleFactor
        val cellH = baseCellHeight * scaleFactor
        textPaint.textSize = 28f * scaleFactor
        headerTextPaint.textSize = 24f * scaleFactor

        // Znajdź zakres min/max wartości dla mapy ciepła
        var minVal = Double.MAX_VALUE
        var maxVal = Double.MIN_VALUE
        for (r in 0 until map.rows) {
            for (c in 0 until map.columns) {
                val v = editor.getCellValue(map, r, c)
                if (v < minVal) minVal = v
                if (v > maxVal) maxVal = v
            }
        }
        val range = (maxVal - minVal).coerceAtLeast(0.0001)

        canvas.save()
        canvas.translate(scrollX, scrollY)

        // Rysowanie komórek danych
        for (r in 0 until map.rows) {
            val top = headerSize + r * cellH
            val bottom = top + cellH
            if (bottom + scrollY < headerSize || top + scrollY > height) continue

            for (c in 0 until map.columns) {
                val left = headerSize + c * cellW
                val right = left + cellW
                if (right + scrollX < headerSize || left + scrollX > width) continue

                val v = editor.getCellValue(map, r, c)
                val ratio = ((v - minVal) / range).toFloat().coerceIn(0f, 1f)

                cellPaint.color = calculateHeatmapColor(ratio)
                canvas.drawRect(left, top, right, bottom, cellPaint)
                canvas.drawRect(left, top, right, bottom, borderPaint)

                val textY = top + (cellH / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
                val formatted = String.format(Locale.US, "%.1f", v)
                canvas.drawText(formatted, left + cellW / 2f, textY, textPaint)
            }
        }

        // Zaznaczenie
        currentSelection?.let { sel ->
            val selLeft = headerSize + sel.minCol * cellW
            val selTop = headerSize + sel.minRow * cellH
            val selRight = headerSize + (sel.maxCol + 1) * cellW
            val selBottom = headerSize + (sel.maxRow + 1) * cellH

            canvas.drawRect(selLeft, selTop, selRight, selBottom, selectionOverlayPaint)
            canvas.drawRect(selLeft, selTop, selRight, selBottom, selectionBorderPaint)
        }

        canvas.restore()

        // Paski nagłówków (Sticky Headers)
        drawHeaders(canvas, map, cellW, cellH)
    }

    private fun drawHeaders(canvas: Canvas, map: MapDefinition, cellW: Float, cellH: Float) {
        // Sticky Header Kolumn (Górny pasek)
        canvas.drawRect(headerSize, 0f, width.toFloat(), headerSize, headerPaint)
        for (c in 0 until map.columns) {
            val left = headerSize + scrollX + c * cellW
            val right = left + cellW
            if (right < headerSize || left > width) continue

            val textY = (headerSize / 2f) - ((headerTextPaint.descent() + headerTextPaint.ascent()) / 2f)
            canvas.drawText("Y$c", left + cellW / 2f, textY, headerTextPaint)
        }

        // Sticky Header Wierszy (Lewy pasek)
        canvas.drawRect(0f, headerSize, headerSize, height.toFloat(), headerPaint)
        for (r in 0 until map.rows) {
            val top = headerSize + scrollY + r * cellH
            val bottom = top + cellH
            if (bottom < headerSize || top > height) continue

            val textY = top + (cellH / 2f) - ((headerTextPaint.descent() + headerTextPaint.ascent()) / 2f)
            canvas.drawText("X$r", headerSize / 2f, textY, headerTextPaint)
        }

        // Lewy górny róg
        canvas.drawRect(0f, 0f, headerSize, headerSize, headerPaint)
        canvas.drawText("R\\C", headerSize / 2f, headerSize / 2f - ((headerTextPaint.descent() + headerTextPaint.ascent()) / 2f), headerTextPaint)
    }

    private fun calculateHeatmapColor(ratio: Float): Int {
        // Interpolacja z ciemnoniebieskiego (chłodny), przez żółty, do głębokiej czerwieni (gorący)
        val r = (ratio * 210).toInt().coerceIn(20, 210)
        val g = ((1.0f - abs(ratio - 0.5f) * 2f) * 160).toInt().coerceIn(20, 160)
        val b = ((1.0f - ratio) * 220).toInt().coerceIn(40, 220)
        return Color.rgb(r, g, b)
    }
}