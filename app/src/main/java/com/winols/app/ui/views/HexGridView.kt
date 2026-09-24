package com.winols.app.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Scroller
import com.winols.app.data.BinaryBufferManager
import com.winols.app.model.BitDepth
import com.winols.app.model.DataRepresentation
import com.winols.app.model.ValueType

class HexGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var bufferManager: BinaryBufferManager? = null
    private var representation: DataRepresentation = DataRepresentation(BitDepth.BITS_8, ValueType.HEX)

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 32f
        typeface = Typeface.MONOSPACE
    }

    private val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF50")
        textSize = 32f
        typeface = Typeface.MONOSPACE
    }

    private val modifiedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF5252")
        textSize = 32f
        typeface = Typeface.MONOSPACE
    }

    private val rowHeight = 44f
    private val colWidth = 64f
    private val addressOffset = 180f
    private val bytesPerRow = 16

    private val scroller = Scroller(context)
    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            scrollBy(0, distanceY.toInt())
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            val totalRows = ((bufferManager?.capacity ?: 0) + bytesPerRow - 1) / bytesPerRow
            val maxScrollY = Math.max(0, (totalRows * rowHeight).toInt() - height)
            scroller.fling(scrollX, scrollY, 0, -velocityY.toInt(), 0, 0, 0, maxScrollY)
            postInvalidateOnAnimation()
            return true
        }
    })

    fun setBufferManager(manager: BinaryBufferManager) {
        this.bufferManager = manager
        invalidate()
    }

    fun setRepresentation(rep: DataRepresentation) {
        this.representation = rep
        invalidate()
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.currX, scroller.currY)
            postInvalidateOnAnimation()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val manager = bufferManager ?: return
        val totalBytes = manager.capacity
        if (totalBytes == 0) return

        val startRow = Math.max(0, (scrollY / rowHeight).toInt())
        val endRow = Math.min((totalBytes + bytesPerRow - 1) / bytesPerRow, ((scrollY + height) / rowHeight).toInt() + 1)

        val bytesPerElement = representation.bitDepth.bytesPerElement
        val elementsPerRow = bytesPerRow / bytesPerElement

        for (r in startRow until endRow) {
            val rowY = (r + 1) * rowHeight
            val baseAddress = r * bytesPerRow

            val addrStr = String.format("%08X:", baseAddress)
            canvas.drawText(addrStr, 10f, rowY, headerPaint)

            for (c in 0 until elementsPerRow) {
                val elementAddress = baseAddress + (c * bytesPerElement)
                if (elementAddress >= totalBytes) break

                val isMod = manager.isModified(elementAddress, bytesPerElement)
                val paint = if (isMod) modifiedPaint else textPaint

                val raw = manager.readRawValue(elementAddress, representation)
                val str = when (representation.bitDepth) {
                    BitDepth.BITS_8 -> String.format("%02X", raw and 0xFF)
                    BitDepth.BITS_16 -> String.format("%04X", raw and 0xFFFF)
                    BitDepth.BITS_32 -> String.format("%08X", raw and 0xFFFFFFFFL)
                }

                val posX = addressOffset + (c * (colWidth * bytesPerElement))
                canvas.drawText(str, posX, rowY, paint)
            }
        }
    }
}