package com.vsiwest.rangle.hdf5

import com.vsiwest.rangle.Series
import com.vsiwest.rangle.PlatformCodec
import com.vsiwest.rangle.PlatformCodecEnhanced

// Helper extensions for reading primitive types from a Series<Byte>
internal fun Series<Byte>.readShort(offset: Long, isLittleEndian: Boolean): Short {
    require(offset + 1 < this.size)
    val bytes = ByteArray(2) { this[(offset + it).toInt()] }
    return PlatformCodecEnhanced.get(isLittleEndian).readShort(bytes)
}

internal fun Series<Byte>.readUInt16(offset: Long, isLittleEndian: Boolean): Int =
    readShort(offset, isLittleEndian).toInt() and 0xFFFF

internal fun Series<Byte>.readInt(offset: Long, isLittleEndian: Boolean): Int {
    require(offset + 3 < this.size)
    val bytes = ByteArray(4) { this[(offset + it).toInt()] }
    return PlatformCodecEnhanced.get(isLittleEndian).readInt(bytes)
}

internal fun Series<Byte>.readUInt32(offset: Long, isLittleEndian: Boolean): Int =
    readInt(offset, isLittleEndian)

internal fun Series<Byte>.readLong(offset: Long, isLittleEndian: Boolean): Long {
    require(offset + 7 < this.size)
    val bytes = ByteArray(8) { this[(offset + it).toInt()] }
    return PlatformCodecEnhanced.get(isLittleEndian).readLong(bytes)
}

internal fun ByteArray.readInt(offset: Int, requiresSwap: Boolean): Int {
    val bytes = this.sliceArray(offset until offset + 4)
    return PlatformCodecEnhanced.get(requiresSwap != PlatformCodec.isLittleEndian).readInt(bytes)
}

internal fun ByteArray.readLong(offset: Int, requiresSwap: Boolean): Long {
    val bytes = this.sliceArray(offset until offset + 8)
    return PlatformCodecEnhanced.get(requiresSwap != PlatformCodec.isLittleEndian).readLong(bytes)
}

internal fun ByteArray.readShort(offset: Int, requiresSwap: Boolean): Short {
    val bytes = this.sliceArray(offset until offset + 2)
    return PlatformCodecEnhanced.get(requiresSwap != PlatformCodec.isLittleEndian).readShort(bytes)
}
