package com.winols.app.viewmodel

import com.winols.app.engine.DataRepresentationEngine
import com.winols.app.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class RepresentationUiState(
    val activeViewMode: ViewMode = ViewMode.HEX_DUMP,
    val formatConfig: DataFormatConfig = DataFormatConfig(),
    val cursorOffset: Int = 0,
    val windowSize: Int = 1024,
    val gridRows: Int = 16,
    val gridCols: Int = 16,
    val points2D: FloatArray = FloatArray(0),
    val matrix3D: Array<DoubleArray> = emptyArray()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as RepresentationUiState
        return activeViewMode == other.activeViewMode &&
                formatConfig == other.formatConfig &&
                cursorOffset == other.cursorOffset &&
                windowSize == other.windowSize &&
                gridRows == other.gridRows &&
                gridCols == other.gridCols &&
                points2D.contentEquals(other.points2D) &&
                matrix3D.contentDeepEquals(other.matrix3D)
    }

    override fun hashCode(): Int {
        var result = activeViewMode.hashCode()
        result = 31 * result + formatConfig.hashCode()
        result = 31 * result + cursorOffset
        result = 31 * result + windowSize
        result = 31 * result + gridRows
        result = 31 * result + gridCols
        result = 31 * result + points2D.contentHashCode()
        result = 31 * result + matrix3D.contentDeepHashCode()
        return result
    }
}

class RepresentationViewModel(
    private val engine: DataRepresentationEngine = DataRepresentationEngine()
) {
    private val _uiState = MutableStateFlow(RepresentationUiState())
    val uiState: StateFlow<RepresentationUiState> = _uiState.asStateFlow()

    private var rawBuffer: ByteArray = ByteArray(0)

    fun loadBuffer(buffer: ByteArray) {
        this.rawBuffer = buffer
        refreshData()
    }

    fun switchViewMode(mode: ViewMode) {
        _uiState.update { it.copy(activeViewMode = mode) }
        refreshData()
    }

    fun setBitWidth(bitWidth: BitWidth) {
        _uiState.update { it.copy(formatConfig = it.formatConfig.copy(bitWidth = bitWidth)) }
        refreshData()
    }

    fun toggleSignedness() {
        val next = if (_uiState.value.formatConfig.signedness == Signedness.SIGNED) {
            Signedness.UNSIGNED
        } else {
            Signedness.SIGNED
        }
        _uiState.update { it.copy(formatConfig = it.formatConfig.copy(signedness = next)) }
        refreshData()
    }

    fun toggleEndianness() {
        val next = if (_uiState.value.formatConfig.endianness == Endianness.HI_LO) {
            Endianness.LO_HI
        } else {
            Endianness.HI_LO
        }
        _uiState.update { it.copy(formatConfig = it.formatConfig.copy(endianness = next)) }
        refreshData()
    }

    fun setMatrixDimensions(rows: Int, cols: Int) {
        _uiState.update { it.copy(gridRows = rows, gridCols = cols) }
        refreshData()
    }

    fun setOffset(offset: Int) {
        _uiState.update { it.copy(cursorOffset = offset.coerceIn(0, (rawBuffer.size - 1).coerceAtLeast(0))) }
        refreshData()
    }

    private fun refreshData() {
        if (rawBuffer.isEmpty()) return

        val state = _uiState.value
        when (state.activeViewMode) {
            ViewMode.HEX_DUMP -> {
                // HEX Dump czyta komórki bezpośrednio via engine.renderHexCell() na żądanie widoku
            }
            ViewMode.GRAPH_2D -> {
                val pts = engine.extractWaveformData2D(
                    rawBuffer,
                    state.cursorOffset,
                    state.windowSize,
                    state.formatConfig
                )
                _uiState.update { it.copy(points2D = pts) }
            }
            ViewMode.SURFACE_3D -> {
                val mat = engine.extractMatrix3D(
                    rawBuffer,
                    state.cursorOffset,
                    state.gridRows,
                    state.gridCols,
                    state.formatConfig
                )
                _uiState.update { it.copy(matrix3D = mat) }
            }
        }
    }
}