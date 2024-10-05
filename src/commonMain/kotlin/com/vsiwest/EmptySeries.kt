package com.vsiwest

inline fun <N : Number, K : Comparable<N>,reified V> emptySeries(zero: K, memento: V ): MetaSeries<K, V> =
    zero.j { it: K -> memento }

val  EmptySeries = 0 j {_:Int-> TODO("Self Destruct")}

