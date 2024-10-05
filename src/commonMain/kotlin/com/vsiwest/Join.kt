@file:Suppress("UNCHECKED_CAST")

package com.vsiwest

/**
 * Joins two things.  Pair semantics but distinct in the symbol naming
 */
interface Join<A, B> {
    val a: A
    val b: B
    operator fun component1(): A = a
    operator fun component2(): B = b

    val pair: Pair<A, B>
        get() = Pair(a, b)

    /** debugger hack only, violates all common sense */
    val list: List<Any?> get() = (this as? Series<out Any?>)?.toList() ?: emptyList()

    companion object {
        //the Join factory method
        operator fun <A, B> invoke(a: A, b: B): Join<A, B> = object : Join<A, B> {
            override inline val a: A get() = a
            override inline val b: B get() = b
        }

        //the Pair factory method
        operator fun <A, B> invoke(pair: Pair<A, B>): Join<A, B> = object : Join<A, B> {
            override val a: A get() = pair.first
            override val b: B get() = pair.second
        }


        //the Map factory method
        operator fun <A, B> invoke(map: Map<A, B>): Series<Join<A, B>> = object : Series<Join<A, B>> {
            override val a: Int get() = map.size
            override val b: (Int) -> Join<A, B> get() = { map.entries.elementAt(it).let { Join(it.key, it.value) } }
        }

        fun <B> emptySeriesOf(): Series<B> = EmptySeries as Series<B>
    }
}


//Twin factory method
inline fun <T> Twin(a: T, b: T): Twin<T> = a j b


inline val <A> Join<A, *>.first: A get() = this.a
inline val <B> Join<*, B>.second: B get() = this.b

/**
 * exactly like "to" for "Join" but with a different (and shorter!) name
 */
inline infix fun <A, B> A.j(b: B): Join<A, B> = Join(this, b)


typealias Twin<T> = Join<T, T>


fun Series<Byte>.toArray(): ByteArray = ByteArray(a, b)

fun Series<Char>.toArray(): CharArray = CharArray(a, b)

fun Series<Int>.toArray(): IntArray = IntArray(size, b)

fun Series<Boolean>.toArray(): BooleanArray = BooleanArray(size, b)

fun Series<Long>.toArray(): LongArray = LongArray(size, b)

fun Series<Float>.toArray(): FloatArray = FloatArray(size, ::get)

fun Series<Double>.toArray(): DoubleArray = DoubleArray(size, ::get)

fun Series<Short>.toArray(): ShortArray = ShortArray(size, ::get)

inline fun <reified T> Series<T>.toArray(): Array<T> = Array(size, ::get)

fun <T> Array<T>.toSeries(): Join<Int, (Int) -> T> = size j ::get








