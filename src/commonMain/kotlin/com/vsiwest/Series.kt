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
