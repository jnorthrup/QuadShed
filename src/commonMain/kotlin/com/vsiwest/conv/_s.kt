package com.vsiwest.conv

//
// /**
// * missing stdlib set operator https://github.com/Kotlin/KEEP/pull/112
// */
object _s {
      operator fun <T> get(vararg t: T): Set<T> = kotlin.collections.setOf(*t)
}
