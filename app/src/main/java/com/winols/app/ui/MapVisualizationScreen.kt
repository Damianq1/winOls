package com.winols.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.winols.app.edit.MapEditor
import com.winols.app.ui.components.MapChart2D
import com.winols.app.ui.components.MapChart3D

enum class ChartDisplayType {
    SPLIT_VIEW,
    ONLY_2D,
    ONLY_3D
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapVisualizationScreen(
    editor: MapEditor,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var displayType by remember { mutableStateOf(ChartDisplayType.SPLIT_VIEW) }
    var selectedRowIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("${editor.mapDef.name} - Wizualizacja") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Wstecz")
                    }
                },
                actions = {
                    Row(modifier = Modifier.padding(end = 8.dp)) {
                        FilterChip(
                            selected = displayType == ChartDisplayType.ONLY_2D,
                            onClick = { displayType = ChartDisplayType.ONLY_2D },
                            label = { Text("2D") }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        FilterChip(
                            selected = displayType == ChartDisplayType.ONLY_3D,
                            onClick = { displayType = ChartDisplayType.ONLY_3D },
                            label = { Text("3D") }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        FilterChip(
                            selected = displayType == ChartDisplayType.SPLIT_VIEW,
                            onClick = { displayType = ChartDisplayType.SPLIT_VIEW },
                            label = { Text("Podzielony") }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1E24),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF121212))
        ) {
            when (displayType) {
                ChartDisplayType.ONLY_2D -> {
                    MapChart2D(
                        editor = editor,
                        selectedRow = selectedRowIndex,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                ChartDisplayType.ONLY_3D -> {
                    MapChart3D(
                        editor = editor,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                ChartDisplayType.SPLIT_VIEW -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        MapChart2D(
                            editor = editor,
                            selectedRow = selectedRowIndex,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                        HorizontalDivider(color = Color(0xFF27272A), thickness = 1.dp)
                        MapChart3D(
                            editor = editor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }
            }
        }
    }
}