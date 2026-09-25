package com.winols.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

class EcuTableView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var rows: Int = 16
        private set
    var cols: Int = 16
        private set

    // Surowe wartości binarne mapy (16-bit unsigned / signed)
    private var data: Array<IntArray> = Array(rows) { IntArray(cols) }

    private val cellWidth = 140f
    private val cellHeight = 70f
    private val headerSize = 90f

    private var startRow = -1
    private var startCol = -1
    private var endRow = -1
    private var endCol = -1

    var onSelectionChangedListener: ((selRows: Int, selCols: Int) -> Unit)? = null

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    private val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#90CAF9")
        textSize = 26f
        textAlign = Paint.Align.CENTER
    }

    private val gridPaint = Paint().apply {
        color = Color.parseColor("#333333")
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val bgHeaderPaint = Paint().apply {
        color = Color.parseColor("#1E1E1E")
        style = Paint.Style.FILL
    }

    private val selectionPaint = Paint().apply {
        color = Color.parseColor("#4400E5FF")
        style = Paint.Style.FILL
    }

    private val textBounds = Rect()

    init {
        // Przykładowe dane początkowe mapy wtrysku/zapłonu
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                data[r][c] = 800 + (r * 120) + (c * 40)
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val totalWidth = (headerSize + (cols * cellWidth)).toInt()
        val totalHeight = (headerSize + (rows * cellHeight)).toInt()
        setMeasuredDimension(totalWidth, totalHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val totalW = headerSize + (cols * cellWidth)
        val totalH = headerSize + (rows * cellHeight)

        // Nagłówek osi X i Y
        canvas.drawRect(0f, 0f, totalW, headerSize, bgHeaderPaint)
        canvas.drawRect(0f, 0f, headerSize, totalH, bgHeaderPaint)

        for (c in 0 until cols) {
            val cx = headerSize + (c * cellWidth) + (cellWidth / 2f)
            val text = "X$c"
            headerPaint.getTextBounds(text, 0, text.length, textBounds)
            canvas.drawText(text, cx, (headerSize / 2f) + (textBounds.height() / 2f), headerPaint)
        }

        for (r in 0 until rows) {
            val cy = headerSize + (r * cellHeight) + (cellHeight / 2f)
            val text = "Y$r"
            headerPaint.getTextBounds(text, 0, text.length, textBounds)
            canvas.drawText(text, headerSize / 2f, cy + (textBounds.height() / 2f), headerPaint)
        }

        // Zaznaczenie obszaru (Selection bounds)
        if (hasSelection()) {
            val minR = min(startRow, endRow)
            val maxR = max(startRow, endRow)
            val minC = min(startCol, endCol)
            val maxC = max(startCol, endCol)

            val left = headerSize + (minC * cellWidth)
            val top = headerSize + (minR * cellHeight)
            val right = headerSize + ((maxC + 1) * cellWidth)
            val bottom = headerSize + ((maxR + 1) * cellHeight)

            canvas.drawRect(left, top, right, bottom, selectionPaint)
        }

        // Komórki danych i siatka
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val left = headerSize + (c * cellWidth)
                val top = headerSize + (r * cellHeight)

                canvas.drawRect(left, top, left + cellWidth, top + cellHeight, gridPaint)

                val valStr = data[r][c].toString()
                textPaint.getTextBounds(valStr, 0, valStr.length, textBounds)
                canvas.drawText(
                    valStr,
                    left + (cellWidth / 2f),
                    top + (cellHeight / 2f) + (textBounds.height() / 2f),
                    textPaint
                )
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        if (x < headerSize || y < headerSize) return super.onTouchEvent(event)

        val col = ((x - headerSize) / cellWidth).toInt().coerceIn(0, cols - 1)
        val row = ((y - headerSize) / cellHeight).toInt().coerceIn(0, rows - 1)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startRow = row
                startCol = col
                endRow = row
                endCol = col
                notifySelection()
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (endRow != row || endCol != col) {
                    endRow = row
                    endCol = col
                    notifySelection()
                    invalidate()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun notifySelection() {
        val countR = (max(startRow, endRow) - min(startRow, endRow)) + 1
        val countC = (max(startCol, endCol) - min(startCol, endCol)) + 1
        onSelectionChangedListener?.invoke(countR, countC)
    }

    fun hasSelection(): Boolean = startRow in 0 until rows && startCol in 0 until cols

    fun getSelectionRange(): Pair<IntRange, IntRange>? {
        if (!hasSelection()) return null
        val minR = min(startRow, endRow)
        val maxR = max(startRow, endRow)
        val minC = min(startCol, endCol)
        val maxC = max(startCol, endCol)
        return Pair(minR..maxR, minC..maxC)
    }

    // --- OPERACJE MASOWEJ EDYCJI ---

    fun applyOffset(value: Int) {
        val (rowRange, colRange) = getSelectionRange() ?: return
        for (r in rowRange) {
            for (c in colRange) {
                data[r][c] += value
            }
        }
        invalidate()
    }

    fun applyPercentage(percentage: Double) {
        val (rowRange, colRange) = getSelectionRange() ?: return
        val factor = 1.0 + (percentage / 100.0)
        for (r in rowRange) {
            for (c in colRange) {
                data[r][c] = (data[r][c] * factor).toInt()
            }
        }
        invalidate()
    }

    fun applyFixedValue(value: Int) {
        val (rowRange, colRange) = getSelectionRange() ?: return
        for (r in rowRange) {
            for (c in colRange) {
                data[r][c] = value
            }
        }
        invalidate()
    }

    fun applySmoothing() {
        val (rowRange, colRange) = getSelectionRange() ?: return
        val temp = Array(rows) { data[it].clone() }

        for (r in rowRange) {
            for (c in colRange) {
                var sum = 0
                var count = 0
                // Wygładzanie filtrem 3x3 w obrębie siatki
                for (dr in -1..1) {
                    for (dc in -1..1) {
                        val nr = r + dr
                        val nc = c + dc
                        if (nr in 0 until rows && nc in 0 until cols) {
                            sum += temp[nr][nc]
                            count++
                        }
                    }
                }
                data[r][c] = (sum / count)
            }
        }
        invalidate()
    }
}