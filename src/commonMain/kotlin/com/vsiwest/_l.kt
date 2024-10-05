package com.vsiwest

object _l {
    operator fun <T> get(vararg t: T): List<T> = kotlin.collections.listOf(*t)
}

