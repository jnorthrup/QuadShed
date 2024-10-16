package com.vsiwest.rangle


interface Join<A, B> {
    val a: A
    val b: B
}

typealias Series<A> = Join<Int, (Int) -> A>
typealias Series2<A,B> = Series<Join<A,B>>
typealias MetaSeries<K, V> = Join<K, (K) -> V>

infix fun <A, B> A.j(other: B) = object : Join<A, B> {
    override val a: A = this@j
    override val b: B = other
}

fun <V, C> Series<V>.conversion(f: (V) -> C): Series<C> = a j { i -> f(b(i)) }
val <T> Series<T>.size: Int get() = a

operator fun <T> Series<T>.get(i: Int): T = b(i)

operator fun <T> MetaSeries<Int, T>.get(r: IntRange): MetaSeries<Int, T> {
    val count = r.count()
    return (count j { x: Int -> this[(r.first + x)] })
}

//iterable
inline class IterableSeries<V>(val series: Series<V>) : Iterable<V> {
    override fun iterator(): Iterator<V> = object : Iterator<V> {
        var i = 0
        override fun hasNext(): Boolean = i < series.size
        override fun next(): V = series[i++]
    }
}
fun <V> Series<V>.asIterable(): Iterable<V> = IterableSeries(this)

//series toArray
inline fun <reified V> Series<V>.toArray()= Array(size) { i:Int -> this[i] }
inline fun Series<Boolean>.toArray()= BooleanArray(size) { i:Int -> this[i] }
inline fun Series<Int>.toArray()= IntArray(size) { i:Int -> this[i] }
inline fun Series<Long>.toArray()= LongArray(size) { i:Int -> this[i] }
inline fun Series<Float>.toArray()= FloatArray(size) { i:Int -> this[i] }
inline fun Series<Double>.toArray()= DoubleArray(size) { i:Int -> this[i] }
inline fun Series<Char>.toArray()= CharArray(size) { i:Int -> this[i] }
inline fun Series<Byte>.toArray()= ByteArray(size) { i:Int -> this[i] }
inline fun Series<Short>.toArray()= ShortArray(size) { i:Int -> this[i] }
inline fun Series<UByte>.toArray()= UByteArray(size) { i:Int -> this[i] }
inline fun Series<UShort>.toArray()= UShortArray(size) { i:Int -> this[i] }
inline fun Series<UInt>.toArray()= UIntArray(size) { i:Int -> this[i] }
inline fun Series<ULong>.toArray()= ULongArray(size) { i:Int -> this[i] }


val <V:Comparable<V>> V.networkBits:Int? get()=null
val Boolean.networkBits: Int get()=1
val Byte.networkBits  get()=Byte.SIZE_BITS
val Short.networkBits get()=Short.SIZE_BITS
val Char.networkBits get()=Char.SIZE_BITS
val Int.networkBits get()=Int.SIZE_BITS
val Long.networkBits get()=Long.SIZE_BITS
val Float.networkBits get()=Float.SIZE_BITS
val Double.networkBits get()=Double.SIZE_BITS

fun<V:Comparable<V>>V.toRegisterMask(r:Int=64, vwidth:Int?=(this::networkBits )())  :Series<Boolean>? =if(this is Number) {

    this.toLong().toRegisterMask(r,vwidth)
} else null

fun Long.toRegisterMask(r:Int=64, vwidth:Int?=networkBits):Series<Boolean> = r j { i -> i < vwidth!! && (this shr i and 1L) == 1L }

