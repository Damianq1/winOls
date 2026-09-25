package com.winols.app.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.winols.app.domain.model.MapTable
import com.winols.app.domain.model.SelectionArea
import kotlin.math.floor

class TableMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var mapTable: MapTable? = null
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var selection: SelectionArea? = null
        private set

    var onSelectionChanged: ((SelectionArea?) -> Unit)? = null

    private val cellWidth = 140f
    private val cellHeight = 70f
    private val headerSize = 90f

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
    }

    private val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2A2A2A")
        style = Paint.Style.FILL
    }

    private val cellBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A1A1A")
        style = Paint.Style.FILL
    }

    private val selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4400E5FF")
        style = Paint.Style.FILL
    }

    private val selectionBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF")
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    private val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        textSize = 24f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val textBounds = Rect()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val table = mapTable
        val w = if (table != null) (headerSize + table.cols * cellWidth).toInt() else 0
        val h = if (table != null) (headerSize + table.rows * cellHeight).toInt() else 0
        setMeasuredDimension(
            resolveSize(w, widthMeasureSpec),
            resolveSize(h, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val table = mapTable ?: return

        // 1. Tło komórek danych
        for (r in 0 until table.rows) {
            for (c in 0 until table.cols) {
                val left = headerSize + c * cellWidth
                val top = headerSize + r * cellHeight
                val right = left + cellWidth
                val bottom = top + cellHeight

                canvas.drawRect(left, top, right, bottom, cellBgPaint)

                val isSelected = selection?.contains(r, c) == true
                if (isSelected) {
                    canvas.drawRect(left, top, right, bottom, selectionPaint)
                }

                canvas.drawRect(left, top, right, bottom, gridPaint)

                val valStr = table.get(r, c).toString()
                textPaint.getTextBounds(valStr, 0, valStr.length, textBounds)
                val textY = top + (cellHeight / 2f) + (textBounds.height() / 2f)
                val textX = left + (cellWidth / 2f)
                canvas.drawText(valStr, textX, textY, textPaint)
            }
        }

        // 2. Nagłówek osi X (Kolumny)
        canvas.drawRect(0f, 0f, headerSize + table.cols * cellWidth, headerSize, headerBgPaint)
        for (c in 0 until table.cols) {
            val left = headerSize + c * cellWidth
            canvas.drawRect(left, 0f, left + cellWidth, headerSize, gridPaint)
            val colLabel = String.format("%02X", c)
            headerTextPaint.getTextBounds(colLabel, 0, colLabel.length, textBounds)
            canvas.drawText(
                colLabel,
                left + cellWidth / 2f,
                headerSize / 2f + textBounds.height() / 2f,
                headerTextPaint
            )
        }

        // 3. Nagłówek osi Y (Wiersze)
        canvas.drawRect(0f, 0f, headerSize, headerSize + table.rows * cellHeight, headerBgPaint)
        for (r in 0 until table.rows) {
            val top = headerSize + r * cellHeight
            canvas.drawRect(0f, top, headerSize, top + cellHeight, gridPaint)
            val rowLabel = String.format("%02X", r)
            headerTextPaint.getTextBounds(rowLabel, 0, rowLabel.length, textBounds)
            canvas.drawText(
                rowLabel,
                headerSize / 2f,
                top + cellHeight / 2f + textBounds.height() / 2f,
                headerTextPaint
            )
        }

        // 4. Obrys aktywnej selekcji
        selection?.let { sel ->
            val selLeft = headerSize + sel.minCol * cellWidth
            val selTop = headerSize + sel.minRow * cellHeight
            val selRight = headerSize + (sel.maxCol + 1) * cellWidth
            val selBottom = headerSize + (sel.maxRow + 1) * cellHeight
            canvas.drawRect(selLeft, selTop, selRight, selBottom, selectionBorderPaint)
        }
    }

    private var touchStartRow = -1
    private var touchStartCol = -1

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val table = mapTable ?: return super.onTouchEvent(event)

        val col = floor((event.x - headerSize) / cellWidth).toInt().coerceIn(0, table.cols - 1)
        val row = floor((event.y - headerSize) / cellHeight).toInt().coerceIn(0, table.rows - 1)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                touchStartRow = row
                touchStartCol = col
                setSelection(SelectionArea(row, col, row, col))
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                setSelection(SelectionArea(touchStartRow, touchStartCol, row, col))
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun selectAll() {
        val table = mapTable ?: return
        setSelection(SelectionArea(0, 0, table.rows - 1, table.cols - 1))
    }

    fun clearSelection() {
        setSelection(null)
    }

    private fun setSelection(newSelection: SelectionArea?) {
        selection = newSelection
        onSelectionChanged?.invoke(newSelection)
        invalidate()
    }
}