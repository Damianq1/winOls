package com.winols.app.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import com.winols.app.data.DataFormatterEngine
import com.winols.app.model.DataRepresentationConfig
import java.nio.ByteBuffer

/**
 * Zoptymalizowany pod kątem pamięci widok siatki binarnej/HEX.
 * Umożliwia płynne przewijanie i natychmiastowe przerysowanie przy zmianie konfiguracji endianness/bitów.
 */
class HexGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var buffer: ByteBuffer? = null
    private var baseOffset: Int = 0
    private var rowsCount: Int = 16
    private var columnsCount: Int = 16

    private var representationConfig = DataRepresentationConfig()
    private var showPhysicalValues: Boolean = false

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E0E0")
        textSize = 32f
        typeface = Typeface.MONOSPACE
    }

    private val addressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4FC3F7")
        textSize = 30f
        typeface = Typeface.MONOSPACE
    }

    fun setBuffer(byteBuffer: ByteBuffer, initialOffset: Int = 0) {
        this.buffer = byteBuffer
        this.baseOffset = initialOffset
        invalidate()
    }

    fun updateConfig(newConfig: DataRepresentationConfig, showPhysical: Boolean = false) {
        this.representationConfig = newConfig
        this.showPhysicalValues = showPhysical
        invalidate()
    }

    fun scrollToOffset(offset: Int) {
        this.baseOffset = offset
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val buf = buffer ?: return

        val cellWidth = width / (columnsCount + 2.5f)
        val rowHeight = height / (rowsCount + 1f)
        var currentOffset = baseOffset

        for (row in 0 until rowsCount) {
            val yPos = (row + 1) * rowHeight

            // Rysowanie adresu wiersza
            val addressStr = String.format("%06X:", currentOffset)
            canvas.drawText(addressStr, 10f, yPos, addressPaint)

            for (col in 0 until columnsCount) {
                if (currentOffset + representationConfig.stepBytes > buf.capacity()) break

                val xPos = (col + 2.5f) * cellWidth
                val cellText = if (showPhysicalValues) {
                    DataFormatterEngine.formatAsDecimal(buf, currentOffset, representationConfig)
                } else {
                    DataFormatterEngine.formatAsHex(buf, currentOffset, representationConfig)
                }

                canvas.drawText(cellText, xPos, yPos, textPaint)
                currentOffset += representationConfig.stepBytes
            }
        }
    }
}