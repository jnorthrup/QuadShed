@file:Suppress("UNCHECKED_CAST") @file:OptIn(ExperimentalUnsignedTypes::class)

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



/**
 * exactly like "to" for "Join" but with a different (and shorter!) name
 */
inline infix fun <A, B> A.j(b: B): Join<A, B> = Join(this, b)


typealias Twin<T> = Join<T, T>


//fun Series<Char>.toArray(): CharArray = CharArray(a, b)
//
//fun Series<Boolean>.toArray(): BooleanArray = BooleanArray(size(), b)
//
//fun Series<Byte>.toArray(): ByteArray = ByteArray(a, b)
//
//fun Series<UByte>.toArray(): UByteArray = UByteArray(a, b)
//
//fun Series<Int>.toArray(): IntArray = IntArray(size(), b)
//
//fun Series<UInt>.toArray(): UIntArray = UIntArray(size(), b)
//
//fun Series<Long>.toArray(): LongArray = LongArray(size(), b)
//
//fun Series<ULong>.toArray(): ULongArray = ULongArray(size(), b)
//
//fun Series<Float>.toArray(): FloatArray = FloatArray(size(), ::get)
//
//fun Series<Double>.toArray(): DoubleArray = DoubleArray(size(), ::get)
//
//fun Series<Short>.toArray(): ShortArray = ShortArray(size(), ::get)

fun Series<UShort>.toArray(): UShortArray = UShortArray(size(), ::get)
inline fun <reified T> Series<T>.toArray(): Array<T> = Array(size(), ::get)

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


