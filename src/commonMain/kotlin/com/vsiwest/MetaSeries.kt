@file:Suppress("NonAsciiCharacters", "UNCHECKED_CAST", "USELESS_CAST")

package com.vsiwest

import com.vsiwest.bitops.CZero.bool
import com.vsiwest.bitops.CZero.nz
import kotlinx.datetime.LocalDateTime
import kotlin.math.pow

typealias MetaSeries<K, V> = Join<K, (K) -> V>

//val <K : Comparable<K>, V> MetaSeries<K, V>.size get() = size()


operator fun <K : Comparable<K>, V> MetaSeries<K, V>.get(x: K): V = b(x)


fun <K : Comparable<K>, V> MetaSeries<K, V>.size() = a
operator fun <K : Comparable<K>, V> MetaSeries<K, V>.get(r: ClosedRange<K>) = a j { i: K -> b(a.duckPlus(r.start, i)) }

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

inline infix fun <K : Comparable<K>, X, V, M : MetaSeries<K, X>> M.α(crossinline xform: (X) -> V) =
    this conversion xform

inline infix fun <K : Comparable<K>, X, C, M : MetaSeries<K, X>> M.conversion(crossinline xform: (X) -> C) =
    a j { i: K -> xform(b(i)) }


inline val <T> T.leftIdentity: () -> T get() = { this }

/**Left Identity Function */
inline val <T> T.`↺`: () -> T get() = leftIdentity
fun <T> `↻`(t: T): T = t
infix fun <T> T.rightIdentity(t: T): T = `↻`(t)

/** execute categorical style that(this) pipe
 *  5 `•` ::get == 5.pipe(::get) == ::get(5)
 * */
infix fun <V : Any?, R2> V.pipe(e: (V) -> R2) = e(this)
infix fun <V : Any?, R2> V.`•`(e: (V) -> R2) = this pipe e  // 5 `•` ::get == 5.pipe(::get) == ::get(5)


infix fun <P1, R1, R2> ((P1) -> R1).pipe(e: (R1) -> R2): (P1) -> R2 = { p1 -> e(this(p1)) }
infix fun <P1, R1, R2> ((P1) -> R1).`•`(e: (R1) -> R2): (P1) -> R2 = this pipe e
//converter for pipe to use
/**
 * Leftwards Arrow: ← (U+2190)
 * HTML  * &larr; or &#8592;
 *
 *    reverse pipe, why not?
 *    ::get ← 5 == ::get(5)
 *
 */
infix fun <V, R1, F1 : (V) -> R1> (F1).revpipe(v: V) = ::invoke //  this `←` v   == ::get(5)

//we want a binary functor to be solved by producing a unary functor of a Join from (A,B)->C to (Join<A,B>)->C so our pipes notation
// can use  f (A,B)->C from (a j b ).invoke(f(a,b))
infix fun <A, B, C> ((A, B) -> C).invoke(j: Join<A, B>) = this(j.a, j.b)


infix fun <V, R1, F1 : (V) -> R1> (F1).`←`(v: V) = this revpipe v  //

val <N : Comparable<N>, V> MetaSeries<N, V>.`⏵`: Iterable<V> get() = this.iterable
val <N : Comparable<N>, V> MetaSeries<N, V>.iterable: Iterable<V>
    get() = object : Iterable<V> {
        val zer = a.duckZero
        override fun iterator(): Iterator<V> = object : Iterator<V> {
            var i = zer
            override fun hasNext(): Boolean = i < a
            override fun next(): V = b(i).also { i = a.duckInc(i) }
        }
    }

/**
 * Extension property to get a reversed MetaSeries.
 * This property returns a new MetaSeries where the index of the elements is inverted.
 */
val <N : Comparable<N>, V> MetaSeries<N, V>.`⏪`: MetaSeries<N, V>
    get() = reverse()

private fun <N : Comparable<N>, V> MetaSeries<N, V>.reverse() = a j {//we use duckMinus to avoid type erasure
        i: N ->
    b(a.duckMinus(a, i))
}

fun <K : Comparable<K>, V> MetaSeries<K, V>.isEmpty() = a.duckZero == a


// Currying
fun <A, B, C> ((A, B) -> C).curry(): (A) -> (B) -> C = { a -> { b -> this(a, b) } }

// Uncurrying
fun <A, B, C> ((A) -> (B) -> C).uncurry(): (A, B) -> C = { a, b -> this(a)(b) }

// Type alias for a curried function
typealias Curried<A, B, C> = (A) -> (B) -> C

// Isomorphic type checker associations
inline fun <A, B> isoCheck(
    crossinline forward: (A) -> B, crossinline backward: (B) -> A
): Join<(A) -> A, (B) -> B> = { a: A -> backward(forward(a)) } j { b: B -> forward(backward(b)) }

// Extension function to create a Series from any Iterable
fun <T> Iterable<T>.toSeries(): Series<T> = object : Series<T> {
    val list = this@toSeries.map { it }
    override val a: Int get() = list.size
    override val b: (Int) -> T get() = list::get
}

//getornull to check the key range
fun <K : Comparable<K>, V> MetaSeries<K, V>.getOrNull(key: K) = takeIf { key < a }?.let { this[key] }

// Extension function to create a partial function
fun <A, B> ((A) -> B).partial(a: A): () -> B = { this(a) }

// Extension function for function composition
infix fun <A, B, C> ((A) -> B).andThen(f: (B) -> C): (A) -> C = { a -> f(this(a)) }

// Convenience function for creating a Series from a range
fun seriesOf(range: IntRange): Series<Int> = object : Series<Int> {
    override val a: Int get() = range.last - range.first + 1
    override val b: (Int) -> Int get() = { i -> range.first + i }
}

// Extension function to transform a Series into a lazy sequence
fun <T> Series<T>.asSequence(): Sequence<T> = sequence {
    for (i in 0 until a) yield(b(i))
}


fun Series<Char>.parseIsoDateTime(): LocalDateTime {
    val ad = a.duckZero
    val year = this[ad..3].parseLong().toInt()
    val month = this[5..6].parseLong().toInt()
    val day = this[8..9].parseLong().toInt()
    val hour = this[11..12].parseLong().toInt()
    val minute = this[14..15].parseLong().toInt()
    val second = this[17..18].parseLong().toInt()
    val nanosecond = this[20..26].parseLong().toInt()
    return LocalDateTime(year, month, day, hour, minute, second, nanosecond)
}

fun <K : Comparable<K>> MetaSeries<K, Char>.parseLong(): Long {
//handles +-
    var sign = 1L
    var x = a.duckZero
    when (this[a.duckZero]) {
        '-' -> {
            sign = -1L
            x = a.duckInc(x)
        }

        '+' -> x = a.duckInc(x)
    }
    var r = 0L
    while (x < a) {
        r = r * 10 + (this[x] - '0')
        x = a.duckInc(x)
    }
    return r * sign
}

/** parse a double or throw an exception
 *
 * @return Double
 */
fun <K : Comparable<K>> MetaSeries<K, Char>.parseDouble(): Double {
    var x = a.duckZero
    var isNegative = false
    var result = 0.0
    var hasDecimal = false
    var exponentSign = 1
    var exponentValue = 0
    var digitsAfterDecimal = 0

    when (this[x]) {
        '-' -> {
            isNegative = true; x = x.duckInc(x)
        }

        '+' -> x = x.duckInc(x)
    }

    var afterE = false
    while (x < size()) when (val c = this[x]) {
        'E', 'e' -> {
            require(!afterE) { "Invalid second exponent" }
            afterE = true
            x = x.duckInc(x)
            val c = this[x]
            if (c == '-') {
                exponentSign = -1; x = x.duckInc(x)
            } else if (c == '+') x = x.duckInc(x)
            while (x < size() && this[x] in '0'..'9') {
                exponentValue = exponentValue * 10 + (this[x] - '0')
                x = x.duckInc(x)
            }
        }

        '.' -> {
            require(!hasDecimal) { "Invalid second decimal point" }
            require(!afterE) { "Invalid decimal point behind exponent" }
            hasDecimal = true
            x = x.duckInc(x)
        }

        in '0'..'9' -> {
            result = result * 10 + (c - '0')
            if (hasDecimal) digitsAfterDecimal++
            x = x.duckInc(x)
        }

        else -> throw NumberFormatException("Invalid character at '$c'")
    }
    val signMultiplier = if (isNegative && result != 0.0) -1.0 else 1.0
    return signMultiplier * result * 10.0.pow((exponentSign * exponentValue - digitsAfterDecimal).toDouble())
}

fun <K : Comparable<K>, V> MetaSeries<K, V>.take(i: K) = i j b
fun <K : Comparable<K>, V> MetaSeries<K, V>.takeLast(i: K) = i j { it: K -> b(a.duckMinus(a, it)) }

fun <K : Comparable<K>, V> MetaSeries<K, V>.drop(i: K) = a.duckMinus(a, i) j { it: K -> b(a.duckPlus(a, it)) }
fun <K : Comparable<K>, V> MetaSeries<K, V>.dropLast(i: K) = a.duckMinus(a, i) j b


fun <K : Comparable<K>, V> MetaSeries<K, V>.toSeries(): Series<V> =
     (when (a) {
        is Boolean ->  ({ it: Boolean -> it.bool }j  { it: Int -> it.nz as Boolean })as Join<((K) -> Int), ((Int) -> K)>
        is Byte ->  (Byte::toInt j Int::toByte) as Join<((K) -> Int), ((Int) -> K)>
        is Short ->  (Short::toInt j Int::toShort) as Join<((K) -> Int), ((Int) -> K)>
        is Char ->  (Char::code j Int::toChar) as Join<((K) -> Int), ((Int) -> K)>
        is Long ->  (Long::toInt j Int::toLong) as Join<((K) -> Int), ((Int) -> K)>
        is UByte ->  (UByte::toInt j Int::toUByte) as Join<((K) -> Int), ((Int) -> K)>
        is UShort ->  (UShort::toInt j Int::toUShort) as Join<((K) -> Int), ((Int) -> K)>
        is UInt ->  (UInt::toInt j Int::toUInt) as Join<((K) -> Int), ((Int) -> K)>
        is ULong ->  (ULong::toInt j Int::toULong) as Join<((K) -> Int), ((Int) -> K)>
        else -> throw IllegalArgumentException("Unsupported type")
    }.let { thing ->

         val (ti: (K) -> Int, fi: (Int) -> K) = thing

         ti(a) j { i: Int -> b(fi(i)) }
    }   as  Series<V>)
