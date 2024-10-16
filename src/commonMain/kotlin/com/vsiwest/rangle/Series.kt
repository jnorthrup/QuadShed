interface Join<A, B> {
    val a: A
    val b: B
}

typealias Series<A> = Join<Int, (Int) -> A>
typealias Series2<A, B> = Series<Join<A, B>>
typealias MetaSeries<K, V> = Join<K, (K) -> V>

infix fun <A, B> A.j(other: B) = object : Join<A, B> {
    override val a: A = this@j
    override val b: B = other
}

//for a j b j c j d we want consistent Join<A, Join<B, Join<C, D>>> structure not Join<Join<Join<A, B>, C>, D>
infix fun <A, B, C> Join<A, B>.j(other: C) = object : Join<A, Join<B, C>> {
    override val a: A = this@j.a
    override val b: Join<B, C> = this@j.b j other
}

fun <V, C> Series<V>.conversion(f: (V) -> C): Series<C> = a j { i -> f(b(i)) }
val <T> Series<T>.size: Int get() = a

operator fun <T> Series<T>.get(i: Int): T = b(i)

operator fun <T> MetaSeries<Int, T>.get(r: IntRange): MetaSeries<Int, T> = r.count() j { x -> this[r.first + x] }

inline class IterableSeries<V>(val series: Series<V>) : Iterable<V> {
    override fun iterator(): Iterator<V> = object : Iterator<V> {
        var i = 0
        override fun hasNext() = i < series.size
        override fun next() = series[i++]
    }
}

fun <V> Series<V>.asIterable(): Iterable<V> = IterableSeries(this)

inline fun <reified V> Series<V>.toArray() = Array(size) { this[it] }
inline fun Series<Boolean>.toArray() = BooleanArray(size) { this[it] }
inline fun Series<Int>.toArray() = IntArray(size) { this[it] }
inline fun Series<Long>.toArray() = LongArray(size) { this[it] }
inline fun Series<Float>.toArray() = FloatArray(size) { this[it] }
inline fun Series<Double>.toArray() = DoubleArray(size) { this[it] }
inline fun Series<Char>.toArray() = CharArray(size) { this[it] }
inline fun Series<Byte>.toArray() = ByteArray(size) { this[it] }
inline fun Series<Short>.toArray() = ShortArray(size) { this[it] }

val <V : Comparable<V>> V.networkBits: Int? get() = null
val Boolean.networkBits: Int get() = 1
val Byte.networkBits get() = Byte.SIZE_BITS
val Short.networkBits get() = Short.SIZE_BITS
val Char.networkBits get() = Char.SIZE_BITS
val Int.networkBits get() = Int.SIZE_BITS
val Long.networkBits get() = Long.SIZE_BITS
val Float.networkBits get() = Float.SIZE_BITS
val Double.networkBits get() = Double.SIZE_BITS

fun <V : Comparable<V>> V.toRegisterMask(r: Int = 64, vwidth: Int?): Series<Boolean>? {
    return if (this is Number) this.toLong().toRegisterMask(r, vwidth) else null
}

fun <V : Comparable<V>> Series<V>.toRegisterMask(
    r2: Int = 64,
    vwidth: Int? = b(a.dec()).networkBits,
): Join<Long, Join<Join<Int, () -> Join<Int, (Int) -> BooleanArray>>, () -> Join<Int, (Int) -> Long>>>? {
    var r: Int = r2
    val sizeInlayBits = r - a.toLong().dec().countLeadingZeroBits()
    val capacity = (r - sizeInlayBits) / (vwidth ?: return null)
    if (a > capacity) return null

    val newr = sizeInlayBits + vwidth * a
    r = newr
    var reg = a.toLong().dec() shl (r - sizeInlayBits)

    asIterable().forEachIndexed { index, v ->
        v.toRegisterMask(r, vwidth)?.let { mask: Series<Boolean> ->
            reg = reg or (mask.a.toLong() shl (r - sizeInlayBits - vwidth!! * index))
        } ?: return null
    }

    return reg j r j {
        a j { x: Int ->
            val l: Long = reg shl (sizeInlayBits + vwidth * x) shr (r - vwidth)
            BooleanArray(vwidth) { i -> l and (1L shl i) != 0L }
        }
    } j {
        a j { x: Int ->
            reg shl (sizeInlayBits + vwidth * x) shr (r - vwidth)
        }
    }
}


