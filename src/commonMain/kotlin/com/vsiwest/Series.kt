package com.vsiwest


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
//inline fun <reified T> Series<T>.toArray(): Array<T> = Array(size(), ::get)


infix fun <C, B : (Int) -> C> IntArray.α(m: B): Series<C> = this.size j { m(this[it]) }

infix fun <C, B : (Long) -> C> LongArray.α(m: B): Series<C> = this.size j { m(this[it]) }

infix fun <C, B : (Float) -> C> FloatArray.α(m: B): Series<C> = this.size j { m(this[it]) }

infix fun <C, B : (Double) -> C> DoubleArray.α(m: B): Series<C> = this.size j { m(this[it]) }

infix fun <C, B : (Short) -> C> ShortArray.α(m: B): Series<C> = this.size j { m(this[it]) }

infix fun <C, B : (Byte) -> C> ByteArray.α(m: B): Series<C> = this.size j { m(this[it]) }

infix fun <C, B : (Char) -> C> CharArray.α(m: B): Series<C> = this.size j { m(this[it]) }

infix fun <C, B : (Boolean) -> C> BooleanArray.α(m: B): Series<C> = this.size j { m(this[it]) }

fun<V>  Series< V>.toSet()  = this.`⏵`.toSet()
