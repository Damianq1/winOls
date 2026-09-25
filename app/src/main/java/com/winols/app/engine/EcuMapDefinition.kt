package com.winols.app.engine

data class EcuMapDefinition(
    val id: String,
    val name: String,
    val address: Int,
    val rows: Int,
    val columns: Int,
    val dataWidth: DataWidth = DataWidth.WORD_16,
    val byteOrder: ByteOrderType = ByteOrderType.LITTLE_ENDIAN,
    val isSigned: Boolean = false,
    val factor: Double = 1.0,
    val offset: Double = 0.0,
    val unit: String = ""
) {
    val totalBytes: Int
        get() = rows * columns * dataWidth.bytesCount
}