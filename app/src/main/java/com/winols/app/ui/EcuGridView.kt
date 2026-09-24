package com.winols.app.ui

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
import com.winols.app.BinModel
import com.winols.app.model.MapDefinition
import java.util.Locale

class EcuGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var binModel: BinModel? = null
    private var activeMap: MapDefinition? = null

    private var cellWidth = 140f
    private var cellHeight = 70f
    private var scaleFactor = 1.0f
    private var scrollXOffset = 0f
    private var scrollYOffset = 0f

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    private val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#388E3C")
        textSize = 26f
        textAlign = Paint.Align.CENTER
    }

    private val gridLinePaint = Paint().apply {
        color = Color.DKGRAY
        strokeWidth = 1.5f
    }

    private val selectionPaint = Paint().apply {
        color = Color.parseColor("#442196F3")
        style = Paint.Style.FILL
    }

    private val diffPaint = Paint().apply {
        color = Color.parseColor("#B71C1C")
        style = Paint.Style.FILL
    }

    var selectedCell: Pair<Int, Int>? = null
        private set

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = scaleFactor.coerceIn(0.5f, 3.0f)
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
            scrollXOffset -= distanceX
            scrollYOffset -= distanceY
            invalidate()
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val map = activeMap ?: return false
            val scaledW = cellWidth * scaleFactor
            val scaledH = cellHeight * scaleFactor

            val touchX = e.x - scrollXOffset
            val touchY = e.y - scrollYOffset

            val col = ((touchX - scaledW) / scaledW).toInt()
            val row = ((touchY - scaledH) / scaledH).toInt()

            if (row in 0 until map.rows && col in 0 until map.columns) {
                selectedCell = Pair(row, col)
                invalidate()
                return true
            }
            return false
        }
    })

    fun bind(model: BinModel, map: MapDefinition) {
        this.binModel = model
        this.activeMap = map
        this.selectedCell = null
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var ret = scaleDetector.onTouchEvent(event)
        ret = gestureDetector.onTouchEvent(event) || ret
        return ret || super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val model = binModel ?: return
        val map = activeMap ?: return

        canvas.save()
        canvas.translate(scrollXOffset, scrollYOffset)

        val sW = cellWidth * scaleFactor
        val sH = cellHeight * scaleFactor

        // Nagłówek osi X
        for (c in 0 until map.columns) {
            val left = (c + 1) * sW
            val rect = Rect(left.toInt(), 0, (left + sW).toInt(), sH.toInt())
            canvas.drawRect(rect, gridLinePaint)
            val label = map.xAxis?.manualValues?.getOrNull(c)?.toString() ?: "${c + 1}"
            canvas.drawText(label, left + sW / 2, sH / 2 + 10, headerPaint)
        }

        for (r in 0 until map.rows) {
            val top = (r + 1) * sH
            // Nagłówek osi Y
            val yLabel = map.yAxis?.manualValues?.getOrNull(r)?.toString() ?: "${r + 1}"
            canvas.drawText(yLabel, sW / 2, top + sH / 2 + 10, headerPaint)

            for (c in 0 until map.columns) {
                val left = (c + 1) * sW
                val isSelected = selectedCell?.first == r && selectedCell?.second == c

                if (isSelected) {
                    canvas.drawRect(left, top, left + sW, top + sH, selectionPaint)
                }

                val value = model.getCellValue(map, r, c)
                val str = String.format(Locale.US, "%.1f", value)

                canvas.drawRect(left, top, left + sW, top + sH, gridLinePaint)
                canvas.drawText(str, left + sW / 2, top + sH / 2 + 10, textPaint)
            }
        }

        canvas.restore()
    }
}