package com.winols.app

import com.winols.app.data.BinaryBufferManager
import com.winols.app.engine.ChecksumEngine
import com.winols.app.engine.MapFinderEngine
import com.winols.app.model.MapDefinition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class BinModel(
    private val bufferManager: BinaryBufferManager = BinaryBufferManager(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {
    val mapFinderEngine = MapFinderEngine(bufferManager)
    val checksumEngine = ChecksumEngine(bufferManager)

    fun loadBinary(
        file: File,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        scope.launch {
            val result = bufferManager.loadFile(file)
            result.fold(
                onSuccess = { onSuccess() },
                onFailure = { onError(it) }
            )
        }
    }

    fun saveBinary(
        file: File,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        scope.launch {
            val result = bufferManager.saveToFile(file)
            result.fold(
                onSuccess = { onSuccess() },
                onFailure = { onError(it) }
            )
        }
    }

    fun scanMaps(
        onProgress: (Int) -> Unit,
        onComplete: (List<MapDefinition>) -> Unit
    ) {
        scope.launch {
            val maps = mapFinderEngine.scanForPotentialMaps(
                onProgress = { progress ->
                    scope.launch(Dispatchers.Main) { onProgress(progress) }
                }
            )
            withContext(Dispatchers.Main) {
                onComplete(maps)
            }
        }
    }
}