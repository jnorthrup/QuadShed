package com.vsiwest

interface   VersionedSeries<T> :Series<T>{
    val version: Long?  // null means external object Identity as version
}