package com.winols.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winols.app.edit.MapEditor
import com.winols.app.ui.CellDisplayMode
import com.winols.app.ui.SelectionRange

@Composable
fun MapTableGrid(
    editor: MapEditor,
    displayMode: CellDisplayMode,
    selectedRange: SelectionRange?,
    onCellSelected: (row: Int, col: Int, isMultiSelect: Boolean) -> Unit,
    onDragSelectionUpdate: (startRow: Int, startCol: Int, endRow: Int, endCol: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val mapDef = editor.mapDef
    val horizontalScrollState = rememberScrollState()

    val xAxisValues = remember(mapDef) { editor.getAxisPhysicalValues(isXAxis = true) }
    val yAxisValues = remember(mapDef) { editor.getAxisPhysicalValues(isXAxis = false) }

    val cellWidth = 72.dp
    val headerHeight = 32.dp
    val cellHeight = 36.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Nagłówek osi X
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2D2D2D))
                    .horizontalScroll(horizontalScrollState)
            ) {
                // Róg osi Y / X
                Box(
                    modifier = Modifier
                        .size(width = 80.dp, height = headerHeight)
                        .background(Color(0xFF252526))
                        .border(0.5.dp, Color(0xFF3F3F46)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Y \\ X",
                        color = Color(0xFFA1A1AA),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Etykiety kolumn X
                for (col in 0 until mapDef.cols) {
                    val label = if (col < xAxisValues.size) {
                        "%.1f".format(xAxisValues[col])
                    } else {
                        col.toString()
                    }
                    Box(
                        modifier = Modifier
                            .size(width = cellWidth, height = headerHeight)
                            .border(0.5.dp, Color(0xFF3F3F46))
                            .background(Color(0xFF252526)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = Color(0xFFE4E4E7),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Zawartość tabeli: wiersze osi Y + komórki mapy
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed((0 until mapDef.rows).toList()) { row, _ ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(horizontalScrollState)
                    ) {
                        // Etykieta wiersza Y
                        val rowLabel = if (row < yAxisValues.size) {
                            "%.1f".format(yAxisValues[row])
                        } else {
                            row.toString()
                        }
                        Box(
                            modifier = Modifier
                                .size(width = 80.dp, height = cellHeight)
                                .background(Color(0xFF252526))
                                .border(0.5.dp, Color(0xFF3F3F46)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = rowLabel,
                                color = Color(0xFFE4E4E7),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Komórki danych w danym wierszu
                        for (col in 0 until mapDef.cols) {
                            val isSelected = selectedRange?.contains(row, col) == true
                            val isModified = editor.isCellModified(row, col)

                            val displayText = when (displayMode) {
                                CellDisplayMode.PHYSICAL_VALUES -> {
                                    val phys = editor.getPhysicalValue(row, col)
                                    "%.${mapDef.formula.precision}f".format(phys)
                                }
                                CellDisplayMode.RAW_HEX -> {
                                    val raw = editor.getRawValue(row, col).toLong()
                                    when (mapDef.dataType.byteSize) {
                                        1 -> "%02X".format(raw and 0xFF)
                                        2 -> "%04X".format(raw and 0xFFFF)
                                        else -> "%08X".format(raw and 0xFFFFFFFFL)
                                    }
                                }
                                CellDisplayMode.RAW_DECIMAL -> {
                                    editor.getRawValue(row, col).toLong().toString()
                                }
                                CellDisplayMode.ABSOLUTE_DIFFERENCE -> {
                                    val delta = editor.getPhysicalDelta(row, col)
                                    (if (delta > 0) "+" else "") + "%.${mapDef.formula.precision}f".format(delta)
                                }
                                CellDisplayMode.PERCENTAGE_DIFFERENCE -> {
                                    val pct = editor.getPercentageDelta(row, col)
                                    (if (pct > 0) "+" else "") + "%.1f%%".format(pct)
                                }
                            }

                            val cellBg = when {
                                isSelected -> Color(0xFF005FB8)
                                isModified -> Color(0xFF7A1C1C)
                                else -> Color(0xFF18181B)
                            }

                            Box(
                                modifier = Modifier
                                    .size(width = cellWidth, height = cellHeight)
                                    .background(cellBg)
                                    .border(0.5.dp, if (isSelected) Color(0xFF60A5FA) else Color(0xFF27272A))
                                    .clickable {
                                        onCellSelected(row, col, false)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = displayText,
                                    color = if (isSelected) Color.White else if (isModified) Color(0xFFFCA5A5) else Color(0xFFD4D4D8),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}