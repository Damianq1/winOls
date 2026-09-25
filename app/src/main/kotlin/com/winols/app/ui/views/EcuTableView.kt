package com.winols.app.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.max
import kotlin.math.min

/**
 * Custom View do renderowania matrycy komórek mapy ECU z obsługą przewijania,
 * skalowania pinch-to-zoom oraz selekcji komórki.
 */
class EcuTableView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var rows = 16
    private var cols = 16
    private var data: Array<IntArray> = Array(rows) { IntArray(cols) { 0 } }

    private var cellWidth = 140f
    private var cellHeight = 70f
    private var scaleFactor = 1.0f

    private var offsetX = 0f
    private var offsetY = 0f

    private var selectedRow = -1
    private var selectedCol = -1

    var onCellSelectedListener: ((row: Int, col: Int, value: Int) -> Unit)? = null

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    private val selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#332196F3")
        style = Paint.Style.FILL
    }

    private val selectionBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2196F3")
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private val textBounds = Rect()

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
            offsetX -= distanceX
            offsetY -= distanceY
            clampOffsets()
            invalidate()
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val adjustedX = (e.x - offsetX) / scaleFactor
            val adjustedY = (e.y - offsetY) / scaleFactor

            val col = (adjustedX / cellWidth).toInt()
            val row = (adjustedY / cellHeight).toInt()

            if (row in 0 until rows && col in 0 until cols) {
                selectedRow = row
                selectedCol = col
                onCellSelectedListener?.invoke(row, col, data[row][col])
                invalidate()
                return true
            }
            return super.onSingleTapUp(e)
        }
    })

    fun setMapData(matrix: Array<IntArray>) {
        if (matrix.isEmpty() || matrix[0].isEmpty()) return
        this.rows = matrix.size
        this.cols = matrix[0].size
        this.data = matrix
        selectedRow = -1
        selectedCol = -1
        invalidate()
    }

    fun updateSelectedCellValue(delta: Int) {
        if (selectedRow in 0 until rows && selectedCol in 0 until cols) {
            data[selectedRow][selectedCol] = (data[selectedRow][selectedCol] + delta).coerceIn(0, 65535)
            onCellSelectedListener?.invoke(selectedRow, selectedCol, data[selectedRow][selectedCol])
            invalidate()
        }
    }

    private fun clampOffsets() {
        val totalWidth = cols * cellWidth * scaleFactor
        val totalHeight = rows * cellHeight * scaleFactor

        val minX = min(0f, width - totalWidth)
        val minY = min(0f, height - totalHeight)

        offsetX = offsetX.coerceIn(minX, 0f)
        offsetY = offsetY.coerceIn(minY, 0f)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var retVal = scaleDetector.onTouchEvent(event)
        retVal = gestureDetector.onTouchEvent(event) || retVal
        return retVal || super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scaleFactor, scaleFactor)

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val left = c * cellWidth
                val top = r * cellHeight
                val right = left + cellWidth
                val bottom = top + cellHeight

                if (r == selectedRow && c == selectedCol) {
                    canvas.drawRect(left, top, right, bottom, selectionPaint)
                    canvas.drawRect(left, top, right, bottom, selectionBorderPaint)
                } else {
                    canvas.drawRect(left, top, right, bottom, gridPaint)
                }

                val valStr = data[r][c].toString()
                textPaint.getTextBounds(valStr, 0, valStr.length, textBounds)
                val textX = left + cellWidth / 2f
                val textY = top + (cellHeight / 2f) + (textBounds.height() / 2f)

                canvas.drawText(valStr, textX, textY, textPaint)
            }
        }
        canvas.restore()
    }
}