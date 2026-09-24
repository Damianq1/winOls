package com.winols.app.data

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class BinaryBufferManager private constructor() {

    private var originalData: ByteArray? = null
    private var modifiedData: ByteArray? = null
    private val rwLock = ReentrantReadWriteLock()

    var config: HexViewConfig = HexViewConfig()

    val size: Int
        get() = rwLock.read { modifiedData?.size ?: 0 }

    val isLoaded: Boolean
        get() = rwLock.read { modifiedData != null }

    fun loadFile(file: File): Boolean {
        return try {
            val length = file.length().toInt()
            val buffer = ByteArray(length)
            FileInputStream(file).use { fis ->
                var totalRead = 0
                while (totalRead < length) {
                    val read = fis.read(buffer, totalRead, length - totalRead)
                    if (read == -1) break
                    totalRead += read
                }
            }

            rwLock.write {
                originalData = buffer.copyOf()
                modifiedData = buffer
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun loadBytes(bytes: ByteArray) {
        rwLock.write {
            originalData = bytes.copyOf()
            modifiedData = bytes.copyOf()
        }
    }

    fun readFormatted(offset: Int): String {
        return rwLock.read {
            val buf = modifiedData ?: return ""
            if (offset < 0 || offset + config.bitWidth.byteCount > buf.size) return "--"
            HexFormatManager.formatCell(buf, offset, config)
        }
    }

    fun writeFormatted(offset: Int, textValue: String): Boolean {
        return rwLock.write {
            val buf = modifiedData ?: return@write false
            try {
                HexFormatManager.parseAndWriteCell(textValue, buf, offset, config)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    fun isModifiedAt(offset: Int, byteCount: Int = config.bitWidth.byteCount): Boolean {
        return rwLock.read {
            val ori = originalData ?: return@read false
            val mod = modifiedData ?: return@read false
            if (offset < 0 || offset + byteCount > mod.size) return@read false

            for (i in 0 until byteCount) {
                if (ori[offset + i] != mod[offset + i]) return@read true
            }
            false
        }
    }

    fun exportModified(destFile: File): Boolean {
        return rwLock.read {
            val mod = modifiedData ?: return@read false
            try {
                FileOutputStream(destFile).use { it.write(mod) }
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    fun getRawBufferCopy(): ByteArray? {
        return rwLock.read { modifiedData?.copyOf() }
    }

    companion object {
        val instance: BinaryBufferManager by lazy { BinaryBufferManager() }
    }
}