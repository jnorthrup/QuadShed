package com.vsiwest

val <N : Number, K : Comparable<N>> MetaSeries<K, *>.xor: (K, K) -> K
    get() = when (this.a) {
        is Int -> Int::xor
        is Long -> Long::xor
        is Short -> ::xor
        is Byte -> ::xor
        is UInt -> UInt::xor
        is ULong -> ULong::xor
        is UShort -> UShort::xor
        is UByte -> UByte::xor
        else -> TODO("handle keys like $a")
    } as (K, K) -> K
val <N : Number, K : Comparable<N>> K.zero: K
    get() = when (this) {
        is Int -> 0
        is Long -> 0L
        is Short -> 0.toShort()
        is Byte -> 0.toByte()
        is UInt -> 0u
        is ULong -> 0uL
        is UShort -> 0u.toUShort()
        is UByte -> 0u.toUByte()
        else -> TODO()
    } as K
val <N : Number, K : Comparable<N>>K.duckInc: (K) -> K
    get() =
        when (this) {
            is Int -> Int::inc
            is Long -> Long::inc
            is Short -> Short::inc
            is Byte -> Byte::inc
            is UInt -> UInt::inc
            is ULong -> ULong::inc
            is UShort -> UShort::inc
            is UByte -> UByte::inc
            else -> TODO("poof")
        } as (K) -> K
val <N : Number, K : Comparable<N>>K.duckDec: (K) -> K
    get() =
        when (this) {
            is Int -> Int::dec
            is Long -> Long::dec
            is Short -> Short::dec
            is Byte -> Byte::dec
            is UInt -> UInt::dec
            is ULong -> ULong::dec
            is UShort -> UShort::dec
            is UByte -> UByte::dec
            else -> TODO("poof")
        } as (K) -> K