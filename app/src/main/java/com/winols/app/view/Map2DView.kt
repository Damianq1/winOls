package com.winols.app.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.winols.app.model.EcuMap

class Map2DView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var ecuMap: EcuMap? = null

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        textSize = 24f
    }

    private val linePaints = listOf(
        Color.parseColor("#448AFF"),
        Color.parseColor("#00E676"),
        Color.parseColor("#FFD600"),
        Color.parseColor("#FF5252")
    ).map { colorInt ->
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorInt
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }
    }

    fun setMap(map: EcuMap) {
        this.ecuMap = map
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val map = ecuMap ?: return
        if (map.columns < 2 || map.rows < 1) return

        val padLeft = 70f
        val padBottom = 60f
        val padTop = 30f
        val padRight = 30f

        val drawWidth = width - padLeft - padRight
        val drawHeight = height - padTop - padBottom

        // Osie
        canvas.drawRect(padLeft, padTop, width - padRight, height - padBottomAby zrealizować podgląd map ECU (dwuwymiarowy widok liniowy/konturowy 2D oraz dynamiczną siatkę przestrzenną 3D z obsługą obracania i skalowania dotykiem), dodamy model danych mapy, widok 2D rysowany na `Canvas` oraz wydajny widok 3D bazujący na rzutowaniu izometrycznym/perspektywicznym w czasie rzeczywistym.

---

### app/src/main/java/com/winols/app/model/EcuMap.kt