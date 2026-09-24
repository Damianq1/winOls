package com.winols.app.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
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
 * Widok 2D renderujący ciągłą falę sygnału binarnego (reprezentacja wykresowa WinOLS).
 * Zoptymalizowany dla pamięci poprzez dynamiczne próbkowanie (downsampling).
 */
class Wave2DView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var bufferManager: BinaryBufferManager? = null
    private var dataType = BinaryBufferManager.DataType.UBYTE
    private var byteOrder = ByteOrder.BIG_ENDIAN

    private var visibleSamples = 512
    private var startOffset = 0

    private val wavePath = Path()
    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4EC9B0")
        strokeWidth = 2.5f
        style = Paint.Style.STROKE
    }

    private val axisPaint = Paint().apply {
        color = Color.parseColor("#333333")
        strokeWidth = 1.5f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        textSize = 24f
    }

    private val scroller = OverScroller(context)

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            val delta = (distanceX * (visibleSamples.toFloat() / width.toFloat())).toInt()
            val maxOffset = (bufferManager?.capacity ?: 0) - (visibleSamples * dataType.byteSize)
            startOffset = (startOffset + delta * dataType.byteSize).coerceIn(0, maxOffset.coerceAtLeast(0))
            ViewCompat.postInvalidateOnAnimation(this@Wave2DView)
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            val maxOffset = (bufferManager?.capacity ?: 0) - (visibleSamples * dataType.byteSize)
            scroller.fling(startOffset, 0, (-velocityX).toInt(), 0, 0, maxOffset.coerceAtLeast(0), 0, 0)
            ViewCompat.postInvalidateOnAnimation(this@Wave2DView)
            return true
        }
    })

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val factor = detector.scaleFactor
            visibleSamples = (visibleSamples / factor).toInt().coerceIn(64, 4096)
            invalidate()
            return true
        }
    })

    fun setBuffer(manager: BinaryBufferManager, type: BinaryBufferManager.DataType, order: ByteOrder) {
        this.bufferManager = manager
        this.dataType = type
        this.byteOrder = order
        invalidate()
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            startOffset = scroller.currX
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
        canvas.drawColor(Color.parseColor("#121212"))

        val manager = bufferManager ?: return
        val availableBytes = manager.capacity - startOffset
        if (availableBytes <= 0) return

        val samplesToRender = visibleSamples.coerceAtMost(availableBytes / dataType.byteSize)
        if (samplesToRender < 2) return

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val stepX = viewWidth / (samplesToRender - 1)

        val maxValue = when (dataType) {
            BinaryBufferManager.DataType.UBYTE -> 255.0
            BinaryBufferManager.DataType.SBYTE -> 127.0
            BinaryBufferManager.DataType.UINT16 -> 65535.0
            BinaryBufferManager.DataType.SINT16 -> 32767.0
            BinaryBufferManager.DataType.UINT32, BinaryBufferManager.DataType.SINT32 -> 100000.0 // Skalowanie poglądowe
        }

        wavePath.reset()
        val midY = viewHeight / 2f
        canvas.drawLine(0f, midY, viewWidth, midY, axisPaint)

        for (i in 0 until samplesToRender) {
            val sampleOffset = startOffset + (i * dataType.byteSize)
            val rawValue = manager.readValue(sampleOffset, dataType, byteOrder)

            val normalized = (rawValue / maxValue).coerceIn(-1.0, 1.0)
            val x = i * stepX
            val y = viewHeight - ((normalized + 1.0) / 2.0 * viewHeight).toFloat()

            if (i == 0) {
                wavePath.moveTo(x, y)
            } else {
                wavePath.lineTo(x, y)
            }
        }

        canvas.drawPath(wavePath, wavePaint)
        canvas.drawText("Offset: 0x${Integer.toHexString(startOffset).uppercase()}", 20f, 40f, textPaint)
        canvas.drawText("Samples: $samplesToRender", 20f, 75f, textPaint)
    }
}