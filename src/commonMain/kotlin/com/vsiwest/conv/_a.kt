@file:Suppress("OPT_IN_USAGE", "UNCHECKED_CAST")

package com.vsiwest.conv

object _a {
    operator fun get(vararg t: Boolean): BooleanArray = t
    operator fun get(vararg t: Byte): ByteArray = t
    operator fun get(vararg t: kotlin.UByte): kotlin.UByteArray = t
    operator fun get(vararg t: Char): CharArray = t
    operator fun get(vararg t: Short): ShortArray = t
    operator fun get(vararg t: kotlin.UShort): kotlin.UShortArray = t
    operator fun get(vararg t: Int): IntArray = t
    operator fun get(vararg t: kotlin.UInt): kotlin.UIntArray = t
    operator fun get(vararg t: Long): LongArray = t
    operator fun get(vararg t: kotlin.ULong): kotlin.ULongArray = t
    operator fun get(vararg t: Float): FloatArray = t
    operator fun get(vararg t: Double): DoubleArray = t
    inline operator fun <reified T> get(vararg t: T): Array< T> = t as Array<T>
}
