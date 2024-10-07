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
inline fun <reified T> Twin(a: T, b: T): Twin<T> = a j b



/**
 * exactly like "to" for "Join" but with a different (and shorter!) name
 */
 infix fun <A, B> A.j(b: B): Join<A, B> = Join(this, b)


typealias Twin<T> = Join<T, T>



