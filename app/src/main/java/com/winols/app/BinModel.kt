package com.winols.app

import com.winols.app.data.AsyncBinaryOperations
import com.winols.app.data.BinaryBufferManager
import com.winols.app.engine.AsyncComputeEngine
import com.winols.app.engine.ChecksumFamily
import com.winols.app.engine.ChecksumResult
import com.winols.app.model.MapDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

sealed class BinLoadingState {
    object Idle : BinLoadingState()
    object Loading : BinLoadingState()
    data class Success(val byteCount: Int, val mapCount: Int) : BinLoadingState()
    data class Error(val message: String) : BinLoadingState()
}

class BinModel(
    val bufferManager: BinaryBufferManager = BinaryBufferManager()
) {
    val asyncIO = AsyncBinaryOperations(bufferManager)
    val asyncCompute = AsyncComputeEngine()

    private val _mapsFlow = MutableStateFlow<List<MapDefinition>>(emptyList())
    val mapsFlow: StateFlow<List<MapDefinition>> = _mapsFlow.asStateFlow()

    private val _loadingState = MutableStateFlow<BinLoadingState>(BinLoadingState.Idle)
    val loadingState: StateFlow<BinLoadingState> = _loadingState.asStateFlow()

    val maps: List<MapDefinition>
        get() = _mapsFlow.value

    suspend fun loadFileAsync(file: File) {
        _loadingState.value = BinLoadingState.Loading
        val loadResult = asyncIO.loadFileAsync(file)

        loadResult.onSuccess { size ->
            val detected = asyncCompute.scanMapsAsync(bufferManager.rawBuffer)
            _mapsFlow.value = detected
            _loadingState.value = BinLoadingState.Success(size, detected.size)
        }.onFailure { err ->
            _loadingState.value = BinLoadingState.Error(err.localizedMessage ?: "Unknown load error")
        }
    }

    suspend fun loadStreamAsync(stream: InputStream) {
        _loadingState.value = BinLoadingState.Loading
        val loadResult = asyncIO.loadStreamAsync(stream)

        loadResult.onSuccess { size ->
            val detected = asyncCompute.scanMapsAsync(bufferManager.rawBuffer)
            _mapsFlow.value = detected
            _loadingState.value = BinLoadingState.Success(size, detected.size)
        }.onFailure { err ->
            _loadingState.value = BinLoadingState.Error(err.localizedMessage ?: "Unknown stream read error")
        }
    }

    suspend fun refreshMapDetectionAsync() = withContext(Dispatchers.Default) {
        if (bufferManager.size > 0) {
            val detected = asyncCompute.scanMapsAsync(bufferManager.rawBuffer)
            _mapsFlow.value = detected
        }
    }

    fun getCellValue(map: MapDefinition, row: Int, col: Int): Double {
        if (row !in 0 until map.rows || col !in 0 until map.columns) return 0.0
        val cellOffset = (row * map.columns + col) * map.dataType.byteSize
        val targetAddr = map.startAddress + cellOffset
        val raw = bufferManager.readValue(targetAddr, map.dataType)
        return (raw * map.factor) + map.offset
    }

    fun setCellValue(map: MapDefinition, row: Int, col: Int, engineeringValue: Double) {
        if (row !in 0 until map.rows || col !in 0 until map.columns) return
        val cellOffset = (row * map.columns + col) * map.dataType.byteSize
        val targetAddr = map.startAddress + cellOffset
        val raw = if (map.factor != 0.0) {
            (engineeringValue - map.offset) / map.factor
        } else {
            0.0
        }
        bufferManager.writeValue(targetAddr, map.dataType, raw)
    }

    fun applyPercentageChange(map: MapDefinition, row: Int, col: Int, percentDelta: Double) {
        val currentVal = getCellValue(map, row, col)
        val newVal = currentVal * (1.0 + (percentDelta / 100.0))
        setCellValue(map, row, col, newVal)
    }

    suspend fun verifyChecksumAsync(start: Int, end: Int, loc: Int, family: ChecksumFamily): ChecksumResult {
        return asyncCompute.verifyChecksumAsync(bufferManager.rawBuffer, start, end, loc, family)
    }

    suspend fun correctChecksumAsync(start: Int, end: Int, loc: Int, family: ChecksumFamily): Boolean {
        return asyncCompute.patchChecksumAsync(bufferManager.rawBuffer, start, end, loc, family)
    }
}