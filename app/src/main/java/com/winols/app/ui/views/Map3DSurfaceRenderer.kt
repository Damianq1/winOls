package com.winols.app.ui.views

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.winols.app.model.EcuMapData
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.max

class Map3DSurfaceRenderer : GLSurfaceView.Renderer {

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    var rotationX: Float = 45f
    var rotationY: Float = -45f
    var scale: Float = 1.0f

    @Volatile
    private var mapData: EcuMapData? = null
    @Volatile
    private var needsUpdate = false

    private var vertexBuffer: FloatBuffer? = null
    private var colorBuffer: FloatBuffer? = null
    private var indexBuffer: ShortBuffer? = null
    private var indexCount = 0

    private var program = 0

    private val vertexShaderCode = """
        uniform mat4 uMVPMatrix;
        attribute vec4 aPosition;
        attribute vec4 aColor;
        varying vec4 vColor;
        void main() {
            gl_Position = uMVPMatrix * aPosition;
            vColor = aColor;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        varying vec4 vColor;
        void main() {
            gl_FragColor = vColor;
        }
    """.trimIndent()

    fun updateData(data: EcuMapData) {
        this.mapData = data
        this.needsUpdate = true
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.12f, 0.12f, 0.14f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height.toFloat()
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 2f, 20f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        if (needsUpdate) {
            buildGeometry()
            needsUpdate = false
        }

        val indices = indexBuffer ?: return
        val vertices = vertexBuffer ?: return
        val colors = colorBuffer ?: return

        GLES20.glUseProgram(program)

        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 5.5f, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale)
        Matrix.rotateM(modelMatrix, 0, rotationX, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, rotationY, 0f, 1f, 0f)

        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        val mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        val positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertices)

        val colorHandle = GLES20.glGetAttribLocation(program, "aColor")
        GLES20.glEnableVertexAttribArray(colorHandle)
        GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, 16, colors)

        GLES20.glDrawElements(GLES20.GL_LINES, indexCount, GLES20.GL_UNSIGNED_SHORT, indices)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(colorHandle)
    }

    private fun buildGeometry() {
        val map = mapData ?: return
        val rows = map.rows
        val cols = map.cols
        val totalVertices = rows * cols

        val vertices = FloatArray(totalVertices * 3)
        val colors = FloatArray(totalVertices * 4)

        val minVal = map.minValue
        val maxVal = max(map.maxValue, minVal + 0.0001f)

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val idx = r * cols + c
                val v = map.getValue(r, c)
                val normV = ((v - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)

                // Normalizacja X, Y do zakresu [-1, 1], Z na podstawie wysokości wartości mapy
                vertices[idx * 3] = ((c.toFloat() / (cols - 1)) * 2f - 1f) * 1.5f
                vertices[idx * 3 + 1] = (normV * 2f - 1f) * 0.8f
                vertices[idx * 3 + 2] = ((r.toFloat() / (rows - 1)) * 2f - 1f) * 1.5f

                // Kolor wierzchołka: Gradient od chłodnego błękitu do jaskrawej czerwieni
                colors[idx * 4] = normV               // R
                colors[idx * 4 + 1] = 1f - normV      // G
                colors[idx * 4 + 2] = 0.2f            // B
                colors[idx * 4 + 3] = 1.0f            // Alpha
            }
        }

        // Linie siatki Wireframe dla osi X i Y
        val indices = ArrayList<Short>()
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val current = (r * cols + c).toShort()
                if (c + 1 < cols) {
                    indices.add(current)
                    indices.add((current + 1).toShort())
                }
                if (r + 1 < rows) {
                    indices.add(current)
                    indices.add((current + cols).toShort())
                }
            }
        }

        indexCount = indices.size
        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply { put(vertices); position(0) }
        }
        colorBuffer = ByteBuffer.allocateDirect(colors.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply { put(colors); position(0) }
        }
        val shortArr = ShortArray(indices.size) { indices[it] }
        indexBuffer = ByteBuffer.allocateDirect(shortArr.size * 2).run {
            order(ByteOrder.nativeOrder())
            asShortBuffer().apply { put(shortArr); position(0) }
        }
    }

    private fun loadShader(type: Int, code: String): Int {
        return GLES20.glCreateShader(type).also {
            GLES20.glShaderSource(it, code)
            GLES20.glCompileShader(it)
        }
    }
}