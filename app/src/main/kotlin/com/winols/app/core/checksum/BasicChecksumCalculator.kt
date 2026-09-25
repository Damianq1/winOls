package com.winols.app.core.checksum

import com.winols.app.core.binary.EcuBinaryBuffer

object BasicChecksumCalculator {

    /**
     * Prosta suma kontrolna 16-bit (16-bit Additive Checksum).
     * Używana jako fallback lub element bazowy dla starszych sterowników.
     */
    fun calculate16BitSum(buffer: EcuBinaryBuffer, startAddress: Int, endAddress: Int): Int {
        var sum = 0
        var i = startAddress
        while (i < endAddress) {
            sum = (sum + buffer.getWord(i)) and 0xFFFF
            i += 2
        }
        return sum
    }

    /**
     * Negacja 16-bitowa sumy (często stosowana w blokach weryfikacji).
     */
    fun calculate16BitComplement(buffer: EcuBinaryBuffer, startAddress: Int, endAddress: Int): Int {
        val sum = calculate16BitSum(buffer, startAddress, endAddress)
        return (0x10000 - sum) and 0xFFFF
    }
}