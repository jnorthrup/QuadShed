@file:Suppress("INLINE_CLASS_DEPRECATED")

package com.vsiwest

import com.vsiwest.meta.IOMemento
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime


//char


typealias Series<V> = MetaSeries<Int, V>

/**
 * index by enum
 */
operator fun <S, K : Enum<K>> Series<S>.get(e: K): S = b(e.ordinal)

/** Series toList
 * @return an AbstractList<T> of the Series<T>
 */
fun <T> Series<T>.toList(): AbstractList<T> = object : AbstractList<T>() {
    override val size: Int = a
    override fun get(index: Int): T = b(index)
}


fun Series<Byte>.toArray(): ByteArray = ByteArray(a, ::get)

fun Series<Char>.toArray(): CharArray = CharArray(a, ::get)

fun Series<Int>.toArray(): IntArray = IntArray(a, ::get)

fun Series<Boolean>.toArray(): BooleanArray = BooleanArray(a, ::get)

fun Series<Long>.toArray(): LongArray = LongArray(a, ::get)

fun Series<Float>.toArray(): FloatArray = FloatArray(a, ::get)

fun Series<Double>.toArray(): DoubleArray = DoubleArray(a, ::get)

fun Series<Short>.toArray(): ShortArray = ShortArray(a, ::get)

//
inline fun <reified T> Series<T>.toArray(): Array<T> = Array(size(), ::get)
fun Series<UInt>.toArray(): UIntArray = UIntArray(size(), b)

fun Series<ULong>.toArray(): ULongArray = ULongArray(size(), b)

fun Series<UByte>.toArray(): UByteArray = UByteArray(a, b)

fun Series<UShort>.toArray(): UShortArray = UShortArray(a, ::get)

fun <T> Array<T>.toSeries(): Join<Int, (Int) -> T> = size j ::get

fun IntArray.toSeries(): Join<Int, (Int) -> Int> = size j ::get
fun LongArray.toSeries(): Join<Int, (Int) -> Long> = size j ::get
fun ULongArray.toSeries(): Join<Int, (Int) -> ULong> = size j ::get
fun ByteArray.toSeries(): Join<Int, (Int) -> Byte> = size j ::get
fun UByteArray.toSeries(): Join<Int, (Int) -> UByte> = size j ::get
fun ShortArray.toSeries(): Join<Int, (Int) -> Short> = size j ::get
fun UShortArray.toSeries(): Join<Int, (Int) -> UShort> = size j ::get
fun FloatArray.toSeries(): Join<Int, (Int) -> Float> = size j ::get
fun DoubleArray.toSeries(): Join<Int, (Int) -> Double> = size j ::get
fun BooleanArray.toSeries(): Join<Int, (Int) -> Boolean> = size j ::get
fun CharArray.toSeries(): Join<Int, (Int) -> Char> = size j ::get
fun CharSequence.toSeries(): Join<Int, (Int) -> Char> = this.length j ::get
fun UIntArray.toSeries(): Join<Int, (Int) -> UInt> = size j ::get


infix fun <C, B : (Int) -> C> IntArray.α(m: B): Series<C> = this.size j { m(this[it]) }

infix fun <C, B : (Long) -> C> LongArray.α(m: B): Series<C> = this.size j { m(this[it]) }

infix fun <C, B : (Float) -> C> FloatArray.α(m: B): Series<C> = this.size j { m(this[it]) }

infix fun <C, B : (Double) -> C> DoubleArray.α(m: B): Series<C> = this.size j { m(this[it]) }

infix fun <C, B : (Short) -> C> ShortArray.α(m: B): Series<C> = this.size j { m(this[it]) }

infix fun <C, B : (Byte) -> C> ByteArray.α(m: B): Series<C> = this.size j { m(this[it]) }

infix fun <C, B : (Char) -> C> CharArray.α(m: B): Series<C> = this.size j { m(this[it]) }

infix fun <C, B : (Boolean) -> C> BooleanArray.α(m: B): Series<C> = this.size j { m(this[it]) }

fun <V> Series<V>.toSet() = this.`⏵`.toSet()
