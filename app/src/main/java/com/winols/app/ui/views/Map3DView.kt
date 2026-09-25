package com.winols.app.ui.views

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.winols.app.model.EcuMapData

/**
 * Interaktywny widok 3D bazujący na GLSurfaceView z obsługą obrotu (Drag) i powiększenia (Pinch-to-zoom).
 */
class Map3DView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    private val renderer = Map3DSurfaceRenderer()
    private var previousX = 0f
    private var previousY = 0f

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            renderer.scale *= detector.scaleFactor
            renderer.scale = renderer.scale.coerceIn(0.4f, 3.5f)
            requestRender()
            return true
        }
    })

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun setMapData(data: EcuMapData) {
        renderer.updateData(data)
        requestRender()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        if (event.pointerCount == 1) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    previousX = event.x
                    previousY = event.y
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.x - previousX
                    val dy = event.y - previousY

                    renderer.rotationY += dx * 0.4f
                    renderer.rotationX += dy * 0.4f
                    renderer.rotationX = renderer.rotationX.coerceIn(-85f, 85f)

                    requestRender()
                    previousX = event.x
                    previousY = event.y
                }
            }
        }
        return true
    }
}