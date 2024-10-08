package com.vsiwest.plaf

import com.vsiwest.MetaSeries
import com.vsiwest.Series
import com.vsiwest.drop
import com.vsiwest.j
import com.vsiwest.take

/** Series with long Indexes for large files */
typealias LongSeries<T> = MetaSeries<Long, T>

fun <A> Series<A>.toLongSeries(): LongSeries<A> = a.toLong() j { it: Long -> b(it.toInt()) }
fun <V> LongSeries<V>.toSeries() = a.toInt() j { i: Int -> b(i.toLong()) }
fun <V> LongSeries<V>.get(intRange: IntRange): Series<V> =
    drop(intRange.first.toLong()).take((intRange.endInclusive - intRange.start).toLong()).toSeries()