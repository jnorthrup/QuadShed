package com.vsiwest.bitops

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