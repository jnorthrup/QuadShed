@file:Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
@file:OptIn(ExperimentalUnsignedTypes::class)

package com.vsiwest.bitops

import com.vsiwest.*
import com.vsiwest.bitops.CZero.z
import gk.kademlia.bitops.impl.*

interface BitOps<Primitive : Comparable<Primitive>> {
    val one: Primitive
    val shl: (Primitive, Int) -> Primitive
    val shr: (Primitive, Int) -> Primitive
    val xor: (Primitive, Primitive) -> Primitive
    val and: (Primitive, Primitive) -> Primitive
    val plus: (Primitive, Primitive) -> Primitive
    val minus: (Primitive, Primitive) -> Primitive
    fun toNumber(x: Primitive): Number = x.let {
        when (one) {
            is Number -> it as Number
            is UByte -> (it as UByte).toInt()
            is UShort -> (it as UShort).toInt()
            is UInt -> (it as UInt).toLong()
            else -> BigInteger((it).toString())//megamorphic call
        }
    }

    companion object {
        /**
         * minimum bitops types for the intended bitcount of NUID
         */
        fun minOps(size: Int): BitOps<*> = when (size) {
            in Int.MIN_VALUE..7 -> ByteBitOps
            8 -> UByteBitOps
            in 9..15 -> ShortBitOps
            16 -> UShortBitOps
            in 17..31 -> IntBitOps
            32 -> UIntBitOps
            in 33..63 -> LongBitOps
            64 -> ULongBitOps
            else -> BigIntOps
        }

    }
}


typealias BigInteger = BigInt


object CZero {
    val Byte.z: Boolean get() = 0 == this.toInt()
  inline  val Short.z: Boolean get() = 0 == this.toInt()
  inline  val Char.z: Boolean get() = 0 == this.code
  inline  val Int.z: Boolean get() = 0 == this
  inline  val Long.z: Boolean get() = 0L == this
  inline  val UByte.z: Boolean get() = 0 == this.toInt()
  inline  val UShort.z: Boolean get() = 0 == this.toInt()
  inline  val UInt.z: Boolean get() = 0U == this
  inline  val ULong.z: Boolean get() = 0UL == this
  inline  val Boolean.bool: Int get() = if (this) 1 else 0
  inline  val Boolean.z: Boolean get() = bool.z
  inline  val Byte.nz: Boolean get() = !z
  inline  val Short.nz: Boolean get() = !z
  inline  val Char.nz: Boolean get() = !z
  inline  val Int.nz: Boolean get() = !z
  inline  val Long.nz: Boolean get() = !z
  inline  val UByte.nz: Boolean get() = !z
  inline  val UShort.nz: Boolean get() = !z
  inline  val UInt.nz: Boolean get() = !z
  inline  val ULong.nz: Boolean get() = !z
  inline  val Boolean.nz: Boolean get() = !z
}


inline fun <N : Number, K : Comparable<N>> MetaSeries<K, *>.isEmpty() = ((a as? Byte)?.z) ?:      //
((a as? Short)?.z) ?:          //
((a as? Char)?.z) ?:           //
((a as? Int)?.z) ?:            //
((a as? Long)?.z) ?:           //
((a as? UByte)?.z) ?:          //
((a as? UShort)?.z) ?:         //
((a as? UInt)?.z) ?:           //
((a as? ULong)?.z) ?:          //
((a as? Boolean)?.z) ?: false  //
