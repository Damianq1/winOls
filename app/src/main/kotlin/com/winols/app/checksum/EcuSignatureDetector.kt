package com.winols.app.checksum

/**
 * Detektor profili ECU rozpoznający typ sterownika po podpisie binarnym (np. Bosch EDC15, EDC16).
 */
object EcuSignatureDetector {

    fun detectBlocks(data: ByteArray): List<ChecksumBlock> {
        val blocks = mutableListOf<ChecksumBlock>()

        // Detekcja plików o standardowym rozmiarze 512 KB (EDC15P/V 29F400BT)
        if (data.size == 524288) {
            blocks.add(
                ChecksumBlock(
                    id = "edc15_main",
                    name = "Bosch EDC15 Main Bank",
                    startOffset = 0x00000,
                    endOffset = 0x77FFF,
                    targetOffset = 0x7FE00,
                    type = ChecksumType.BOSCH_EDC15
                )
            )
        }
        // Detekcja plików o standardowym rozmiarze 1024 KB / 2048 KB (EDC16 / ME7)
        else if (data.size == 1048576 || data.size == 2097152) {
            blocks.add(
                ChecksumBlock(
                    id = "edc16_block1",
                    name = "EDC16 Code/Data Block 1",
                    startOffset = 0x00000,
                    endOffset = 0x3FFFF,
                    targetOffset = 0x3FFFC,
                    type = ChecksumType.ADD_32_BE
                )
            )
            blocks.add(
                ChecksumBlock(
                    id = "edc16_block2",
                    name = "EDC16 Calibration Block 2",
                    startOffset = 0x40000,
                    endOffset = 0x7FFFF,
                    targetOffset = 0x7FFFC,
                    type = ChecksumType.ADD_32_BE
                )
            )
        }

        return blocks
    }
}