package com.winols.app.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.winols.app.data.BinaryBufferManager
import com.winols.app.model.MapDefinition

class Surface3DView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var bufferManager: BinaryBufferManager? = null
    private var mapDefinition: MapDefinition? = null

    private var rotX = 45.0
    private var rotZ = 45.0
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#448AFF")
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val cellFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    fun setMap(manager: BinaryBufferManager, definition: MapDefinition) {
        this.bufferManager = manager
        this.mapDefinition = definition
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY
                rotZ += dx * 0.5
                rotX = Math.max(10.0, Math.min(80.0, rotX - dy * 0.5))
                lastTouchX = event.x
                lastTouchY = event.y
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val manager = bufferManager ?: return
        val map = mapDefinition ?: return

        val rows = map.rows
        val cols = map.columns
        if (rows < 2 || cols < 2) return

        val radX = Math.toRadians(rotX)
        val radZ = Math.toRadians(rotZ)
        val cosZ = Math.cos(radZ)
        val sinZ = Math.sin(radZ)
        val sinX = Math.sin(radX)
        val cosX = Math.cos(radX)

        val values = Array(rows) { DoubleArray(cols) }
        var minVal = Double.MAX_VALUE
        var maxVal = -Double.MAX_VALUE

        val bytesPerCell = map.representation.bitDepth.bytesPerElement
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val offset = (r * cols + c) * bytesPerCell
                val raw = manager.readRawValue(map.startAddress + offset, map.representation)
                val phys = map.representation.rawToPhysical(raw)
                values[r][c] = phys
                if (phys < minVal) minVal = phys
                if (phys > maxVal) maxVal = phys
            }
        }

        val range = if (maxVal == minVal) 1.0 else maxVal - minVal
        val centerX = width / 2f
        val centerY = height / 2f
        val scale = Math.min(width, height) * 0.6f

        val projX = Array(rows) { FloatArray(cols) }
        val projY = Array(rows) { FloatArray(cols) }

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val x = (c.toDouble() / (cols - 1)) - 0.5
                val y = (r.toDouble() / (rows - 1)) - 0.5
                val z = ((values[r][c] - minVal) / range) - 0.5

                val rx = x * cosZ - y * sinZ
                val ry = x * sinZ + y * cosZ
                val rz = z

                val finalY = (ry * cosX - rz * sinX)

                projX[r][c] = (centerX + (rx * scale)).toFloat()
                projY[r][c] = (centerY + (finalY * scale)).toFloat()
            }
        }

        for (r in 0 until rows - 1) {
            for (c in 0 until cols - 1) {
                val avgVal = (values[r][c] + values[r+1][c] + values[r][c+1] + values[r+1][c+1]) / 4.0
                val norm = ((avgVal - minVal) / range).toFloat().coerceIn(0f, 1f)

                val red = (norm * 255).toInt()
                val blue = ((1f - norm) * 255).toInt()
                cellFillPaint.color = Color.argb(180, red, 50, blue)

                val path = Path().apply {
                    moveTo(projX[r][c], projY[r][c])
                    lineTo(projX[r][c + 1], projY[r][c + 1])
                    lineTo(projX[r + 1][c + 1], projY[r + 1][c + 1])
                    lineTo(projX[r + 1][c], projY[r + 1][c])
                    close()
                }

                canvas.drawPath(path, cellFillPaint)
                canvas.drawPath(path, linePaint)
            }
        }
    }
}