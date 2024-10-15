package com.vsiwest.rangle

import Series
import com.vsiwest.rangle.IOMemento.Companion.readByte
import com.vsiwest.rangle.IOMemento.Companion.writeByte
import com.vsiwest.rangle.PlatformCodec.*
import com.vsiwest.rangle.PlatformCodec.Companion.currentPlatformCodec.*
import com.vsiwest.rangle.PlatformCodec.Companion.currentPlatformCodec.readInt
import com.vsiwest.rangle.PlatformCodec.Companion.currentPlatformCodec.readLong
import com.vsiwest.rangle.PlatformCodec.Companion.currentPlatformCodec.writeInt
import com.vsiwest.rangle.PlatformCodec.Companion.currentPlatformCodec.writeLong
import get
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or
import kotlin.math.absoluteValue

// Safe conversion extension function
@Suppress("UNCHECKED_CAST")
fun <A : Comparable<A>, P : Comparable<P>> A.duckConvert(foreign: P): A {
    if (this::class == foreign::class) return foreign as A

    return when (this) {
        is Boolean -> (foreign as? Number)?.toInt() != 0 as A
        is Byte -> (foreign as? Number)?.toByte() ?: (foreign as? Char)?.code?.toByte() as A
        is Short -> (foreign as? Number)?.toShort() ?: (foreign as? Char)?.code?.toShort() as A
        is Int -> (foreign as? Number)?.toInt() ?: (foreign as? Char)?.code as A
        is Long -> (foreign as? Number)?.toLong() ?: (foreign as? Char)?.code?.toLong() as A
        is Float -> (foreign as? Number)?.toFloat() ?: (foreign as? Char)?.code?.toFloat() as A
        is Double -> (foreign as? Number)?.toDouble() ?: (foreign as? Char)?.code?.toDouble() as A
        is Char -> ((foreign as? Number)?.toInt()?.toChar() ?: foreign as? Char as A) as A
        else -> error("Unsupported conversion to: ${this::class.simpleName}")
    } as A
}

fun <K> K.duckInc(index: K): K {
    return when (index) {
        is Int -> index.inc()
        is Long -> index.inc()
        is Short -> (index.inc()).toShort()
        is Byte -> (index.inc()).toByte()
        else -> throw IllegalArgumentException("Unsupported index type")
    } as K
}

fun <K> K.duckMinus(a: K, b: K): K {
    return when (a) {
        is Int -> a - b as Int
        is Long -> a - b as Long
        is Short -> (a - b as Short).toShort()
        is Byte -> (a - b as Byte).toByte()

        else -> throw IllegalArgumentException("Unsupported index type")
    } as K
}

//duckplus
fun <K> K.duckPlus(a: K, b: K): K {
    return when (a) {
        is Int -> a + b as Int
        is Long -> a + b as Long
        is Short -> (a + b as Short).toShort()
        is Byte -> (a + b as Byte).toByte()
        else -> throw IllegalArgumentException("Unsupported index type")
    } as K
}

fun <K> K.duckOr(a: K, b: K): K {
    return when (a) {
        is Long -> a or b as Long
        is Int -> a or b as Int
        is Short -> (a or b as Short).toShort()
        is Byte -> (a or b as Byte).toByte()
        else -> throw IllegalArgumentException("Unsupported index type")
    } as K
}

fun <K> K.duckAnd(a: K, b: K): K {
    return when (a) {
        is Long -> a and b as Long
        is Int -> a and b as Int
        is Short -> (a and b as Short).toShort()
        is Byte -> (a and b as Byte).toByte()

        else -> throw IllegalArgumentException("Unsupported index type")
    } as K
}

/** - ushiftleft positive ushr right */
fun <K> K.duckShift(
    index: K,
    /** shift left = -bits.  shift right = positive */
    leftToRight: Int,
): K {

    val func: (Long, Int) -> Long = if (leftToRight < 0) Long::shl else Long::ushr
    val abshift = leftToRight.absoluteValue
    return when (index) {
        is Int -> func(index.toLong(), abshift).toInt()
        is Long -> func(index, abshift)
        is Short -> func(index.toLong(), abshift).toShort()
        is Byte -> func(index.toLong(), abshift).toByte()
        else -> throw IllegalArgumentException("Unsupported index type")
    } as K
}

fun <K> K.duckClz(index: K): K {
    return when (index) {
        is Int -> index.countLeadingZeroBits() as K
        is Long -> index.countLeadingZeroBits() as K
        else -> throw IllegalArgumentException("Unsupported index type")
    }
}

fun <K> K.duckInv(index: K): K {
    return when (index) {
        is Int -> index.inv() as K
        is Long -> index.inv() as K
        else -> throw IllegalArgumentException("Unsupported index type")
    }
}

interface DuckOperation<T : Comparable<T>> {
    fun plus(a: T, b: T): T
    fun minus(a: T, b: T): T
    fun or(a: T, b: T): T
    fun and(a: T, b: T): T
    fun shl(a: T, b: Int): T  // Shift left
    fun shr(a: T, b: Int): T  // Shift right (unsigned)
    fun clz(a: T): Int       // Count leading zeros
    fun inv(a: T): T        // Bitwise inversion
}

interface UnifiedType<T : Comparable<T>> : DuckOperation<T> {
    val networkSize: Int?
    fun encode(value: T): ByteArray
    fun decode(bytes: ByteArray): T
}

typealias ConversionObject<T> = UnifiedType<T>

object UnifiedTypes {
    val BooleanType = object : ConversionObject<Boolean> {
        override val networkSize: Int? = 1
        override fun encode(value: Boolean): ByteArray = ByteArray(1) { if (value) 1 else 0 }


        override fun decode(bytes: ByteArray): Boolean = bytes[0] != 0.toByte() // 0 is false,else true

        override fun plus(a: Boolean, b: Boolean): Boolean = a || b
        override fun minus(a: Boolean, b: Boolean): Boolean = a || !b
        override fun or(a: Boolean, b: Boolean): Boolean = a || b
        override fun and(a: Boolean, b: Boolean): Boolean = a && b
        override fun shl(a: Boolean, b: Int): Boolean =
            throw UnsupportedOperationException("Shift operations are not supported for Boolean")

        override fun shr(a: Boolean, b: Int): Boolean =
            throw UnsupportedOperationException("Shift operations are not supported for Boolean")

        override fun clz(a: Boolean): Int =
            throw UnsupportedOperationException("Count leading zeros is not supported for Boolean")

        override fun inv(a: Boolean): Boolean =
            throw UnsupportedOperationException("Bitwise inversion is not supported for Boolean")

        // fromChars function from IOMemento
        fun fromChars(chars: Series<Char>): Boolean =
            when (chars[0]) {
                't' -> true
                'f' -> false
                else -> throw IllegalArgumentException("invalid boolean: $chars")
            }
    }

    val ByteType = object : ConversionObject<Byte> {
        override val networkSize: Int? = 1
        override fun encode(value: Byte): ByteArray =
            writeByte(value)

        override fun decode(bytes: ByteArray): Byte =
            readByte(bytes)

        override fun plus(a: Byte, b: Byte): Byte = (a + b).toByte()
        override fun minus(a: Byte, b: Byte): Byte = (a - b).toByte()
        override fun or(a: Byte, b: Byte): Byte = (a or b).toByte()
        override fun and(a: Byte, b: Byte): Byte = (a and b).toByte()
        override fun shl(a: Byte, b: Int): Byte = (a.toInt().shl(b)).toByte()
        override fun shr(a: Byte, b: Int): Byte = (a.toInt().shr(b)).toByte()
        override fun clz(a: Byte): Int = a.countLeadingZeroBits()
        override fun inv(a: Byte): Byte = a.inv()

        // fromChars function from IOMemento
        fun fromChars(chars: Series<Char>): Byte = chars.parseLong().toByte()

    }

    val UByteType = object : ConversionObject<UByte> {
        override val networkSize: Int? = 1
        override fun encode(value: UByte): ByteArray =
            writeUByte(value)

        override fun decode(bytes: ByteArray): UByte =
            readUByte(bytes)

        override fun plus(a: UByte, b: UByte): UByte = (a + b).toUByte()
        override fun minus(a: UByte, b: UByte): UByte = (a - b).toUByte()
        override fun or(a: UByte, b: UByte): UByte = (a or b).toUByte()
        override fun and(a: UByte, b: UByte): UByte = (a and b).toUByte()
        override fun shl(a: UByte, b: Int): UByte = (a shl b).toUByte()
        override fun shr(a: UByte, b: Int): UByte = (a shr b).toUByte()
        override fun clz(a: UByte): Int = a.toInt().countLeadingZeroBits()
        override fun inv(a: UByte): UByte = a.inv()

        // fromChars function from IOMemento
        fun fromChars(chars: Series<Char>): UByte = chars.parseLong().toUByte()
    }

    val ShortType = object : ConversionObject<Short> {
        override val networkSize: Int? = 2
        override fun encode(value: Short): ByteArray =
            writeShort(value)

        override fun decode(bytes: ByteArray): Short =
            readShort(bytes)

        override fun plus(a: Short, b: Short): Short = (a + b).toShort()
        override fun minus(a: Short, b: Short): Short = (a - b).toShort()
        override fun or(a: Short, b: Short): Short = (a or b).toShort()
        override fun and(a: Short, b: Short): Short = (a and b).toShort()
        override fun shl(a: Short, b: Int): Short = (1 * a shl b).toShort()
        override fun shr(a: Short, b: Int): Short = (1 * a shr b).toShort()
        override fun clz(a: Short): Int = a.toInt().countLeadingZeroBits()
        override fun inv(a: Short): Short = a.inv()

        // fromChars function from IOMemento
        fun fromChars(chars: Series<Char>): Short = chars.parseLong().toShort()
    }

    val IntType = object : ConversionObject<Int> {
        override val networkSize: Int? = 4
        override fun encode(value: Int): ByteArray =
            writeInt(value)

        override fun decode(bytes: ByteArray): Int =
            readInt(bytes)

        override fun plus(a: Int, b: Int): Int = a + b
        override fun minus(a: Int, b: Int): Int = a - b
        override fun or(a: Int, b: Int): Int = a or b
        override fun and(a: Int, b: Int): Int = a and b
        override fun shl(a: Int, b: Int): Int = a shl b
        override fun shr(a: Int, b: Int): Int = a ushr b // Use unsigned shift right (ushr)
        override fun clz(a: Int): Int = a.countLeadingZeroBits()
        override fun inv(a: Int): Int = a.inv()

        // fromChars function from IOMemento
        fun fromChars(chars: Series<Char>): Int = chars.parseLong().toInt()
    }

    val LongType = object : ConversionObject<Long> {
        override val networkSize: Int? = 8
        override fun encode(value: Long): ByteArray =
            writeLong(value)

        override fun decode(bytes: ByteArray): Long =
            readLong(bytes)

        override fun plus(a: Long, b: Long): Long = a + b
        override fun minus(a: Long, b: Long): Long = a - b
        override fun or(a: Long, b: Long): Long = a or b
        override fun and(a: Long, b: Long): Long = a and b
        override fun shl(a: Long, b: Int): Long = a shl b
        override fun shr(a: Long, b: Int): Long = a ushr b // Use unsigned shift right (ushr)
        override fun clz(a: Long): Int = a.countLeadingZeroBits()
        override fun inv(a: Long): Long = a.inv()

        // fromChars function from IOMemento
        fun fromChars(chars: Series<Char>): Long = chars.parseLong()
    }

    // ... other types (Short, Char, etc.)

}


