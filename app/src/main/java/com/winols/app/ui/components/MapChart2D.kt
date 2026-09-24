package com.winols.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winols.app.edit.MapEditor

@Composable
fun MapChart2D(
    editor: MapEditor,
    selectedRow: Int = 0,
    modifier: Modifier = Modifier
) {
    val mapDef = editor.mapDef
    val rows = mapDef.rows
    val cols = mapDef.cols

    // Pobranie danych dla wybranego wiersza lub wszystkich serii
    val values = remember(editor, selectedRow, mapDef) {
        val r = selectedRow.coerceIn(0, (rows - 1).coerceAtLeast(0))
        DoubleArray(cols) { c -> editor.getPhysicalValue(r, c) }
    }

    val origValues = remember(editor, selectedRow, mapDef) {
        val r = selectedRow.coerceIn(0, (rows - 1).coerceAtLeast(0))
        DoubleArray(cols) { c -> editor.getOriginalPhysicalValue(r, c) }
    }

    val xAxisValues = remember(editor, mapDef) {
        val axisVals = editor.getAxisPhysicalValues(isXAxis = true)
        if (axisVals.size == cols) axisVals else DoubleArray(cols) { it.toDouble() }
    }

    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF18181B))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Profil 2D - Wiersz $selectedRow (${mapDef.unit.ifBlank { "Wartość" }})",
                color = Color(0xFFF4F4F5),
                style = MaterialTheme.typography.labelMedium
            )
            selectedPointIndex?.let { idx ->
                if (idx in 0 until cols) {
                    Text(
                        text = "X: %.1f | Y: %.2f (Oryg: %.2f)".format(xAxisValues[idx], values[idx], origValues[idx]),
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(values) {
                    detectTapGestures { tapOffset ->
                        val paddingX = 40f
                        val widthUsable = size.width - 2 * paddingX
                        val stepX = widthUsable / (cols - 1).coerceAtLeast(1)
                        val relativeX = tapOffset.x - paddingX
                        val clickedIdx = (relativeX / stepX + 0.5f).toInt()
                        if (clickedIdx in 0 until cols) {
                            selectedPointIndex = clickedIdx
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            val padLeft = 40f
            val padRight = 40f
            val padTop = 30f
            val padBottom = 40f

            val chartW = w - padLeft - padRight
            val chartH = h - padTop - padBottom

            var minVal = Double.MAX_VALUE
            var maxVal = -Double.MAX_VALUE
            for (i in 0 until cols) {
                if (values[i] < minVal) minVal = values[i]
                if (values[i] > maxVal) maxVal = values[i]
                if (origValues[i] < minVal) minVal = origValues[i]
                if (origValues[i] > maxVal) maxVal = origValues[i]
            }

            if (minVal == maxVal) {
                minVal -= 1.0
                maxVal += 1.0
            }

            val rangeVal = (maxVal - minVal).toFloat()

            // Siatka pozioma i osie
            val gridLines = 4
            for (i in 0..gridLines) {
                val yNorm = i.toFloat() / gridLines
                val yPos = padTop + chartH * (1f - yNorm)
                drawLine(
                    color = Color(0xFF27272A),
                    start = Offset(padLeft, yPos),
                    end = Offset(w - padRight, yPos),
                    strokeWidth = 1f
                )
            }

            // Rysowanie serii oryginalnej (jeśli różni się od bieżącej)
            val origPath = Path()
            val currPath = Path()

            for (i in 0 until cols) {
                val x = padLeft + i * (chartW / (cols - 1).coerceAtLeast(1))
                val yOrig = padTop + chartH * (1f - ((origValues[i] - minVal).toFloat() / rangeVal))
                val yCurr = padTop + chartH * (1f - ((values[i] - minVal).toFloat() / rangeVal))

                if (i == 0) {
                    origPath.moveTo(x, yOrig)
                    currPath.moveTo(x, yCurr)
                } else {
                    origPath.lineTo(x, yOrig)
                    currPath.lineTo(x, yCurr)
                }
            }

            drawPath(
                path = origPath,
                color = Color(0xFF71717A),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            drawPath(
                path = currPath,
                color = Color(0xFF38BDF8),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Punkty (węzły)
            for (i in 0 until cols) {
                val x = padLeft + i * (chartW / (cols - 1).coerceAtLeast(1))
                val yCurr = padTop + chartH * (1f - ((values[i] - minVal).toFloat() / rangeVal))
                val isPointSelected = selectedPointIndex == i

                drawCircle(
                    color = if (isPointSelected) Color(0xFFF43F5E) else Color(0xFF0284C7),
                    radius = if (isPointSelected) 6.dp.toPx() else 3.5.dp.toPx(),
                    center = Offset(x, yCurr)
                )
            }
        }
    }
}