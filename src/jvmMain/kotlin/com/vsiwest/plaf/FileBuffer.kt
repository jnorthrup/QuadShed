
package com.vsiwest.plaf

import com.vsiwest.logDebug
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.nio.file.Paths

/**
 * An openable and closeable mmap file.
 *
 * Get has no side effects but put has undefined effects on size and sync.
 *
 * @see FileBuffer.open
 */
actual class FileBuffer actual constructor(
    actual val filename: String,
    actual val initialOffset: Long,
    actual val blkSize: Long,
    actual val readOnly: Boolean
) : LongSeries<Byte> {

    private var jvmChannel: FileChannel? = null
    private var jvmMappedByteBuffer: MappedByteBuffer? = null

    actual override val a: Long
        get() = jvmMappedByteBuffer?.limit()?.toLong() ?: 0L

    actual override val b: (Long) -> Byte
        get() = { index: Long ->
            jvmMappedByteBuffer!!.get(index.toInt())
        }

    actual fun open() {
        if (isOpen()) return
        logDebug { "Opening $filename" }

        val path = Paths.get(filename)
        val options = if (readOnly) {
            setOf(StandardOpenOption.READ)
        } else {
            setOf(StandardOpenOption.READ, StandardOpenOption.WRITE)
        }

        jvmChannel = FileChannel.open(path, options).apply {
            val size = if (blkSize == -1L) size() - initialOffset else blkSize
            jvmMappedByteBuffer = map(
                if (readOnly) FileChannel.MapMode.READ_ONLY else FileChannel.MapMode.READ_WRITE,
                initialOffset,
                size
            )
        }

        logDebug { "Opened $filename" }
    }

    actual fun close() {
        if (!isOpen()) return
        logDebug { "Closing $filename" }
        jvmMappedByteBuffer?.force()
        jvmMappedByteBuffer = null
        jvmChannel?.close()
        jvmChannel = null
        logDebug { "Closed $filename" }
    }

    actual fun isOpen(): Boolean = jvmMappedByteBuffer != null

    actual fun size(): Long = jvmMappedByteBuffer?.capacity()?.toLong() ?: 0L

    actual fun get(index: Long): Byte = jvmMappedByteBuffer!!.get(index.toInt())

    actual fun put(index: Long, value: Byte) {
        if (readOnly) throw Exception("Cannot modify read-only buffer")
        jvmMappedByteBuffer!!.put(index.toInt(), value)
    }
}