package com.winols.app.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import com.winols.app.data.DataFormatterEngine
import com.winols.app.model.ViewConfiguration
import java.nio.ByteBuffer
import java.util.Locale

/**
 * Zoptymalizowany Custom View do płynnego renderowania heksadecymalnego/tabelarycznego.
 */
class HexGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var buffer: ByteBuffer? = null
    private var config = ViewConfiguration()
    private var startOffset: Int = 0

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 32f
        typeface = Typeface.MONOSPACE
    }

    private val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        textSize = 28f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }

    private val offsetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.CYAN
        textSize = 30f
        typeface = Typeface.MONOSPACE
    }

    fun bindBuffer(buf: ByteBuffer?, initialOffset: Int = 0) {
        this.buffer = buf
        this.startOffset = initialOffset
        invalidate()
    }

    fun applyConfiguration(newConfig: ViewConfiguration) {
        this.config = newConfig
        invalidate()
    }

    fun scrollToOffset(offset: Int) {
        this.startOffset = offset.coerceAtLeast(0)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val buf = buffer ?: return

        val step = config.wordSize.bytesCount
        val cols = config.columnsCount
        val rowHeight = 48f
        val colWidth = if (config.isHexDisplay) (config.wordSize.bytesCount * 28f + 30f) else 120f
        val offsetColWidth = 180f

        var y = 60f

        // Nagłówek kolumn
        for (col in 0 until cols) {
            val colHeader = String.format(Locale.US, "+%X", col * step)
            canvas.drawText(colHeader, offsetColWidth + (col * colWidth), y, headerPaint)
        }
        y += rowHeight

        val visibleRows = ((height - y) / rowHeight).toInt()
        val totalBytesPerRow = cols * step

        var currentAddress = startOffset

        for (row in 0 until visibleRows) {
            if (currentAddress >= buf.capacity()) break

            // Rysowanie adresu wiersza
            val addressText = String.format(Locale.US, "%08X:", currentAddress)
            canvas.drawText(addressText, 16f, y, offsetPaint)

            // Rysowanie poszczególnych komórek w wierszu
            for (col in 0 until cols) {
                val cellAddress = currentAddress + (col * step)
                if (cellAddress + step <= buf.capacity()) {
                    val formatted = DataFormatterEngine.formatValue(buf, cellAddress, config)
                    canvas.drawText(formatted, offsetColWidth + (col * colWidth), y, textPaint)
                }
            }

            currentAddress += totalBytesPerRow
            y += rowHeight
        }
    }
}