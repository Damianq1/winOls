package com.winols.app.checksum

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Typ algorytmu używanego do wyliczenia sumy kontrolnej ECU.
 */
enum class ChecksumType {
    ADD_16_LE,      // 16-bitowa suma addytywna Little Endian
    ADD_16_BE,      // 16-bitowa suma addytywna Big Endian
    ADD_32_LE,      // 32-bitowa suma addytywna Little Endian
    ADD_32_BE,      // 32-bitowa suma addytywna Big Endian
    CRC32_STANDARD, // Standardowy wielomian IEEE 802.3
    BOSCH_EDC15     // Uproszczony model blokowy sumy kontrolnej EDC15 (blok danych + kompensator/inwersja)
}

/**
 * Definicja bloku pamięci podlegającego weryfikacji.
 * @param startOffset Początek zakresu sprawdzania w bajtach
 * @param endOffset Koniec zakresu sprawdzania w bajtach (włącznie)
 * @param targetOffset Adres, pod którym zapisana jest wartość sumy kontrolnej
 * @param type Algorytm sumowania
 */
data class ChecksumBlock(
    val id: String,
    val name: String,
    val startOffset: Int,
    val endOffset: Int,
    val targetOffset: Int,
    val type: ChecksumType
)

data class ChecksumResult(
    val block: ChecksumBlock,
    val expectedValue: Long,
    val actualValue: Long,
    val isValid: Boolean
)

object ChecksumEngine {

    /**
     * Weryfikuje sumy kontrolne dla zdefiniowanych bloków w pliku binarnym.
     */
    fun verifyChecksums(data: ByteArray, blocks: List<ChecksumBlock>): List<ChecksumResult> {
        val results = mutableListOf<ChecksumResult>()

        for (block in blocks) {
            if (block.endOffset >= data.size || block.targetOffset >= data.size) {
                continue
            }

            val calculated = calculate(data, block)
            val stored = readStoredChecksum(data, block)

            results.add(
                ChecksumResult(
                    block = block,
                    expectedValue = calculated,
                    actualValue = stored,
                    isValid = calculated == stored
                )
            )
        }
        return results
    }

    /**
     * Przelicza i koryguje (patchuje) sumy kontrolne bezpośrednio w buforze danych.
     * Zwraca liczbę zaktualizowanych bloków.
     */
    fun correctChecksums(data: ByteArray, blocks: List<ChecksumBlock>): Int {
        var patchedCount = 0

        for (block in blocks) {
            if (block.endOffset >= data.size || block.targetOffset >= data.size) {
                continue
            }

            val calculated = calculate(data, block)
            writeStoredChecksum(data, block, calculated)
            patchedCount++
        }

        return patchedCount
    }

    private fun calculate(data: ByteArray, block: ChecksumBlock): Long {
        return when (block.type) {
            ChecksumType.ADD_16_LE -> calculateSum16(data, block.startOffset, block.endOffset, ByteOrder.LITTLE_ENDIAN)
            ChecksumType.ADD_16_BE -> calculateSum16(data, block.startOffset, block.endOffset, ByteOrder.BIG_ENDIAN)
            ChecksumType.ADD_32_LE -> calculateSum32(data, block.startOffset, block.endOffset, ByteOrder.LITTLE_ENDIAN)
            ChecksumType.ADD_32_BE -> calculateSum32(data, block.startOffset, block.endOffset, ByteOrder.BIG_ENDIAN)
            ChecksumType.CRC32_STANDARD -> calculateCRC32(data, block.startOffset, block.endOffset)
            ChecksumType.BOSCH_EDC15 -> calculateEDC15(data, block.startOffset, block.endOffset)
        }
    }

    private fun calculateSum16(data: ByteArray, start: Int, end: Int, order: ByteOrder): Long {
        var sum = 0L
        val length = (end - start + 1)
        val buffer = ByteBuffer.wrap(data, start, length - (length % 2)).order(order)

        while (buffer.hasRemaining()) {
            sum += (buffer.short.toLong() and 0xFFFFL)
        }
        return sum and 0xFFFFL
    }

    private fun calculateSum32(data: ByteArray, start: Int, end: Int, order: ByteOrder): Long {
        var sum = 0L
        val length = (end - start + 1)
        val buffer = ByteBuffer.wrap(data, start, length - (length % 4)).order(order)

        while (buffer.hasRemaining()) {
            sum += (buffer.int.toLong() and 0xFFFFFFFFL)
        }
        return sum and 0xFFFFFFFFL
    }

    private fun calculateCRC32(data: ByteArray, start: Int, end: Int): Long {
        val crc = java.util.zip.CRC32()
        crc.update(data, start, end - start + 1)
        return crc.value
    }

    private fun calculateEDC15(data: ByteArray, start: Int, end: Int): Long {
        var sum = 0L
        val length = (end - start + 1)
        val buffer = ByteBuffer.wrap(data, start, length - (length % 2)).order(ByteOrder.LITTLE_ENDIAN)

        while (buffer.hasRemaining()) {
            sum = (sum + (buffer.short.toLong() and 0xFFFFL)) and 0xFFFFFFFFL
        }
        // Bosch EDC15 stosuje dopełnienie do zera / inwersję sumy 16-bitowej
        val final16 = (0x10000L - (sum and 0xFFFFL)) and 0xFFFFL
        return final16
    }

    private fun readStoredChecksum(data: ByteArray, block: ChecksumBlock): Long {
        val order = if (block.type.name.endsWith("_BE")) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN
        val buffer = ByteBuffer.wrap(data, block.targetOffset, data.size - block.targetOffset).order(order)

        return when (block.type) {
            ChecksumType.ADD_16_LE, ChecksumType.ADD_16_BE, ChecksumType.BOSCH_EDC15 -> {
                buffer.short.toLong() and 0xFFFFL
            }
            ChecksumType.ADD_32_LE, ChecksumType.ADD_32_BE, ChecksumType.CRC32_STANDARD -> {
                buffer.int.toLong() and 0xFFFFFFFFL
            }
        }
    }

    private fun writeStoredChecksum(data: ByteArray, block: ChecksumBlock, value: Long) {
        val order = if (block.type.name.endsWith("_BE")) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN
        val buffer = ByteBuffer.wrap(data, block.targetOffset, data.size - block.targetOffset).order(order)

        when (block.type) {
            ChecksumType.ADD_16_LE, ChecksumType.ADD_16_BE, ChecksumType.BOSCH_EDC15 -> {
                buffer.putShort((value and 0xFFFFL).toShort())
            }
            ChecksumType.ADD_32_LE, ChecksumType.ADD_32_BE, ChecksumType.CRC32_STANDARD -> {
                buffer.putInt((value and 0xFFFFFFFFL).toInt())
            }
        }
    }
}