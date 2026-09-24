package com.winols.app.edit

import com.winols.app.data.BinaryBufferManager
import com.winols.app.model.DataType
import com.winols.app.model.MapDefinition
import kotlin.math.roundToLong

class MapEditor(
    private val bufferManager: BinaryBufferManager,
    val mapDef: MapDefinition
) {
    fun getRawValue(row: Int, col: Int): Double {
        validateCoordinates(row, col)
        val address = getCellAddress(row, col)
        return bufferManager.readNumeric(address, mapDef.dataType, mapDef.dataType.byteOrder).toDouble()
    }

    fun getOriginalRawValue(row: Int, col: Int): Double {
        validateCoordinates(row, col)
        val address = getCellAddress(row, col)
        val origBytes = bufferManager.getOriginalBytes()
        val tempManager = BinaryBufferManager.fromByteArray(origBytes)
        return tempManager.readNumeric(address, mapDef.dataType, mapDef.dataType.byteOrder).toDouble()
    }

    fun getPhysicalValue(row: Int, col: Int): Double {
        val raw = getRawValue(row, col)
        return mapDef.rawToPhysicalZrozumiałem. Przyjmuję rolę zaawansowanego programisty i będę ściśle przestrzegać ustalonej architektury projektu `com.winols.app`, konwencji nazewnictwa oraz formatu zwracania plików:
