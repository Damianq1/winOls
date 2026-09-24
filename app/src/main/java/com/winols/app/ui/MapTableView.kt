package com.winols.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winols.app.edit.CellSelection
import com.winols.app.edit.MapBatchOperation
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapTableView(
    xAxisHeaders: List<String>,
    yAxisHeaders: List<String>,
    gridData: Array<DoubleArray>,
    selection: CellSelection?,
    onSelectionChange: (CellSelection) -> Unit,
    onApplyBatchOperation: (MapBatchOperation) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf<BatchDialogType?>(null) }
    var inputValue by remember { mutableStateOf("") }

    val rows = gridData.size
    val cols = if (rows > 0) gridData[0].size else 0

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        // Toolbar akcji masowych
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { inputValue = "0.0"; showDialog = BatchDialogType.ADD_OFFSET },
                enabled = selection != null
            ) {
                Text("+ / - Wartość")
            }
            Button(
                onClick = { inputValue = "5.0"; showDialog = BatchDialogType.PERCENT },
                enabled = selection != null
            ) {
                Text("% Skalowanie")
            }
            Button(
                onClick = { inputValue = "0.0"; showDialog = BatchDialogType.SET_CONSTANT },
                enabled = selection != null
            ) {
                Text("Stała wartość")
            }
            FilledTonalButton(
                onClick = { onApplyBatchOperation(MapBatchOperation.SmoothSelection(passes = 1)) },
                enabled = selection != null
            ) {
                Text("Wygładź (Smooth)")
            }
        }

        // Tabela z komórkami
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            item {
                Row {
                    // Narożnik Y/X
                    Box(
                        modifier = Modifier
                            .size(width = 80.dp, height = 36.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(0.5.dp, MaterialTheme.colorScheme.outline),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Y \\ X", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // Nagłówki osi X
                    xAxisHeaders.forEach { xHeader ->
                        Box(
                            modifier = Modifier
                                .size(width = 80.dp, height = 36.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(0.5.dp, MaterialTheme.colorScheme.outline),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(xHeader, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            items(rows) { rowIndex ->
                Row {
                    // Nagłówek osi Y
                    Box(
                        modifier = Modifier
                            .size(width = 80.dp, height = 36.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(0.5.dp, MaterialTheme.colorScheme.outline),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = yAxisHeaders.getOrElse(rowIndex) { rowIndex.toString() },
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    // Komórki danych w wierszu
                    for (colIndex in 0 until cols) {
                        val isSelected = selection?.contains(rowIndex, colIndex) == true
                        val cellValue = gridData[rowIndex][colIndex]

                        Box(
                            modifier = Modifier
                                .size(width = 80.dp, height = 36.dp)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                )
                                .clickable {
                                    onSelectionChange(CellSelection(rowIndex, colIndex, rowIndex, colIndex))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.2f", cellValue),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }

    // Modalne dialogi wprowadzania parametrów dla akcji masowych
    showDialog?.let { type ->
        AlertDialog(
            onDismissRequest = { showDialog = null },
            title = {
                Text(
                    when (type) {
                        BatchDialogType.ADD_OFFSET -> "Dodaj / Odejmij wartość"
                        BatchDialogType.PERCENT -> "Zmiana procentowa"
                        BatchDialogType.SET_CONSTANT -> "Wklej stałą wartość"
                    }
                )
            },
            text = {
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    label = { Text("Wartość") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val num = inputValue.toDoubleOrNull() ?: 0.0
                        val op = when (type) {
                            BatchDialogType.ADD_OFFSET -> MapBatchOperation.AddOffset(num)
                            BatchDialogType.PERCENT -> MapBatchOperation.MultiplyPercentage(num)
                            BatchDialogType.SET_CONSTANT -> MapBatchOperation.SetConstant(num)
                        }
                        onApplyBatchOperation(op)
                        showDialog = null
                    }
                ) {
                    Text("Zastosuj")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = null }) {
                    Text("Anuluj")
                }
            }
        )
    }
}

private enum class BatchDialogType {
    ADD_OFFSET,
    PERCENT,
    SET_CONSTANT
}