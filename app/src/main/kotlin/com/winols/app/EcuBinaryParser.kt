package com.winols.app

class EcuBinaryParser {

    fun formatHexDump(data: ByteArray, maxBytes: Int = 4096): String {
        val limit = minOf(data.size, maxBytes)
        val sb = StringBuilder()

        for (offset in 0 until limit step 16) {
            sb.append(String.format("%08X  ", offset))
            val lineBytes = minOf(16, limit - offset)

            for (i in 0 until 16) {
                if (i < lineBytes) {
                    sb.append(String.format("%02X ", data[offset + i]))
                } else {
                    sb.append("   ")
                }
                if (i == 7) sb.append(" ")
            }

            sb.append(" |")
            for (i in 0 until lineBytes) {
                val b = data[offset + i].toInt() and 0xFF
                if (b in 32..126) {
                    sb.append(b.toChar())
                } else {
                    sb.append('.')
                }
            }
            sb.append("|\n")
        }

        if (data.size > maxBytes) {
            sb.append("\n... [truncated: showing $limit of ${data.size} bytes]")
        }

        return sb.toString()
    }
}