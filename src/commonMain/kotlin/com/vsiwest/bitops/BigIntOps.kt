package com.vsiwest.bitops

object BigIntOps : BitOps<BigInt > {
    override val one: BigInteger = BigInteger.ONE
    override val xor: (BigInteger, BigInteger) -> BigInteger = BigInteger::xor
    override val and: (BigInteger, BigInteger) -> BigInteger = BigInteger::and
    override val shl: (BigInteger, Int) -> BigInteger = BigInteger::shl
    override val shr: (BigInteger, Int) -> BigInteger = BigInteger::shr
    override val plus: (BigInteger, BigInteger) -> BigInteger = BigInteger::plus
    override val minus: (BigInteger, BigInteger) -> BigInteger = BigInteger::minus
}