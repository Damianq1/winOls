package com.winols.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.winols.app.edit.MapEditor
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MapChart3D(
    editor: MapEditor,
    modifier: Modifier = Modifier
) {
    val mapDef = editor.mapDef
    val rows = mapDef.rows
    val cols = mapDef.cols

    var rotX by remember { mutableFloatStateOf(35f) }
    var rotY by remember { mutableFloatStateOf(45f) }
    var zoom by remember { mutableFloatStateOf(1.0f) }

    // Wyciągnięcie znormalizowanej macierzy wysokości Z
    val matrix = remember(editor, mapDef) {
        Array(rows) { r ->
            DoubleArray(cols) { c -> editor.getPhysicalValue(r, c) }
        }
    }

    var minVal = Double.MAX_VALUE
    var maxVal = -Double.MAX_VALUE
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            val v = matrix[r][c]
            if (v < minVal) minVal = v
            if (v > maxVal) maxVal = v
        }
    }
    if (minVal == maxVal) {
        minVal -= 1.0
        maxVal += 1.0
    }
    val valueRange = (maxVal - minVal).toFloat()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF141416))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Siatka powierzchniowa 3D - ${mapDef.name}",
                color = Color(0xFFF4F4F5),
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = "Kąt: ${rotX.toInt()}° / ${rotY.toInt()}° | Zoom: %.1fx".format(zoom),
                color = Color(0xFFA1A1AA),
                style = MaterialTheme.typography.labelSmall
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        rotY = (rotY + pan.x * 0.4f) % 360f
                        rotX = (rotX - pan.y * 0.4f).coerceIn(5f, 85f)
                        zoom = (zoom * gestureZoom).coerceIn(0.5f, 3.0f)
                    }
                }
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f

            val radX = Math.toRadians(rotX.toDouble())
            val radY = Math.toRadians(rotY.toDouble())

            val cosX = cos(radX).toFloat()
            val sinX = sin(radX).toFloat()
            val cosY = cos(radY).toFloat()
            val sinY = sin(radY).toFloat()

            val spanX = size.width * 0.55f * zoom
            val spanY = size.height * 0.55f * zoom
            val heightScale = 140f * zoom

            fun project(row: Int, col: Int): Offset {
                // Wyśrodkowane współrzędne [-0.5, 0.5]
                val nx = (col.toFloat() / (cols - 1).coerceAtLeast(1)) - 0.5f
                val ny = (row.toFloat() / (rows - 1).coerceAtLeast(1)) - 0.5f
                val nz = ((matrix[row][col] - minVal).toFloat() / valueRange) - 0.5f

                val worldX = nx * spanX
                val worldY = ny * spanY
                val worldZ = nz * heightScale

                // Obrót wokół osi Z (yaw / rotY)
                val x1 = worldX * cosY - worldY * sinY
                val y1 = worldX * sinY + worldY * cosY
                val z1 = worldZ

                // Obrót wokół osi X (pitch / rotX)
                val x2 = x1
                val y2 = y1 * cosX - z1 * sinX
                val z2 = y1 * sinX + z1 * cosX

                // Rzut ortogonalny z przesunięciem do środka
                return Offset(cx + x2, cy + y2)
            }

            // Rysowanie siatki wzdłuż kolumn i wierszy z cieniowaniem wysokości
            for (r in 0 until rows) {
                val rowPath = Path()
                for (c in 0 until cols) {
                    val pt = project(r, c)
                    if (c == 0) rowPath.moveTo(pt.x, pt.y) else rowPath.lineTo(pt.x, pt.y)
                }
                drawPath(
                    path = rowPath,
                    color = Color(0xFF3B82F6).copy(alpha = 0.75f),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            for (c in 0 until cols) {
                val colPath = Path()
                for (r in 0 until rows) {
                    val pt = project(r, c)
                    if (r == 0) colPath.moveTo(pt.x, pt.y) else colPath.lineTo(pt.x, pt.y)
                }
                drawPath(
                    path = colPath,
                    color = Color(0xFF10B981).copy(alpha = 0.75f),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // Wierzchołki z gradientem wysokości od błękitu do czerwieni
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val normVal = ((matrix[r][c] - minVal) / valueRange).toFloat().coerceIn(0f, 1f)
                    val pt = project(r, c)

                    // Kolor termowizyjny (niebieski -> zielony -> czerwony)
                    val vertexColor = when {
                        normVal < 0.5f -> {
                            val factor = normVal * 2f
                            Color(
                                red = 0.1f * (1f - factor),
                                green = 0.6f * factor,
                                blue = 1f * (1f - factor) + 0.3f * factor,
                                alpha = 1f
                            )
                        }
                        else -> {
                            val factor = (normVal - 0.5f) * 2f
                            Color(
                                red = 0.2f * (1f - factor) + 1f * factor,
                                green = 0.8f * (1f - factor) + 0.15f * factor,
                                blue = 0.2f * (1f - factor),
                                alpha = 1f
                            )
                        }
                    }

                    drawCircle(
                        color = vertexColor,
                        radius = 2.5.dp.toPx(),
                        center = pt
                    )
                }
            }
        }
    }
}