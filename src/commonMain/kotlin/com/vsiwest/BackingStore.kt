package com.vsiwest

//typealias Twin<T> = Join<T, T>
//
//interface Join<out A, out B> {
//    val a: A
//    val b: B
//}
//
interface BackingStore<T> {
    val p: T
}

sealed class BitAllocation<T> {
    abstract val bits: Int
    abstract fun encode(value: T): ULong
    abstract fun decode(value: ULong): T
}

object ByteAllocation : BitAllocation<Byte>() {
    override val bits: Int = 8
    override fun encode(value: Byte): ULong = value.toULong() and 0xFFUL
    override fun decode(value: ULong): Byte = value.toByte()
}

object ShortAllocation : BitAllocation<Short>() {
    override val bits: Int = 16
    override fun encode(value: Short): ULong = value.toULong() and 0xFFFFUL
    override fun decode(value: ULong): Short = value.toShort()
}

object IntAllocation : BitAllocation<Int>() {
    override val bits: Int = 32
    override fun encode(value: Int): ULong = value.toULong() and 0xFFFFFFFFUL
    override fun decode(value: ULong): Int = value.toInt()
}

object LongAllocation : BitAllocation<Long>() {
    override val bits: Int = 64
    override fun encode(value: Long): ULong = value.toULong()
    override fun decode(value: ULong): Long = value.toLong()
}

object CharAllocation : BitAllocation<Char>() {
    override val bits: Int = 16
    override fun encode(value: Char): ULong = value.code.toULong() and 0xFFFFUL
    override fun decode(value: ULong): Char = value.toInt().toChar()
}

inline fun <reified T> getAllocation(): BitAllocation<T> = when (T::class) {
    Byte::class -> ByteAllocation as BitAllocation<T>
    Short::class -> ShortAllocation as BitAllocation<T>
    Int::class -> IntAllocation as BitAllocation<T>
    Long::class -> LongAllocation as BitAllocation<T>
    Char::class -> CharAllocation as BitAllocation<T>
    else -> throw IllegalArgumentException("Unsupported type: ${T::class}")
}