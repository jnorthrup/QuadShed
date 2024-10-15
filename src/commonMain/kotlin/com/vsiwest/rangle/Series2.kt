package com.vsiwest.rangle

import Join
import Series
import conversion

/**
 *  Series2<A, B> is a Series of Join<A, B>
 */
typealias Series2<A, B> = Series<Join<A, B>>

val <T, I> Series2<T, I>.left: Series<T> get() = this.conversion(Join<T, I>::a)
val <T, I> Series2<I, T>.right: Series<T> get() = this.conversion(Join<I, T>::b)


//left join
operator fun <A, B> Series<Series2<A, B>>.unaryMinus(): Series<Series<A>> =
    this conversion  Series2<A, B>::left

//right join
operator fun <A, B> Series<Series2<A, B>>.unaryPlus(): Series<Series<B>> = this conversion Series2<A, B>::right