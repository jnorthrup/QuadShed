@file:Suppress("NonAsciiCharacters")

package com.vsiwest

typealias MetaSeries<K, V> = Join<K, (K) -> V>

operator fun <S, K : Comparable<K>> MetaSeries<K, S>.get(x: K): S = b(x)

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

inline infix fun <N : Number, K : Comparable<N>, X, C, V : MetaSeries<K, X>> V.α(crossinline xform: (X) -> C)       = this conversion xform
inline infix fun <N : Number, K : Comparable<N>, X, C, V : MetaSeries<K, X>> V.conversion(crossinline xform: (X) -> C)   =a j { i: K -> xform(b(i)) }

//val <K: Comparable<Int>,V> MetaSeries<K,V>.size:K get() =a
val <K : Number, V> MetaSeries<K, V>.size: K get() = a
inline val <T> T.leftIdentity: () -> T get() = { this }

/**Left Identity Function */
inline val <T> T.`↺`: () -> T get() = leftIdentity
fun <T> `↻`(t: T): T = t
infix fun <T> T.rightIdentity(t: T): T = `↻`(t)

/** execute categorical style that(this) pipe
 *  5 `•` ::get == 5.pipe(::get) == ::get(5)
 * */
infix fun <V:Any?, R2> V.pipe(e: (V)->R2): R2 = e(this)
infix fun <V:Any?, R2> V.`•`(e: (V)->R2): R2 = this pipe e  // 5 `•` ::get == 5.pipe(::get) == ::get(5)

/**
 * Leftwards Arrow: ← (U+2190)
 * HTML  * &larr; or &#8592;
 *
 *    reverse pipe, why not?
 *    ::get ← 5 == ::get(5)
 *
 */
infix fun <V,R1, F1:(V)->R1> (F1).revpipe(v:V) = ::invoke //  this `←` v   == ::get(5)
infix fun <V,R1, F1:(V)->R1> (F1).`←`(v:V) =   this revpipe v  //
val <N : Number, K : Comparable<N>, V> MetaSeries<K, V>.`▶`: Iterable<V>
    get() = object : Iterable<V> {
        val zer=(a as K).zero
        override fun iterator(): Iterator<V> = object : Iterator<V> {
            var i = zer
            override fun hasNext(): Boolean = i < a  as N
            override fun next(): V = b(i).also { i = a.duckInc(i) }
        }
    }
