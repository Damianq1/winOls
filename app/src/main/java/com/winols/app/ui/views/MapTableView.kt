package com.winols.app.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.max
import kotlin.math.min

class MapTableView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var rows = 16
    private var cols = 16
    private var data: IntArray = IntArray(rows * cols)

    private var cellWidth = 140f
    private var cellHeight = 70f
    private var headerWidth = 120f
    private var headerHeight = 60f

    private var scrollX = 0f
    private var scrollY = 0f
    private var scaleFactor = 1.0f

    private var selectedRow = -1
    private var selectedCol = -1

    var onCellSelected: ((row: Int, col: Int, value: Int) -> Unit)? = null

    private val bgPaint = Paint().apply { color = 0xFF121212.toInt() }
    private val gridPaint = Paint().apply {
        color = 0xFF282828.toInt()
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }
    private val headerBgPaint = Paint().apply { color = 0xFF1E1E1E.toInt() }
    private val selectedCellPaint = Paint().apply {
        color = 0x4400E5FF.toInt()
        style = Paint.Style.FILL
    }
    private val textPaint = Paint().apply {
        color = 0xFFECEFF1.toInt()
        textSize = 28f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    private val headerTextPaint = Paint().apply {
        color = 0xFFFFAB00.toInt()
        textSize = 26f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = scaleFactor.coerceIn(0.6f, 2.5f)
            invalidate()
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            scrollX += distanceX
            scrollY += distanceY
            clampScroll()
            invalidate()
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val effCellW = cellWidth * scaleFactor
            val effCellH = cellHeight * scaleFactor

            val touchX = e.x + scrollX - headerWidth
            val touchY = e.y + scrollY - headerHeight

            if (touchX >= 0 && touchY >= 0) {
                val c = (touchX / effCellW).toInt()
                val r = (touchY / effCellH).toInt()
                if (r in 0 until rows && c in 0 until cols) {
                    selectedRow = r
                    selectedCol = c
                    val valIdx = r * cols + c
                    onCellSelected?.invoke(r, c, data.getOrElse(valIdx) { 0 })
                    invalidate()
                    return true
                }
            }
            return false
        }
    })

    fun setMapData(numRows: Int, numCols: Int, values: IntArray) {
        this.rows = max(1, numRows)
        this.cols = max(1, numCols)
        this.data = values
        this.selectedRow = -1
        this.selectedCol = -1
        clampScroll()
        invalidate()
    }

    private fun clampScroll() {
        val effCellW = cellWidth * scaleFactor
        val effCellH = cellHeight * scaleFactor
        val totalWidth = headerWidth + cols * effCellW
        val totalHeight = headerHeight + rows * effCellH

        val maxScrollX = max(0f, totalWidth - width)
        val maxScrollY = max(0f, totalHeight - height)

        scrollX = scrollX.coerceIn(0f, maxScrollX)
        scrollY = scrollY.coerceIn(0f, maxScrollY)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var handled = scaleDetector.onTouchEvent(event)
        handled = gestureDetector.onTouchEvent(event) || handled
        return handled || super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val effCellW = cellWidth * scaleFactor
        val effCellH = cellHeight * scaleFactor

        canvas.save()
        canvas.translate(-scrollX, -scrollY)

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val x = headerWidth + c * effCellW
                val y = headerHeight + r * effCellH

                if (x + effCellW < scrollX || x > scrollX + width ||
                    y + effCellH < scrollY || y > scrollY + height) {
                    continue
                }

                if (r == selectedRow && c == selectedCol) {
                    canvas.drawRect(x, y, x + effCellW, y + effCellH, selectedCellPaint)
                }

                canvas.drawRect(x, y, x + effCellW, y + effCellH, gridPaint)

                val valIdx = r * cols + c
                val cellVal = data.getOrElse(valIdx) { 0 }
                val textBaseline = y + (effCellH / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
                canvas.drawText("%04X".format(cellVal), x + effCellW / 2f, textBaseline, textPaint)
            }
        }
        canvas.restore()

        // Persistent Row & Col Headers
        canvas.save()
        canvas.translate(-scrollX, 0f)
        for (c in 0 until cols) {
            val x = headerWidth + c * effCellW
            if (x + effCellW < scrollX || x > scrollX + width) continue
            canvas.drawRect(x, 0f, x + effCellW, headerHeight, headerBgPaint)
            canvas.drawRect(x, 0f, x + effCellW, headerHeight, gridPaint)
            val baseline = headerHeight / 2f - ((headerTextPaint.descent() + headerTextPaint.ascent()) / 2f)
            canvas.drawText("Y:%02d".format(c), x + effCellW / 2f, baseline, headerTextPaint)
        }
        canvas.restore()

        canvas.save()
        canvas.translate(0f, -scrollY)
        for (r in 0 until rows) {
            val y = headerHeight + r * effCellH
            if (y + effCellH < scrollY || y > scrollY + height) continue
            canvas.drawRect(0f, y, headerWidth, y + effCellH, headerBgPaint)
            canvas.drawRect(0f, y, headerWidth, y + effCellH, gridPaint)
            val baseline = y + effCellH / 2f - ((headerTextPaint.descent() + headerTextPaint.ascent()) / 2f)
            canvas.drawText("X:%02d".format(r), headerWidth / 2f, baseline, headerTextPaint)
        }
        canvas.restore()

        canvas.drawRect(0f, 0f, headerWidth, headerHeight, headerBgPaint)
        canvas.drawRect(0f, 0f, headerWidth, headerHeight, gridPaint)
    }
}