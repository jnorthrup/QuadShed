package com.vsiwest

inline fun <  reified K : Comparable<K>,reified V> emptySeries(zero: K, memento: V ): MetaSeries<K, V> =
    zero.j { it: K -> memento }

val  EmptySeries = 0 j {_:Int-> TODO("Self Destruct")}

