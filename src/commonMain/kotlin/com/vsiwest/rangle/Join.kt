import com.vsiwest.rangle.duckConvert
import com.vsiwest.rangle.duckInc
import com.vsiwest.rangle.duckMinus
import com.vsiwest.rangle.duckPlus
import kotlin.jvm.JvmInline

typealias MetaSeries<K, V> = Join<K, (K) -> V>

infix fun <K : Comparable<K>, V, C> MetaSeries<K, V>.convert(lens: (V) -> C) = a j { it: K -> lens(b(it)) }

inline fun <K : Comparable<K>, reified V> MetaSeries<K, V>.toArray(): Array<V> =
    Array(0.duckConvert(a)) { it: Int -> b(a.duckConvert(it)) }

inline fun <K : Comparable<K>, reified V> MetaSeries<K, V>.toList(): AbstractList<V> = object : AbstractList<V>() {
    override val size: Int get() = 0.duckConvert(a)
    override fun get(index: Int): V = b(a.duckConvert(index))
}


val <K>MetaSeries<K, *>.size get() = a

typealias Series <V> = Join<Int, (Int) -> V>

interface Join<A, out B> {
    val a: A
    val b: B
}

// a j b = Join<A, B>
infix fun <A, B> A.j(b: B): Join<A, B> = object : Join<A, B> {
    override val a: A = this@j
    override val b: B = b
}

// Flexible get function for MetaSeries
inline operator fun <K : Comparable<K>, V, P : Comparable<P>> MetaSeries<K, V>.get(index: P): V {
    val convertedIndex: K = this.a.duckConvert(index)
    return b(convertedIndex)
}


/** α
 * (λx.M[x]) → (λy.M[y])	α-conversion
 * https://en.wikipedia.org/wiki/Lambda_calculus
 *
 * in kotlin terms, λ above is a lambda expression and M is a function and the '.' is the body of the lambda
 * therefore the function M is the receiver of the extension function and the lambda expression is the argument
 *
 *  the simplest possible kotlin example of λx.M[x] is
 *  ` { x -> M(x) } ` making the delta symbol into lambda braces and the x into a parameter and the M(x) into the body
 */

inline infix fun <X, C, V : Series<X>> V.conversion(crossinline xform: (X) -> C): Series<C> = size j { i -> xform(this[i]) }

/*iterable conversion*/
infix fun <X, C, Subject : Iterable<X>> Subject.conversion(xform: (X) -> C) = object : Iterable<C> {
    override fun iterator(): Iterator<C> = object : Iterator<C> {
        val iter: Iterator<X> = this@conversion.iterator()
        override fun hasNext(): Boolean = iter.hasNext()
        override fun next(): C = xform(iter.next())
    }
}


/** this is an alpha conversion however the type erasure forces inlining here for Arrays as a holdover from java
 *   */
inline infix fun <X, C> Array<X>.conversion(crossinline xform: (X) -> C): Series<C> = size j { i: Int -> xform(this[i]) }

//metaseries get(closedreange)
inline operator fun <K : Comparable<K>, V> MetaSeries<K, V>.get(range: ClosedRange<K>) =
//the range length, converted to the type of the series
    range.run {
        val expans = endInclusive.duckMinus(endInclusive, start)

        //find size from range
        val size = expans.duckInc(expans)

        //return a new series with the range
        a j { i: K -> b(i.duckPlus(i, start)) }
    }


val <K>K.networkBits: Int? get() = null


val <K : Comparable<K>>K.networkBits: Int?
    get() = when (this) {
        is Boolean -> 1
        is Byte -> 8
        is Short -> 16
        is Int -> 32
        is Long -> 64
        is Float -> 32
        is Double -> 64
        is Char -> 16
        else -> null
    }


infix fun <A, B> A.jb(b: B): Join<A, B> = object : Join<A, B> {
    override val a: A = this@jb
    override val b: B = b
}

operator fun <K : Comparable<K>, V> MetaSeries<K, V>.iterator(): Iterator<V> = MetaSeriesIterable(this).iterator()


@JvmInline
value class MetaSeriesIterable<K : Comparable<K>, V>(val series: MetaSeries<K, V>) : Iterable<V>,
    Join<K, (K) -> V> by series {
    override fun iterator(): Iterator<V> = object : Iterator<V> {
        var index = a
        override fun hasNext(): Boolean = a > index
        override fun next(): V = b(index).also { index = index.duckInc(index) }
    }
}

/**
 * A comparable wrapper for MetaSeriesIterable
 */
@JvmInline
value class MetaSeriesComparable<K : Comparable<K>, V>(val series: MetaSeriesIterable<K, V>) :
    Comparable<MetaSeriesComparable<K, V>>, Join<K, (K) -> V> by series {
    override fun compareTo(other: MetaSeriesComparable<K, V>): Int {
        val iterator = series.iterator()
        val otherIterator = other.series.iterator()
        while (iterator.hasNext() && otherIterator.hasNext()) {
            val next = iterator.next()
            val otherNext = otherIterator.next()
            if (next != otherNext) {
                return next.toString().compareTo(otherNext.toString())
            }
        }
        return 0
    }


}

/**Let's consider a MetaSeries of Byte values (8 bits each) in a 64-bit register:

Size: 7 (encoded as 6, requires 3 bits)
Values: v1, v2, v3, v4, v5, v6, v7 (each 8 bits)

Bit layout:
```
63  61 60    53 52    45 44    37 36    29 28    21 20    13 12     5 4      0
v   v v      v v      v v      v v      v v      v v      v v      v v      v
+---+--------+--------+--------+--------+--------+--------+--------+--------+
|110|vvvvvvvv|vvvvvvvv|vvvvvvvv|vvvvvvvv|vvvvvvvv|vvvvvvvv|vvvvvvvv|unused  |
+---+--------+--------+--------+--------+--------+--------+--------+--------+
^   ^        ^        ^        ^        ^        ^        ^        ^
|   |        |        |        |        |        |        |        |
|   |        |        |        |        |        |        |        +-- Unused bits (5 bits)
|   |        |        |        |        |        |        +----------- v7 (8 bits)
|   |        |        |        |        |        +-------------------- v6 (8 bits)
|   |        |        |        |        +----------------------------- v5 (8 bits)
|   |        |        |        +------------------------------------ v4 (8 bits)
|   |        |        +------------------------------------------- v3 (8 bits)
|   |        +-------------------------------------------------- v2 (8 bits)
|   +--------------------------------------------------------- v1 (8 bits)
+-------------------------------------------------------------- Encoded size (6 = 7-1, 3 bits)
```
Explanation:

1. Size encoding:
- Actual size is 7, encoded as 6 (7-1)
- 6 in binary is 110, which requires 3 bits
- These 3 bits are placed in the most significant bits: 110

2. Value packing:
- Each Byte value requires 8 bits
- We can fit 7 complete Bytes (56 bits) in the remaining 61 bits
- The remaining 5 bits are unused in this 64-bit word

3. Efficient use of space:
- This layout allows us to pack 7 Byte values into a single 64-bit word
- Only 5 bits are unused, maximizing the use of available space

Decoding process:

1. Extract size: Read the first 3 bits (110), add 1 to get 7
2. Extract values: Read each subsequent 8-bit segment for values v1 through v7

the register has no use for K at decode time so no info is implied about K

 when a reification doesn't fit or is not pure bits, the return is null


 */
inline fun <K : Comparable<K>, reified V : Comparable<V>> MetaSeriesComparable<K, V>.reify(r: Int = 64): Join<K, (K) -> V>? {
    //determine sizebits
    val sizeBits = a.


}



