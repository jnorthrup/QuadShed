package com.vsiwest.rangle

import kotlin.jvm.JvmStatic

/**
 * Enhanced PlatformCodec interface that provides explicit endianness support.
 * This allows reading/writing data with a specific endianness regardless of platform.
 */
interface PlatformCodecEnhanced {
    val readLong: (ByteArray) -> Long
    val readInt: (ByteArray) -> Int
    val readShort: (ByteArray) -> Short
    val readFloat: (ByteArray) -> Float get() = { Float.fromBits(readInt(it)) }
    val readDouble: (ByteArray) -> Double get() = { Double.fromBits(readLong(it)) }

    val writeLong: (Long) -> ByteArray
    val writeInt: (Int) -> ByteArray
    val writeShort: (Short) -> ByteArray
    val writeFloat: (Float) -> ByteArray get() = { writeInt(it.toBits()) }
    val writeDouble: (Double) -> ByteArray get() = { writeLong(it.toBits()) }

    // Unsigned variants
    val readUShort: (ByteArray) -> UShort get() = { readShort(it).toUShort() }
    val readUInt: (ByteArray) -> UInt get() = { readInt(it).toUInt() }
    val readULong: (ByteArray) -> ULong get() = { readLong(it).toULong() }
    val writeUShort: (UShort) -> ByteArray get() = { writeShort(it.toShort()) }
    val writeUInt: (UInt) -> ByteArray get() = { writeInt(it.toInt()) }
    val writeULong: (ULong) -> ByteArray get() = { writeLong(it.toLong()) }

    companion object {
        // Platform endianness detection
        val isLittleEndian: Boolean by lazy {
            val i = 0x01020304
            val b = i.toByte()
            b == 0x04.toByte()
        }
        
        val isBigEndian: Boolean get() = !isLittleEndian

        // Factory method to get codec for specific endianness
        fun get(littleEndian: Boolean): PlatformCodecEnhanced {
            return if (littleEndian) LittleEndianCodec else BigEndianCodec
        }

        // Standard codec instances
        val LittleEndian: PlatformCodecEnhanced = LittleEndianCodec
        val BigEndian: PlatformCodecEnhanced = BigEndianCodec // Network Order

        // Current platform codec for native operations
        val currentPlatformCodec: PlatformCodecEnhanced by lazy {
            get(isLittleEndian)
        }
    }
}

// Concrete implementation for Little Endian
internal object LittleEndianCodec : PlatformCodecEnhanced {
    // Assume input ByteArray *is* Little Endian
    override val readShort: (ByteArray) -> Short =
        { ((it[1].toInt() and 0xFF) shl 8 or (it[0].toInt() and 0xFF)).toShort() }
    
    override val readInt: (ByteArray) -> Int =
        {
            ((it[3].toInt() and 0xFF) shl 24) or
            ((it[2].toInt() and 0xFF) shl 16) or
            ((it[1].toInt() and 0xFF) shl 8) or
            (it[0].toInt() and 0xFF)
        }
    
    override val readLong: (ByteArray) -> Long =
        {
            ((it[7].toLong() and 0xFF) shl 56) or
            ((it[6].toLong() and 0xFF) shl 48) or
            ((it[5].toLong() and 0xFF) shl 40) or
            ((it[4].toLong() and 0xFF) shl 32) or
            ((it[3].toLong() and 0xFF) shl 24) or
            ((it[2].toLong() and 0xFF) shl 16) or
            ((it[1].toLong() and 0xFF) shl 8) or
            (it[0].toLong() and 0xFF)
        }
    
    // Write value *as* Little Endian bytes
    override val writeShort: (Short) -> ByteArray =
        { v -> byteArrayOf( (v.toInt() and 0xFF).toByte(), (v.toInt() shr 8 and 0xFF).toByte() ) }
    
    override val writeInt: (Int) -> ByteArray =
        { v -> byteArrayOf(
            (v        and 0xFF).toByte(),
            (v shr 8  and 0xFF).toByte(),
            (v shr 16 and 0xFF).toByte(),
            (v shr 24 and 0xFF).toByte()
        )}
    
    override val writeLong: (Long) -> ByteArray =
        { v -> byteArrayOf(
            (v        and 0xFF).toByte(),
            (v shr 8  and 0xFF).toByte(),
            (v shr 16 and 0xFF).toByte(),
            (v shr 24 and 0xFF).toByte(),
            (v shr 32 and 0xFF).toByte(),
            (v shr 40 and 0xFF).toByte(),
            (v shr 48 and 0xFF).toByte(),
            (v shr 56 and 0xFF).toByte()
        )}
}

// Concrete implementation for Big Endian (Network Order)
internal object BigEndianCodec : PlatformCodecEnhanced {
    // Assume input ByteArray *is* Big Endian
    override val readShort: (ByteArray) -> Short =
        { ((it[0].toInt() and 0xFF) shl 8 or (it[1].toInt() and 0xFF)).toShort() }
    
    override val readInt: (ByteArray) -> Int =
        {
            ((it[0].toInt() and 0xFF) shl 24) or
            ((it[1].toInt() and 0xFF) shl 16) or
            ((it[2].toInt() and 0xFF) shl 8) or
            (it[3].toInt() and 0xFF)
        }
    
    override val readLong: (ByteArray) -> Long =
        {
            ((it[0].toLong() and 0xFF) shl 56) or
            ((it[1].toLong() and 0xFF) shl 48) or
            ((it[2].toLong() and 0xFF) shl 40) or
            ((it[3].toLong() and 0xFF) shl 32) or
            ((it[4].toLong() and 0xFF) shl 24) or
            ((it[5].toLong() and 0xFF) shl 16) or
            ((it[6].toLong() and 0xFF) shl 8) or
            (it[7].toLong() and 0xFF)
        }
    
    // Write value *as* Big Endian bytes
    override val writeShort: (Short) -> ByteArray =
        { v -> byteArrayOf( (v.toInt() shr 8 and 0xFF).toByte(), (v.toInt() and 0xFF).toByte() ) }
    
    override val writeInt: (Int) -> ByteArray =
        { v -> byteArrayOf(
            (v shr 24 and 0xFF).toByte(),
            (v shr 16 and 0xFF).toByte(),
            (v shr 8  and 0xFF).toByte(),
            (v        and 0xFF).toByte()
        )}
    
    override val writeLong: (Long) -> ByteArray =
        { v -> byteArrayOf(
            (v shr 56 and 0xFF).toByte(),
            (v shr 48 and 0xFF).toByte(),
            (v shr 40 and 0xFF).toByte(),
            (v shr 32 and 0xFF).toByte(),
            (v shr 24 and 0xFF).toByte(),
            (v shr 16 and 0xFF).toByte(),
            (v shr 8  and 0xFF).toByte(),
            (v        and 0xFF).toByte()
        )}
}
