package com.winols.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.winols.app.edit.BatchEditOperation
import com.winols.app.edit.MapEditor
import com.winols.app.ui.components.BatchEditToolbar
import com.winols.app.ui.components.MapTableGrid

@Composable
fun MapEditorScreen(
    editor: MapEditor,
    onSaveRequested: () -> Unit,
    onCloseRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    var displayMode by remember { mutableStateOf(CellDisplayMode.PHYSICAL_VALUES) }
    var selectedRange by remember { mutableStateOf<SelectionRange?>(null) }
    var revisionCounter by remember { mutableIntStateOf(0) }

    fun executeOperation(op: BatchEditOperation) {
        val cells = selectedRange?.toCellList()
        when (op) {
            is BatchEditOperation.AddOffset -> editor.applyAbsoluteOffset(op.offset, cells)
            is BatchEditOperation.SubtractOffset -> editor.applyAbsoluteOffset(-op.offset, cells)
            is BatchEditOperation.PercentageChange -> editor.applyPercentageChange(op.percent, cells)
            is BatchEditOperation.SetExactValue -> editor.setExactPhysicalValue(op.value, cells)
            is BatchEditOperation.Smooth -> editor.smooth(op.factor, cells)
            is BatchEditOperation.InterpolateHorizontal -> editor.interpolateLinearHorizontal(op.row, op.startCol, op.endCol)
            is BatchEditOperation.InterpolateVertical -> editor.interpolateLinearVertical(op.col, op.startRow, op.endRow)
            is BatchEditOperation.Interpolate2D -> editor.interpolateBilinear2D(op.rStart, op.cStart, op.rEnd, op.cEnd)
        }
        revisionCounter++
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BatchEditToolbar(
                displayMode = displayMode,
                selectedRange = selectedRange,
                onApplyBatchEdit = { executeOperation(it) },
                onDisplayModeChange = { displayMode = it },
                onUndo = {
                    // Odwołanie przez bufor
                    revisionCounter++
                },
                onRedo = {
                    revisionCounter++
                },
                canUndo = true,
                canRedo = true
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF121212))
        ) {
            // Informacja o aktualnej mapie
            Surface(
                color = Color(0xFF1E1E24),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${editor.mapDef.name} [${editor.mapDef.rows}x${editor.mapDef.cols}] @ 0x${editor.mapDef.dataAddress.toString(16).uppercase()}",
                        color = Color(0xFFF4F4F5),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = "Jednostka: ${editor.mapDef.unit.ifBlank { "-" }}",
                        color = Color(0xFFA1A1AA),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            key(revisionCounter) {
                MapTableGrid(
                    editor = editor,
                    displayMode = displayMode,
                    selectedRange = selectedRange,
                    onCellSelected = { row, col, isMultiSelect ->
                        selectedRange = if (!isMultiSelect || selectedRange == null) {
                            SelectionRange(row, col, row, col)
                        } else {
                            selectedRange!!.copy(endRow = row, endCol = col)
                        }
                    },
                    onDragSelectionUpdate = { startR, startC, endR, endC ->
                        selectedRange = SelectionRange(startR, startC, endR, endC)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}