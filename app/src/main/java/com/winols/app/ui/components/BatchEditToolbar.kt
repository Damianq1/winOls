package com.winols.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winols.app.edit.BatchEditOperation
import com.winols.app.ui.CellDisplayMode
import com.winols.app.ui.SelectionRange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchEditToolbar(
    displayMode: CellDisplayMode,
    selectedRange: SelectionRange?,
    onApplyBatchEdit: (BatchEditOperation) -> Unit,
    onDisplayModeChange: (CellDisplayMode) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf<BatchDialogType?>(null) }
    var inputValue by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF202023),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .horizontalScroll(scrollState),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Undo / Redo
            IconButton(onClick = onUndo, enabled = canUndo) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Undo", tint = if (canUndo) Color.White else Color.Gray)
            }
            IconButton(onClick = onRedo, enabled = canRedo) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Redo", tint = if (canRedo) Color.White else Color.Gray)
            }

            VerticalDivider(modifier = Modifier.height(24.dp), color = Color(0xFF3F3F46))

            // Przełączanie trybu widoku
            AssistChip(
                onClick = {
                    val nextMode = when (displayMode) {
                        CellDisplayMode.PHYSICAL_VALUES -> CellDisplayMode.RAW_HEX
                        CellDisplayMode.RAW_HEX -> CellDisplayMode.RAW_DECIMAL
                        CellDisplayMode.RAW_DECIMAL -> CellDisplayMode.ABSOLUTE_DIFFERENCE
                        CellDisplayMode.ABSOLUTE_DIFFERENCE -> CellDisplayMode.PERCENTAGE_DIFFERENCE
                        CellDisplayMode.PERCENTAGE_DIFFERENCE -> CellDisplayMode.PHYSICAL_VALUES
                    }
                    onDisplayModeChange(nextMode)
                },
                label = {
                    Text(
                        text = displayMode.name.replace("_", " "),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            )

            VerticalDivider(modifier = Modifier.height(24.dp), color = Color(0xFF3F3F46))

            // Operacje masowej edycji (dostępne przy zaznaczeniu)
            val hasSelection = selectedRange != null

            FilledTonalButton(
                onClick = {
                    inputValue = "5.0"
                    showDialog = BatchDialogType.PERCENTAGE
                },
                enabled = hasSelection,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("% Zmiana", fontSize = 12.sp)
            }

            FilledTonalButton(
                onClick = {
                    inputValue = "10.0"
                    showDialog = BatchDialogType.ADD_OFFSET
                },
                enabled = hasSelection,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("+ / - Delta", fontSize = 12.sp)
            }

            FilledTonalButton(
                onClick = {
                    inputValue = "0.0"
                    showDialog = BatchDialogType.SET_EXACT
                },
                enabled = hasSelection,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("Stała", fontSize = 12.sp)
            }

            Button(
                onClick = {
                    onApplyBatchEdit(BatchEditOperation.Smooth(factor = 0.5))
                },
                enabled = hasSelection,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("Wygładź", fontSize = 12.sp)
            }

            Button(
                onClick = {
                    selectedRange?.let {
                        val minR = minOf(it.startRow, it.endRow)
                        val maxR = maxOf(it.startRow, it.endRow)
                        val minC = minOf(it.startCol, it.endCol)
                        val maxC = maxOf(it.startCol, it.endCol)
                        onApplyBatchEdit(BatchEditOperation.Interpolate2D(minR, minC, maxR, maxC))
                    }
                },
                enabled = hasSelection && selectedRange.startRow != selectedRange.endRow && selectedRange.startCol != selectedRange.endCol,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("Interp 2D", fontSize = 12.sp)
            }
        }
    }

    // Modal dialog wprowadzania wartości parametrów operacji
    showDialog?.let { dialogType ->
        AlertDialog(
            onDismissRequest = { showDialog = null },
            title = {
                Text(
                    text = when (dialogType) {
                        BatchDialogType.PERCENTAGE -> "Zmiana procentowa (%)"
                        BatchDialogType.ADD_OFFSET -> "Dodaj / Odejmij wartość"
                        BatchDialogType.SET_EXACT -> "Ustaw stałą wartość fizyczną"
                    },
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = inputValue,
                        onValueChange = { inputValue = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("Wartość") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parsed = inputValue.toDoubleOrNull()
                        if (parsed != null) {
                            when (dialogType) {
                                BatchDialogType.PERCENTAGE -> onApplyBatchEdit(BatchEditOperation.PercentageChange(parsed))
                                BatchDialogType.ADD_OFFSET -> onApplyBatchEdit(BatchEditOperation.AddOffset(parsed))
                                BatchDialogType.SET_EXACT -> onApplyBatchEdit(BatchEditOperation.SetExactValue(parsed))
                            }
                        }
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
    PERCENTAGE,
    ADD_OFFSET,
    SET_EXACT
}