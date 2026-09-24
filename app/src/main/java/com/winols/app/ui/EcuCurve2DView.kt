package com.winols.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.winols.app.BinModel
import com.winols.app.model.MapDefinition
import java.util.Locale

/**
 * Własny widok (Custom View) renderujący wykres 2D pojedynczego wiersza/krzywej (Characteristic Curve)
 * z obsługą interaktywnego wyboru punktu pracy oraz wizualizacją osi referencyjnej.
 */
class EcuCurve2DView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var binModel: BinModel? = null
    private var activeMap: MapDefinition? = null
    private var selectedRow: Int = 0

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E676")
        strokeWidth = 4.0f
        style = Paint.Style.STROKE
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val activePointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF5252")
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33FFFFFF")
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        textSize = 24f
    }

    private val curvePath = Path()
    private var selectedIndex: Int = -1

    fun bind(model: BinModel, map: MapDefinition, row: Int = 0) {
        this.binModel = model
        this.activeMap = map
        this.selectedRow = row.coerceIn(0, (map.rows - 1).coerceAtLeast(0))
        this.selectedIndex = -1
        invalidate()
    }

    fun setSelectedRow(row: Int) {
        val map = activeMap ?: return
        this.selectedRow = row.coerceIn(0, map.rows - 1)
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            val map = activeMap ?: return false
            val padding = 60f
            val graphWidth = width - 2 * padding
            val colStep = graphWidth / (map.columns - 1).coerceAtLeast(1)

            val clickedIdx = (((event.x - padding) + (colStep / 2)) / colStep).toInt()
            if (clickedIdx in 0 until map.columns) {
                selectedIndex = clickedIdx
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val model = binModel ?: return
        val map = activeMap ?: return
        if (map.columns < 2) return

        val paddingLeft = 80f
        val paddingRight = 40f
        val paddingTop = 60f
        val paddingBottom = 70f

        val graphWidth = width - paddingLeft - paddingRight
        val graphHeight = height - paddingTop - paddingBottom

        var minVal = Double.MAX_VALUE
        var maxVal = -Double.MAX_VALUE

        val values = DoubleArray(map.columns) { col ->
            val v = model.getCellValue(map, selectedRow, col)
            if (v < minVal) minVal = v
            if (v > maxVal) maxVal = v
            v
        }

        val range = if (maxVal > minVal) (maxVal - minVal) else 1.0

        // Siatka i etykiety min/max
        canvas.drawLine(paddingLeft, paddingTop, paddingLeft, paddingTop + graphHeight, gridPaint)
        canvas.drawLine(paddingLeft, paddingTop + graphHeight, paddingLeft + graphWidth, paddingTop + graphHeight, gridPaint)

        canvas.drawText(String.format(Locale.US, "%.1f %s", maxVal, map.unit), 10f, paddingTop + 20f, textPaint)
        canvas.drawText(String.format(Locale.US, "%.1f %s", minVal, map.unit), 10f, paddingTop + graphHeight, textPaint)

        curvePath.reset()
        val stepX = graphWidth / (map.columns - 1)

        val pointCoords = Array(map.columns) { FloatArray(2) }

        for (c in 0 until map.columns) {
            val px = paddingLeft + (c * stepX)
            val normalizedY = ((values[c] - minVal) / range).toFloat()
            val py = paddingTop + graphHeight - (normalizedY * graphHeight)

            pointCoords[c][0] = px
            pointCoords[c][1] = py

            if (c == 0) {
                curvePath.moveTo(px, py)
            } else {
                curvePath.lineTo(px, py)
            }
        }

        canvas.drawPath(curvePath, linePaint)

        // Rysowanie punktów węzłowych
        for (c in 0 until map.columns) {
            val px = pointCoords[c][0]
            val py = pointCoords[c][1]

            if (c == selectedIndex) {
                canvas.drawCircle(px, py, 12f, activePointPaint)
                val label = String.format(Locale.US, "%.1f", values[c])
                canvas.drawText(label, px - 20f, py - 20f, textPaint)
            } else {
                canvas.drawCircle(px, py, 6f, pointPaint)
            }

            // Oznaczenie punktu na osi X
            val xLabel = map.xAxis?.manualValues?.getOrNull(c)?.toString() ?: "$c"
            canvas.drawText(xLabel, px - 10f, height - 20f, textPaint)
        }
    }
}