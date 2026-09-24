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
import com.winols.app.model.DataDisplayConfig
import com.winols.app.model.Endianness
import com.winols.app.model.MapDefinition
import com.winols.app.model.NumberBase
import com.winols.app.model.WordWidth
import java.util.Locale

class EcuGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var binModel: BinModel? = null
    private var activeMap: MapDefinition? = null

    // Konfiguracja reprezentacji danych (HEX / DEC / Little / Big Endian)
    var displayConfig: DataDisplayConfig = DataDisplayConfig(
        base = NumberBase.HEX,
        wordWidth = WordWidth.WORD_16,
        endianness = Endianness.LITTLE_ENDIAN,
        applyFormulas = false
    )
        set(value) {
            field = value
            invalidate()
        }

    private var cellWidth = 150f
    private var cellHeight = 70f
    private var scaleFactor = 1.0f
    private var scrollXOffset = 0f
    private var scrollYOffset = 0f

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    private val modifiedTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF5252") // Czerwony dla wartości zmienionych względem ORI
        textSize = 28f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF50")
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }

    private val gridLinePaint = Paint().apply {
        color = Color.parseColor("#333333")
        strokeWidth = 1.5f
    }

    private val selectionPaint = Paint().apply {
        color = Color.parseColor("#442196F3")
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

    fun toggleHexDec() {
        displayConfig = when (displayConfig.base) {
            NumberBase.HEX -> displayConfig.copy(base = NumberBase.DECIMAL_UNSIGNED)
            NumberBase.DECIMAL_UNSIGNED -> displayConfig.copy(base = NumberBase.DECIMAL_SIGNED)
            NumberBase.DECIMAL_SIGNED -> displayConfig.copy(base = NumberBase.HEX)
        }
    }

    fun toggleEndianness() {
        displayConfig = displayConfig.copy(
            endianness = if (displayConfig.endianness == Endianness.LITTLE_ENDIAN) {
                Endianness.BIG_ENDIAN
            } else {
                Endianness.LITTLE_ENDIAN
            }
        )
    }

    fun toggleBitWidth() {
        displayConfig = when (displayConfig.wordWidth) {
            WordWidth.BYTE_8 -> displayConfig.copy(wordWidth = WordWidth.WORD_16)
            WordWidth.WORD_16 -> displayConfig.copy(wordWidth = WordWidth.DWORD_32)
            WordWidth.DWORD_32 -> displayConfig.copy(wordWidth = WordWidth.BYTE_8)
        }
    }

    fun toggleEngineeringFormula() {
        displayConfig = displayConfig.copy(applyFormulas = !displayConfig.applyFormulas)
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

        val activeType = displayConfig.toDataType()
        val step = activeType.byteSize

        // Etykieta lewego górnego rogu (informacja o trybie)
        val modeLabel = "${displayConfig.base.name.take(3)} ${displayConfig.wordWidth.byteSize * 8}b ${if (displayConfig.endianness == Endianness.LITTLE_ENDIAN) "LoHi" else "HiLo"}"
        canvas.drawText(modeLabel, sW / 2, sH / 2 + 8, headerPaint)
        canvas.drawRect(0f, 0f, sW, sH, gridLinePaint)

        // Nagłówek osi X
        for (c in 0 until map.columns) {
            val left = (c + 1) * sW
            val rect = Rect(left.toInt(), 0, (left + sW).toInt(), sH.toInt())
            canvas.drawRect(rect, gridLinePaint)
            val label = map.xAxis?.manualValues?.getOrNull(c)?.toString() ?: "${c + 1}"
            canvas.drawText(label, left + sW / 2, sH / 2 + 8, headerPaint)
        }

        // Komórki mapy
        for (r in 0 until map.rows) {
            val top = (r + 1) * sH
            val yLabel = map.yAxis?.manualValues?.getOrNull(r)?.toString() ?: "${r + 1}"
            canvas.drawRect(0f, top, sW, top + sH, gridLinePaint)
            canvas.drawText(yLabel, sW / 2, top + sH / 2 + 8, headerPaint)

            for (c in 0 until map.columns) {
                val left = (c + 1) * sW
                val isSelected = selectedCell?.first == r && selectedCell?.second == c

                if (isSelected) {
                    canvas.drawRect(left, top, left + sW, top + sH, selectionPaint)
                }

                val cellOffset = (r * map.columns + c) * step
                val cellAddress = map.startAddress + cellOffset

                val rawValue = model.bufferManager.readValue(cellAddress, activeType)
                val engValue = (rawValue * map.factor) + map.offset

                val text = displayConfig.formatValue(rawValue, engValue)
                val isModified = model.bufferManager.isModifiedAt(cellAddress)

                val paintToUse = if (isModified) modifiedTextPaint else textPaint

                canvas.drawRect(left, top, left + sW, top + sH, gridLinePaint)
                canvas.drawText(text, left + sW / 2, top + sH / 2 + 10, paintToUse)
            }
        }

        canvas.restore()
    }
}