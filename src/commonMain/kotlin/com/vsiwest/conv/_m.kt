package com.vsiwest.conv

import kotlin.collections.toMap

/**
 * missing stdlib map convenience operator
 */
object _m {
    operator fun <K, V, P : kotlin.Pair<K, V>> get(p: List<P>): Map<K, V> = (p).toMap()
    operator fun <K, V, P : kotlin.Pair<K, V>> get(vararg p: P): Map<K, V> = kotlin.collections.mapOf(*p)
}
