package com.winols.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winols.app.edit.Table3DEditor
import com.winols.app.model.Table3DModel
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Table3DScreen(
    initialTable: Table3DModel,
    onTableUpdated: (Table3DModel) -> Unit
) {
    var table by remember { mutableStateOf(initialTable) }
    var selectedCells by remember { mutableStateOf(setOf<Pair<Int, Int>>()) }
    var editModePercent by remember { mutableStateOf(false) }
    var modValueInput by remember { mutableStateOf("") }
    var showSurfacePreview by remember { mutableStateOf(true) }

    val editor = remember { Table3DEditor() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .padding(12.dp)
    ) {
        // Górny pasek narzędziowy tabeli
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${table.name} (0x${table.startAddress.toString(16).uppercase()})",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { showSurfacePreview = !showSurfacePreview },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
            ) {
                Text(if (showSurfacePreview) "Ukryj podgląd 3D" else "Pokaż siatkę 3D", color = Color.White)
            }
        }

        // Kontrolki edycji selekcji
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = modValueInput,
                onValueChange = { modValueInput = it },
                label = { Text("Wartość") },
                singleLine = true,
                modifier = Modifier.width(130.dp),
                textStyle = TextStyle(color = Color.White, fontSize = 13.sp)
            )

            Button(
                onClick = {
                    val v = modValueInput.toDoubleOrNull() ?: return@Button
                    table = editor.applyAbsolute(table, selectedCells, v)
                    onTableUpdated(table)
                }
            ) { Text("Ustaw") }

            Button(
                onClick = {
                    val p = modValueInput.toDoubleOrNull() ?: return@Button
                    table = editor.applyRelativePercent(table, selectedCells, p)
                    onTableUpdated(table)
                }
            ) { Text("+/- %") }

            Button(
                onClick = {
                    val d = modValueInput.toDoubleOrNull() ?: return@Button
                    table = editor.applyOffset(table, selectedCells, d)
                    onTableUpdated(table)
                }
            ) { Text("+/- Offset") }
        }

        // Widok split: Tabela komórek oraz opcjonalna projekcja izometryczna 3D
        Row(modifier = Modifier.fillMaxSize().weight(1f)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFF252526))
                    .border(1.dp, Color(0xFF3C3C3C))
            ) {
                Table3DMatrix(
                    table = table,
                    selectedCells = selectedCells,
                    onCellClick = { r, c ->
                        selectedCells = if (selectedCells.contains(r to c)) {
                            selectedCells - (r to c)
                        } else {
                            selectedCells + (r to c)
                        }
                    }
                )
            }

            if (showSurfacePreview) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFF181818))
                        .border(1.dp, Color(0xFF3C3C3C))
                ) {
                    Table3DSurfaceProjection(table = table)
                }
            }
        }
    }
}

@Composable
fun Table3DMatrix(
    table: Table3DModel,
    selectedCells: Set<Pair<Int, Int>>,
    onCellClick: (Int, Int) -> Unit
) {
    val hScroll = rememberScrollState()
    val vScroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(vScroll)
            .horizontalScroll(hScroll)
            .padding(4.dp)
    ) {
        // Nagłówek osi X
        Row {
            Box(
                modifier = Modifier
                    .size(width = 70.dp, height = 26.dp)
                    .background(Color(0xFF333333)),
                contentAlignment = Alignment.Center
            ) {
                Text("Y \\ X", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            for (col in 0 until table.xSize) {
                Box(
                    modifier = Modifier
                        .size(width = 64.dp, height = 26.dp)
                        .background(Color(0xFF2D2D30))
                        .border(0.5.dp, Color(0xFF3E3E42)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f", table.xAxis.getDisplayValue(col)),
                        color = Color(0xFF4EC9B0),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Wiersze (Oś Y + Macierz Z)
        for (row in 0 until table.ySize) {
            Row {
                Box(
                    modifier = Modifier
                        .size(width = 70.dp, height = 26.dp)
                        .background(Color(0xFF2D2D30))
                        .border(0.5.dp, Color(0xFF3E3E42)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f", table.yAxis.getDisplayValue(row)),
                        color = Color(0xFFCE9178),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                for (col in 0 until table.xSize) {
                    val isSelected = selectedCells.contains(row to col)
                    val value = table.getDisplayZ(row, col)

                    Box(
                        modifier = Modifier
                            .size(width = 64.dp, height = 26.dp)
                            .background(if (isSelected) Color(0xFF094771) else Color(0xFF1E1E1E))
                            .border(0.5.dp, if (isSelected) Color(0xFF007ACC) else Color(0xFF2D2D30))
                            .clickable { onCellClick(row, col) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1f", value),
                            color = if (isSelected) Color.White else Color(0xFFDCDCDC),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Projekcja siatki izometrycznej 3D na Canvas 2D
 */
@Composable
fun Table3DSurfaceProjection(table: Table3DModel) {
    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val w = size.width
        val h = size.height
        if (table.xSize < 2 || table.ySize < 2) return@Canvas

        var minZ = Double.MAX_VALUE
        var maxZ = Double.MIN_VALUE
        for (r in 0 until table.ySize) {
            for (c in 0 until table.xSize) {
                val z = table.getDisplayZ(r, c)
                if (z < minZ) minZ = z
                if (z > maxZ) maxZ = z
            }
        }
        val zRange = if (maxZ > minZ) maxZ - minZ else 1.0

        val isoAngle = Math.toRadians(30.0)
        val cosA = cos(isoAngle).toFloat()
        val sinA = sin(isoAngle).toFloat()

        val originX = w * 0.5f
        val originY = h * 0.75f
        val stepX = (w * 0.4f) / table.xSize
        val stepY = (h * 0.35f) / table.ySize
        val zScale = (h * 0.35f) / zRange.toFloat()

        fun project(r: Int, c: Int): Offset {
            val zVal = table.getDisplayZ(r, c)
            val zNorm = (zVal - minZ).toFloat()

            val xIso = (c - r) * stepX * cosA
            val yIso = (c + r) * stepY * sinA - (zNorm * zScale)
            return Offset(originX + xIso, originY + yIso)
        }

        // Rysowanie siatki wzdłuż osi X i Y
        for (r in 0 until table.ySize) {
            val path = Path()
            for (c in 0 until table.xSize) {
                val pt = project(r, c)
                if (c == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
            }
            drawPath(path, color = Color(0xFF007ACC), style = Stroke(width = 1.5f))
        }

        for (c in 0 until table.xSize) {
            val path = Path()
            for (r in 0 until table.ySize) {
                val pt = project(r, c)
                if (r == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
            }
            drawPath(path, color = Color(0xFF4EC9B0), style = Stroke(width = 1.5f))
        }
    }
}