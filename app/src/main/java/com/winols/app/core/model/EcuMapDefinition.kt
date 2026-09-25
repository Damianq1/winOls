package com.winols.app.core.model

import com.winols.app.core.binary.DataEndianness
import com.winols.app.core.binary.ValueType

data class AxisDefinition(
    val name: String,
    val unit: String,
    val startAddress: Int,
    val length: Int,
    val valueType: ValueType,
    val endianness: DataEndianness,
    val factor: Double = 1.0,
    val offset: Double = 0.0
)

data class EcuMapDefinition(
    val id: String,
    val name: String,
    val description: String,
    val zStartAddress: Int,
    val valueType: ValueType,
    val endianness: DataEndianness,
    val factor: Double = 1.0,
    val offset: Double = 0.0,
    val xAxis: AxisDefinition,
    val yAxis: AxisDefinition
)