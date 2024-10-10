@file:OptIn(ExperimentalForeignApi::class) @file:Suppress("UNCHECKED_CAST")

package com.vsiwest.plaf

import kotlinx.cinterop.*
import platform.posix.*
import platform.posix.O_RDWR
import platform.posix.PROT_WRITE
import platform.posix.mmap

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
    actual val readOnly: Boolean,
) : LongSeries<Byte> {

    private var fd: Int = -1
    private var mappedPtr: CPointer<ByteVar>? = null
    private var fileSize: Long = 0

    actual override val a: Long
        get() = fileSize

    actual override val b: (Long) -> Byte
        get() = { index: Long ->
            mappedPtr!![index.toInt()]
        }

    actual fun open() {
        if (isOpen()) return
        val flags = if (readOnly) O_RDONLY else O_RDWR
        fd = open(filename, flags)
        if (fd == -1) throw Error("Failed to open file")

        memScoped {
            val statBuf = alloc<stat>()
            if (fstat(fd, statBuf.ptr) != 0) throw Error("Failed to get file size")
            fileSize = if (blkSize == -1L) statBuf.st_size - initialOffset else blkSize
        }

        val prot = if (readOnly) PROT_READ else PROT_READ or PROT_WRITE
        val mapFlags = if (readOnly) MAP_PRIVATE else MAP_SHARED

        mappedPtr = mmap(null, fileSize.convert(), prot, mapFlags, fd, initialOffset)!!.reinterpret()
        if (mappedPtr == MAP_FAILED) throw Error("Failed to map file")
    }

    actual fun close() {
        if (!isOpen()) return
        munmap(mappedPtr, fileSize.convert())
        mappedPtr = null
        close(fd)
        fd = -1
    }

    actual fun isOpen(): Boolean = fd != -1

    actual fun size(): Long = fileSize

    actual fun get(index: Long): Byte = mappedPtr!![index.toInt()]

    actual fun put(index: Long, value: Byte) {
        if (readOnly) throw Error("Cannot modify read-only buffer")
        mappedPtr!![index.toInt()] = value
    }
}
