package com.winols.app.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.OverScroller
import androidx.core.view.ViewCompat
import com.winols.app.data.BinaryBufferManager
import java.nio.ByteOrder

/**
 * Wysokowydajny komponent renderujący podgląd szesnastkowy (Hex Dump)
 * z wbudowaną wirtualizacją wierszy, obsługą pinch-to-zoom oraz smooth scrollingu.
 */
class HexGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var bufferManager: BinaryBufferManager? = null
    private var bytesPerRow = 16
    private var byteOrder = ByteOrder.BIG_ENDIAN
    private var dataType = BinaryBufferManager.DataType.UBYTE

    private var cellWidth = 0f
    private var rowHeight = 0f
    private var addressAreaWidth = 0f
    private var asciiAreaStartX = 0f

    private var scrollYOffset = 0f
    private val scroller = OverScroller(context)

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.MONOSPACE
        textSize = 32f
        color = Color.parseColor("#D0D0D0")
    }

    private val addressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.MONOSPACE
        textSize = 32f
        color = Color.parseColor("#4EC9B0")
    }

    private val asciiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.MONOSPACE
        textSize = 32f
        color = Color.parseColor("#CE9178")
    }

    private val gridLinePaint = Paint().apply {
        color = Color.parseColor("#2D2D2D")
        strokeWidth = 1f
    }

    private val backgroundPaint = Paint().apply {
        color = Color.parseColor("#1E1E1E")
    }

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            scrollYOffset = (scrollYOffset + distanceY).coerceIn(0f, maxScrollY())
            ViewCompat.postInvalidateOnAnimation(this@HexGridView)
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            scroller.fling(0, scrollYOffset.toInt(), 0, (-velocityY).toInt(), 0, 0, 0, maxScrollY().toInt())
            ViewCompat.postInvalidateOnAnimation(this@HexGridView)
            return true
        }
    })

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            textPaint.textSize = (textPaint.textSize * scaleFactor).coerceIn(18f, 64f)
            addressPaint.textSize = textPaint.textSize
            asciiPaint.textSize = textPaint.textSize
            calculateMetrics()
            invalidate()
            return true
        }
    })

    init {
        calculateMetrics()
    }

    fun attachBuffer(manager: BinaryBufferManager) {
        this.bufferManager = manager
        scrollYOffset = 0f
        invalidate()
    }

    fun setConfiguration(bytesPerRow: Int, type: BinaryBufferManager.DataType, order: ByteOrder) {
        this.bytesPerRow = bytesPerRow
        this.dataType = type
        this.byteOrder = order
        calculateMetrics()
        invalidate()
    }

    private fun calculateMetrics() {
        val fm = textPaint.fontMetrics
        rowHeight = (fm.bottom - fm.top) * 1.35f
        cellWidth = textPaint.measureText(" 00 ")
        addressAreaWidth = textPaint.measureText("00000000:  ")
        asciiAreaStartX = addressAreaWidth + (bytesPerRow * cellWidth) + 20f
    }

    private fun maxScrollY(): Float {
        val count = bufferManager?.capacity ?: 0
        val totalRows = (count + bytesPerRow - 1) / bytesPerRow
        return (totalRows * rowHeight - height).coerceAtLeast(0f)
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollYOffset = scroller.currY.toFloat()
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var handled = scaleDetector.onTouchEvent(event)
        if (!scaleDetector.isInProgress) {
            handled = gestureDetector.onTouchEvent(event) || handled
        }
        return handled || super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        val manager = bufferManager ?: return
        val totalBytes = manager.capacity
        if (totalBytes <= 0) return

        val totalRows = (totalBytes + bytesPerRow - 1) / bytesPerRow
        val firstVisibleRow = (scrollYOffset / rowHeight).toInt().coerceAtLeast(0)
        val lastVisibleRow = ((scrollYOffset + height) / rowHeight).toInt().coerceAtMost(totalRows - 1)

        val yBaselineOffset = -textPaint.fontMetrics.top

        for (row in firstVisibleRow..lastVisibleRow) {
            val yPos = row * rowHeight - scrollYOffset
            val baseline = yPos + yBaselineOffset
            val rowStartOffset = row * bytesPerRow

            // Rysowanie adresu w HEX
            val addrText = String.format("%08X: ", rowStartOffset)
            canvas.drawText(addrText, 10f, baseline, addressPaint)

            // Rysowanie bajtów / komórek
            val bytesInThisRow = (totalBytes - rowStartOffset).coerceAtMost(bytesPerRow)
            val rowBytes = manager.readBytes(rowStartOffset, bytesInThisRow)

            val asciiBuilder = StringBuilder()

            for (i in 0 until bytesInThisRow) {
                val b = rowBytes[i].toInt() and 0xFF
                val byteX = addressAreaWidth + (i * cellWidth)
                
                canvas.drawText(String.format("%02X", b), byteX, baseline, textPaint)

                // ASCII printable range: 32 - 126
                if (b in 32..126) {
                    asciiBuilder.append(b.toChar())
                } else {
                    asciiBuilder.append('.')
                }
            }

            // Sekcja podglądu ASCII
            canvas.drawText(asciiBuilder.toString(), asciiAreaStartX, baseline, asciiPaint)

            // Linia podziału wiersza
            canvas.drawLine(0f, yPos + rowHeight, width.toFloat(), yPos + rowHeight, gridLinePaint)
        }
    }
}