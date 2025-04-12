package com.vsiwest.rangle.hdf5

import com.vsiwest.rangle.Series
import com.vsiwest.rangle.PlatformCodec

// Helper extensions for reading primitive types respecting endianness
internal fun Series<Byte>.readShort(offset: Long, isLittleEndian: Boolean): Short {
    require(offset + 1 < this.size)
    val b0 = this[offset.toInt()].toInt() and 0xFF
    val b1 = this[(offset + 1).toInt()].toInt() and 0xFF
    return if (isLittleEndian) ((b1 shl 8) or b0).toShort() else ((b0 shl 8) or b1).toShort()
}

internal fun Series<Byte>.readUInt16(offset: Long, isLittleEndian: Boolean): Int =
    readShort(offset, isLittleEndian).toInt() and 0xFFFF

internal fun Series<Byte>.readInt(offset: Long, isLittleEndian: Boolean): Int {
    require(offset + 3 < this.size)
    val b0 = this[offset.toInt()].toInt() and 0xFF
    val b1 = this[(offset + 1).toInt()].toInt() and 0xFF
    val b2 = this[(offset + 2).toInt()].toInt() and 0xFF
    val b3 = this[(offset + 3).toInt()].toInt() and 0xFF
    return if (isLittleEndian) {
        (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
    } else {
        (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
    }
}

internal fun Series<Byte>.readUInt32(offset: Long, isLittleEndian: Boolean): Int =
    readInt(offset, isLittleEndian)

internal fun Series<Byte>.readLong(offset: Long, isLittleEndian: Boolean): Long {
    require(offset + 7 < this.size)
    var value = 0L
    if (isLittleEndian) {
        for (i in 7 downTo 0) {
            value = (value shl 8) or (this[(offset + i).toInt()].toLong() and 0xFF)
        }
    } else {
        for (i in 0..7) {
            value = (value shl 8) or (this[(offset + i).toInt()].toLong() and 0xFF)
        }
    }
    return value
}

internal fun ByteArray.readInt(offset: Int, requiresSwap: Boolean): Int {
    // Simplified - assumes length >= 4
    val b0 = this[offset + 0].toInt() and 0xFF
    val b1 = this[offset + 1].toInt() and 0xFF
    val b2 = this[offset + 2].toInt() and 0xFF
    val b3 = this[offset + 3].toInt() and 0xFF
    return if (requiresSwap == PlatformCodec.isLittleEndian) { // Target LE matches platform LE OR Target BE matches platform BE
        PlatformCodec.currentPlatformCodec.readInt(this.sliceArray(offset until offset+4)) // Use platform direct
    } else { // Need to swap
        if (PlatformCodec.isLittleEndian) { // Platform is LE, Target is BE
            (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
        } else { // Platform is BE, Target is LE
            (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
        }
    }
}

internal fun ByteArray.readLong(offset: Int, requiresSwap: Boolean): Long {
    // Simplified - assumes length >= 8
    return if (requiresSwap == PlatformCodec.isLittleEndian) { // Target LE matches platform LE OR Target BE matches platform BE
        PlatformCodec.currentPlatformCodec.readLong(this.sliceArray(offset until offset+8)) // Use platform direct
    } else { // Need to swap
        var value = 0L
        if (PlatformCodec.isLittleEndian) { // Platform is LE, Target is BE
            for (i in 0..7) {
                value = (value shl 8) or (this[offset + i].toLong() and 0xFF)
            }
        } else { // Platform is BE, Target is LE
            for (i in 7 downTo 0) {
                value = (value shl 8) or (this[offset + i].toLong() and 0xFF)
            }
        }
        value
    }
}

internal fun ByteArray.readShort(offset: Int, requiresSwap: Boolean): Short {
    // Simplified - assumes length >= 2
    val b0 = this[offset + 0].toInt() and 0xFF
    val b1 = this[offset + 1].toInt() and 0xFF
    return if (requiresSwap == PlatformCodec.isLittleEndian) { // Target LE matches platform LE OR Target BE matches platform BE
        PlatformCodec.currentPlatformCodec.readShort(this.sliceArray(offset until offset+2)) // Use platform direct
    } else { // Need to swap
        if (PlatformCodec.isLittleEndian) { // Platform is LE, Target is BE
            ((b0 shl 8) or b1).toShort()
        } else { // Platform is BE, Target is LE
            ((b1 shl 8) or b0).toShort()
        }
    }
}
