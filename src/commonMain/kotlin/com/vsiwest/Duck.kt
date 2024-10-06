@file:Suppress("UNCHECKED_CAST")

package com.vsiwest

import kotlin.experimental.xor

val <N : Comparable<N>>  N.duckXor: (N, N) -> N
    get()= when (this ) {
        is Int -> Int::xor
        is Long -> Long::xor
        is Short -> Short::xor
        is Byte -> Byte::xor
        is UInt -> UInt::xor
        is ULong -> ULong::xor
        is UShort -> UShort::xor
        is UByte -> UByte::xor
        else -> TODO("handle keys like $this")
    } as (N, N) -> N


val < N : Comparable<N>> N.duckZero: N
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
    } as N
val < N : Comparable<N  >>N.duckInc: (N) -> N
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
        } as (N) -> N

val < N : Comparable<N>>N.duckDec: (N) -> N
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
        } as (N) -> N


val <N : Comparable<N>> N.duckMinus: (N,N) -> N
    get() = when (this) {
        is Int -> {me:Int, them: Int -> (me - them.toInt()) }
        is Long -> {me:Long, them: Long -> (me - them.toLong()) }
        is Short -> { me:Short,them: Short -> (me - them.toShort()) }
        is Byte -> {me:Byte, them: Byte -> (me - them.toByte()) }
        is UInt -> {me:UInt, them: UInt -> (me - them.toUInt()) }
        is ULong -> {me:ULong, them: ULong -> (me - them.toULong()) }
        is UShort -> { me:UShort,them: UShort -> (me - them.toUShort()) }
        is UByte -> {me:UByte, them: UByte -> (me - them.toUByte()) }
        else -> throw IllegalArgumentException("Unsupported type ${this::class}")
    } as (N,N) -> N


val <N : Comparable<N>> N.duckPlus: (N,N) -> N get()= when (this) {
    is Int -> {me:Int, them: Int -> (me + them.toInt()) }
    is Long -> {me:Long, them: Long -> (me + them.toLong()) }
    is Short -> { me:Short,them: Short -> (me + them.toShort()) }
    is Byte -> {me:Byte, them: Byte -> (me + them.toByte()) }
    is UInt -> {me:UInt, them: UInt -> (me + them.toUInt()) }
    is ULong -> {me:ULong, them: ULong -> (me + them.toULong()) }
    is UShort -> { me:UShort,them: UShort -> (me + them.toUShort()) }
    is UByte -> {me:UByte, them: UByte -> (me + them.toUByte()) }
    else -> throw IllegalArgumentException("Unsupported type ${this::class}")
} as (N,N) -> N

