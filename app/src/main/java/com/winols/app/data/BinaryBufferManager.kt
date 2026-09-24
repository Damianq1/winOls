package com.winols.app.data

import com.winols.app.model.SignMode
import com.winols.app.model.ViewConfiguration
import com.winols.app.model.WordSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Singleton / Manager bufora pamięci Flash/EEPROM z obsługą dynamicznej zmiany widoku.
 */
class BinaryBufferManager {

    private var buffer: ByteBuffer? = null

    private val _viewConfig = MutableStateFlow(ViewConfiguration())
    val viewConfig: StateFlow<ViewConfiguration> = _viewConfig.asStateFlow()

    fun setBuffer(data: ByteArray) {
        val directBuf = ByteBuffer.allocateDirect(data.size)
        directBuf.order(ByteOrder.LITTLE_ENDIAN)
        directBuf.put(data)
        directBuf.position(0)
        this.buffer = directBuf
    }

    fun getBuffer(): ByteBuffer? = buffer

    fun updateWordSize(size: WordSize) {
        _viewConfig.value = _viewConfig.value.copy(wordSize = size)
    }

    fun updateByteOrder(order: ByteOrder) {
        _viewConfig.value = _viewConfig.value.copy(byteOrder = order)
    }

    fun updateSignMode(signMode: SignMode) {
        _viewConfig.value = _viewConfig.value.copy(signMode = signMode)
    }

    fun toggleHexDecimal(showHex: Boolean) {
        _viewConfig.value = _viewConfig.value.copy(isHexDisplay = showHex)
    }

    fun updatePhysicalFactors(factor: Double, offset: Double, precision: Int = 2) {
        _viewConfig.value = _viewConfig.value.copy(
            factor = factor,
            offset = offset,
            precision = precision
        )
    }

    fun setColumnsCount(columns: Int) {
        if (columns > 0) {
            _viewConfig.value = _viewConfig.value.copy(columnsCount = columns)
        }
    }

    fun getFormattedValueAt(address: Int): String {
        val currentBuf = buffer ?: return ""
        return DataFormatterEngine.formatValue(currentBuf, address, _viewConfig.value)
    }
}