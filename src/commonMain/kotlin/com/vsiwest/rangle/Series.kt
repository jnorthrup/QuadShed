@file:Suppress("UNCHECKED_CAST")

package com.vsiwest.rangle

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.experimental.or
import kotlin.jvm.JvmStatic

interface Join<A, B> {
    val a: A
    val b: B
}

typealias Series<A> = Join<Int, (Int) -> A>
typealias Series2<A, B> = Series<Join<A, B>>
typealias MetaSeries<K, V> = Join<K, (K) -> V>

infix fun <A, B> A.j(other: B) = object : Join<A, B> {
    override val a: A = this@j
    override val b: B = other
}



infix fun <A, B> A.of(other: B) = object : Join<B, A> {
    override val a: B = other
    override val b: A = this@of
}


fun <V, C> Series<V>.conversion(f: (V) -> C): Series<C> = a j { i -> f(b(i)) }
val <T> Series<T>.size: Int get() = a

operator fun <T> Series<T>.get(i: Int): T = b(i)

operator fun <T> MetaSeries<Int, T>.get(r: IntRange): MetaSeries<Int, T> = r.count() j { x -> this[r.first + x] }

inline class IterableSeries<V>(val series: Series<V>) : Iterable<V> {
    override fun iterator(): Iterator<V> = object : Iterator<V> {
        var i = 0
        override fun hasNext() = i < series.size
        override fun next() = series[i++]
    }
}

fun <V> Series<V>.asIterable(): Iterable<V> = IterableSeries(this)

inline fun <reified V> Series<V>.toArray() = Array(size) { this[it] }
inline fun Series<Boolean>.toArray() = BooleanArray(size) { this[it] }
inline fun Series<Int>.toArray() = IntArray(size) { this[it] }
inline fun Series<Long>.toArray() = LongArray(size) { this[it] }
inline fun Series<Float>.toArray() = FloatArray(size) { this[it] }
inline fun Series<Double>.toArray() = DoubleArray(size) { this[it] }
inline fun Series<Char>.toArray() = CharArray(size) { this[it] }
inline fun Series<Byte>.toArray() = ByteArray(size) { this[it] }
inline fun Series<Short>.toArray() = ShortArray(size) { this[it] }

val <V : Comparable<V>> V.networkBits: Int? get() = null
val Boolean.networkBits: Int get() = 1
val Byte.networkBits get() = Byte.SIZE_BITS
val Short.networkBits get() = Short.SIZE_BITS
val Char.networkBits get() = Char.SIZE_BITS
val Int.networkBits get() = Int.SIZE_BITS
val Long.networkBits get() = Long.SIZE_BITS
val Float.networkBits get() = Float.SIZE_BITS
val Double.networkBits get() = Double.SIZE_BITS


fun <V : Comparable<V>> Series<V>.toRegisterMask(
    r2: Int = 64,
    vwidth: Int? = b(a.dec()).networkBits,
): Join<Join<Join<Long, Int>, () -> Join<Int, (Int) -> BooleanArray>>, () -> Join<Int, (Int) -> Long>>? {
    var r: Int = r2
    val sizeInlayBits = r - a.toLong().dec().countLeadingZeroBits()
    val capacity = (r - sizeInlayBits) / (vwidth ?: return null)
    if (a > capacity) return null

    val newr = sizeInlayBits + vwidth * a
    r = newr
    var reg = a.toLong().dec() shl (r - sizeInlayBits)

    fun <V : Comparable<V>> V.toRegisterMask(r: Int = 64, vwidth: Int?): Series<Boolean>? {
        return if (this is Number) this.toLong().toRegisterMask(r, vwidth) else null
    }

    asIterable().forEachIndexed { index, v ->
        v.toRegisterMask(r, vwidth)?.let { mask: Series<Boolean> ->
            reg = reg or (mask.a.toLong() shl (r - sizeInlayBits - vwidth!! * index))
        } ?: return null
    }

    return reg j r j {
        a j { x: Int ->
            val l: Long = simpleReg(reg, sizeInlayBits, vwidth, x, r)
            BooleanArray(vwidth) { i -> l and (1L shl i) != 0L }
        }
    } j {
        a j { x: Int ->
            simpleReg(reg, sizeInlayBits, vwidth, x, r)
        }
    }
}

fun simpleReg(
    reg: Long,
    sizeInlayBits: Int,
    vwidth: Int,
    x: Int,
    r: Int,
): Long = reg shl (sizeInlayBits + vwidth * x) shr (r - vwidth)

val <V> V.networkBits: Int? get() = null

//default 64 bit r, register register factory construction
infix fun<A,B> (A).jr(b:B) {

    val vwidthA= networkBits;
    val vwidthB= b.networkBits;
}


enum class BinaryFunct {
    SHL,
    SHR,
    XOR,
    AND,
    PLUS,
    MINUS
}

interface PlatformCodec {
    val readLong: (ByteArray) -> Long
    val readInt: (ByteArray) -> Int

    val readShort: (ByteArray) -> Short
    val writeLong: (Long) -> ByteArray
    val writeInt: (Int) -> ByteArray
    val writeShort: (Short) -> ByteArray
    val writeDouble: (Double) -> ByteArray get() = { writeLong(it.toBits()) }
    val writeFloat: (Float) -> ByteArray get() = { writeInt(it.toBits()) }
    val readDouble: (ByteArray) -> Double get() = { Double.fromBits(readLong(it)) }
    val readFloat: (ByteArray) -> Float get() = { Float.fromBits(readInt(it)) }
    val readUShort: (ByteArray) -> UShort
    val readUInt: (ByteArray) -> UInt
    val readULong: (ByteArray) -> ULong
    val writeUShort: (UShort) -> ByteArray
    val writeUInt: (UInt) -> ByteArray
    val writeULong: (ULong) -> ByteArray

    companion object {
        @JvmStatic
        val isNetworkEndian: Boolean by lazy {
            val i = 0x01020304
            val b = i.toByte()
            b == 0x01.toByte()

        }

        @JvmStatic
        val isLittleEndian: Boolean get() = !isNetworkEndian

        object currentPlatformCodec : PlatformCodec {
            override val readShort: (ByteArray) -> Short by lazy {
                if (isLittleEndian) {
                    { ((it[1].toInt() and 0xFF) shl 8).toShort() or (it[0].toInt() and 0xFF).toShort() }
                } else {
                    { (it[0].toInt() and 0xFF shl 8 or (it[1].toInt() and 0xFF)).toShort() }
                }
            }
            override val readInt: (ByteArray) -> Int by lazy {
                if (isLittleEndian) {
                    {
                        (((it[3].toUByte()).toUInt() shl 24) or
                                ((it[2].toUByte()).toUInt() shl 16) or
                                ((it[1].toUByte()).toUInt() shl 8) or
                                (it[0].toUByte()).toUInt()).toInt()

                    }
                } else {
                    {
                        (((it[0].toUByte()).toUInt() shl 24) or
                                ((it[1].toUByte()).toUInt() shl 16) or
                                ((it[2].toUByte()).toUInt() shl 8) or
                                (it[3].toUByte()).toUInt()).toInt()
                    }
                }

            }
            override val readLong: (ByteArray) -> Long by lazy {

                if (isLittleEndian) {
                    {
                        (((it[7].toUByte()).toULong() shl 56) or
                                ((it[6].toUByte()).toULong() shl 48) or
                                ((it[5].toUByte()).toULong() shl 40) or
                                ((it[4].toUByte()).toULong() shl 32) or
                                ((((it[3].toUByte()).toUInt() shl 24) or
                                        ((it[2].toUByte()).toUInt() shl 16) or
                                        ((it[1].toUByte()).toUInt() shl 8) or
                                        (it[0].toUByte()).toUInt()).toULong())).toLong()
                    }
                } else {
                    {
                        (((it[0].toUByte()).toULong() shl 56) or
                                ((it[1].toUByte()).toULong() shl 48) or
                                ((it[2].toUByte()).toULong() shl 40) or
                                ((it[3].toUByte()).toULong() shl 32) or
                                ((((it[4].toUByte()).toUInt() shl 24) or
                                        ((it[5].toUByte()).toUInt() shl 16) or
                                        ((it[6].toUByte()).toUInt() shl 8) or
                                        (it[7].toUByte()).toUInt()).toULong())).toLong()

                    }
                }
            }
            override val writeShort: (Short) -> ByteArray by lazy {

                if (isLittleEndian) {
                    {
                        byteArrayOf(
                            (it.toUByte()).toByte(),
                            ((it.toUInt() shr 8).toUByte()).toByte()
                        )
                    }
                } else {
                    {
                        byteArrayOf(
                            ((it.toUInt() shr 8).toUByte()).toByte(),
                            (it.toUByte()).toByte()
                        )
                    }
                }
            }
            override val writeInt: (Int) -> ByteArray by lazy {
                if (isLittleEndian) {
                    {
                        byteArrayOf(
                            (it.toUByte()).toByte(),
                            ((it shr 8).toUByte()).toByte(),
                            ((it shr 16).toUByte()).toByte(),
                            ((it shr 24).toUByte()).toByte()
                        )
                    }
                } else {
                    {
                        byteArrayOf(
                            ((it shr 24).toUByte()).toByte(),
                            ((it shr 16).toUByte()).toByte(),
                            ((it shr 8).toUByte()).toByte(),
                            (it.toUByte()).toByte()
                        )
                    }
                }
            }
            override val writeLong: (Long) -> ByteArray by lazy {
                if (isLittleEndian) {
                    {
                        byteArrayOf(
                            (it.toUByte()).toByte(),
                            ((it shr 8).toUByte()).toByte(),
                            ((it shr 16).toUByte()).toByte(),
                            ((it shr 24).toUByte()).toByte(),
                            ((it shr 32).toUByte()).toByte(),
                            ((it shr 40).toUByte()).toByte(),
                            ((it shr 48).toUByte()).toByte(),
                            ((it shr 56).toUByte()).toByte()
                        )
                    }
                } else {
                    {
                        byteArrayOf(
                            ((it shr 56).toUByte()).toByte(),
                            ((it shr 48).toUByte()).toByte(),
                            ((it shr 40).toUByte()).toByte(),
                            ((it shr 32).toUByte()).toByte(),
                            ((it shr 24).toUByte()).toByte(),
                            ((it shr 16).toUByte()).toByte(),
                            ((it shr 8).toUByte()).toByte(),
                            (it.toUByte()).toByte()
                        )
                    }
                }
            }
            //6 kotlin unsigned adapters below for the above 6
            override val readUShort: (ByteArray) -> UShort ={it->readShort(it).toUShort()}
            override val readUInt: (ByteArray) -> UInt ={it->readInt(it).toUInt()}
            override val readULong: (ByteArray) -> ULong ={it->readLong(it).toULong()}
            override val writeUShort: (UShort) -> ByteArray ={it->writeShort(it.toShort())}
            override val writeUInt: (UInt) -> ByteArray ={it->writeInt(it.toInt())}
            override val writeULong: (ULong) -> ByteArray ={it->writeLong(it.toLong())}

        }
    }

}

// Implement similar objects for other types (ByteCodec, ShortCodec, etc.)


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

    fun BigInteger(toString: String): Number

    companion object {
        /**
         * minimum bitops types for the intended bitcount of NUID
         */
        fun minOps(size: Int): BitOps<*> =
            when (size) {
                in Int.MIN_VALUE..7 -> ByteBitOps
                8 -> UByteBitOps
                in 9..15 -> ShortBitOps
                16 -> UShortBitOps
                in 17..31 -> IntBitOps
                32 -> UIntBitOps
                in 33..63 -> LongBitOps
                64 -> ULongBitOps
                else -> BigIntOps
            } as BitOps<*>
    }
}


interface TypeMemento {
    val networkSize: Int?
}

enum class IOMemento(override val networkSize: Int? = null, val fromChars: (Series<Char>) -> Any) : TypeMemento {
    IoBoolean(1, {
        when (it[0]) {
            't' -> true
            'f' -> false
            else -> throw IllegalArgumentException("invalid boolean: $it")
        }
    }) {
        override fun createEncoder(i: Int): (Any?) -> ByteArray = writeBool
        override fun createDecoder(size: Int): (ByteArray) -> Any? = readBool
    },
    IoByte(1, {
        it.parseLong().toByte()
    }) {
        override fun createEncoder(i: Int): (Any?) -> ByteArray = writeByte
        override fun createDecoder(size: Int): (ByteArray) -> Any? = readByte
    },
    IoUByte(1, {
        it.parseLong().toUByte()
    }) {
        override fun createEncoder(i: Int): (Any?) -> ByteArray = writeUByte
        override fun createDecoder(size: Int): (ByteArray) -> Any? = readUByte
    },

    IoShort(2, { it.parseLong().toShort() }) {
        override fun createEncoder(i: Int): (Any?) -> ByteArray = currentPlatformCodec.writeShort as (Any?) -> ByteArray
        override fun createDecoder(size: Int): (ByteArray) -> Any? = currentPlatformCodec.readShort
    },

    IoInt(4, { it.parseLong().toInt() }) {
        override fun createEncoder(i: Int): (Any?) -> ByteArray = writeInt as (Any?) -> ByteArray
        override fun createDecoder(size: Int): (ByteArray) -> Any? = readInt
    },
    IoLong(8, Series<Char>::parseLong) {
        override fun createEncoder(i: Int): (Any?) -> ByteArray = writeLong as (Any?) -> ByteArray
        override fun createDecoder(size: Int): (ByteArray) -> Any? = readLong
    },
    IoUShort(2, { it.parseLong().toUShort() }) {
        override fun createEncoder(i: Int): (Any?) -> ByteArray =
            currentPlatformCodec.writeUShort as (Any?) -> ByteArray

        override fun createDecoder(size: Int): (ByteArray) -> Any? = currentPlatformCodec.readUShort
    },

    IoUInt(4, { it.parseLong().toUInt() }) {
        override fun createEncoder(i: Int): (Any?) -> ByteArray = writeUInt as (Any?) -> ByteArray
        override fun createDecoder(size: Int): (ByteArray) -> Any? = readUInt
    },
    IoULong(8, { it.parseLong().toULong() }) {
        override fun createEncoder(i: Int): (Any?) -> ByteArray = writeULong as (Any?) -> ByteArray
        override fun createDecoder(size: Int): (ByteArray) -> Any? = readULong
    },
    IoFloat(4, { it.parseDouble().toFloat() }) {
        override fun createEncoder(i: Int): (Any?) -> ByteArray = currentPlatformCodec.writeFloat as (Any?) -> ByteArray
        override fun createDecoder(size: Int): (ByteArray) -> Any? = currentPlatformCodec.readFloat
    },
    IoDouble(8, { it.parseDouble() }) {
        override fun createEncoder(i: Int): (Any?) -> ByteArray =
            currentPlatformCodec.writeDouble as (Any?) -> ByteArray

        override fun createDecoder(size: Int): (ByteArray) -> Any? = currentPlatformCodec.readDouble
    },
    IoLocalDate(8, { it.parseIsoDateTime() }) {
        override fun createEncoder(i: Int): (Any?) -> ByteArray = {
            //try a cast elvis first with Instant then with LocalDate
            val date = (it as? Instant)?.toLocalDateTime(TimeZone.UTC)?.date ?: it as LocalDate
//
//            val toEpochDays = (it as LocalDate).toEpochDays()
//            writeLong (toEpochDays.toLong())
            writeLong(date.toEpochDays().toLong())


        }

        override fun createDecoder(size: Int): (ByteArray) -> Any? = {
            val fromEpochDays = LocalDate.fromEpochDays(readLong(it).toInt())
            fromEpochDays
        }
    },

    /**
     * 12 bytes of storage, first epoch seconds Long , then nanos Int
     */
    IoInstant(12,
        { Instant.parse(it.toString()) }) {
        override fun createEncoder(i: Int): (Any?) -> ByteArray = { inst: Any? ->
            val instant = inst as Instant
            val epochSeconds = instant.epochSeconds
            val nanoAdjustment = instant.nanosecondsOfSecond
            writeLong(epochSeconds) + writeInt(nanoAdjustment)
        }

        override fun createDecoder(size: Int): (ByteArray) -> Any? = { bytes: ByteArray ->
            val epochSeconds = readLong(bytes)
            val nanoAdjustment = readInt(bytes.sliceArray(8..11))
            Instant.fromEpochSeconds(epochSeconds, nanoAdjustment)
        }
    },
    IoString(null, { it.asString() }) {
        override fun createEncoder(i: Int): (Any?) -> ByteArray = writeString

        override fun createDecoder(size: Int): (ByteArray) -> Any? = readString
    },
    IoCharSeries(null, ::CharSeries) {
        override fun createEncoder(i: Int): (Any?) -> ByteArray = writeCharSeries
        override fun createDecoder(size: Int): (ByteArray) -> Any? = readCharSeries
    },

    IoByteArray(null, { it.encodeToByteArray() }) {
        override fun createEncoder(i: Int): (Any?) -> ByteArray = writeByteArray
        override fun createDecoder(size: Int): (ByteArray) -> Any? = readByteArray
    },
    IoNothing(null, { "" }) {
        override fun createEncoder(i: Int): (Any?) -> ByteArray = { ByteArray(0) }
        override fun createDecoder(size: Int): (ByteArray) -> Any? = { ByteArray(0) }
    }
    ;

    abstract fun createEncoder(i: Int): (Any?) -> ByteArray
    abstract fun createDecoder(size: Int): (ByteArray) -> Any?

    companion object {
        val readCharSeries: (ByteArray) -> Series<Char> = { value: ByteArray -> value.decodeToChars() }
        val writeCharSeries: (Any?) -> ByteArray = { value: Any? -> (value as Series<Char>).encodeToByteArray() }
        val readByteSeries: (ByteArray) -> Series<Byte> = { value: ByteArray -> value.toSeries() }
        val writeByteSeries: (Any?) -> ByteArray = { value: Any? -> (value as Series<Byte>).toArray() }
        val readByteArray: (ByteArray) -> ByteArray = { value: ByteArray -> value }
        val writeByteArray: (Any?) -> ByteArray = { value: Any? -> value as ByteArray }

        val readString: (ByteArray) -> String = { value: ByteArray -> value.decodeToString() }
        val writeString: (Any?) -> ByteArray = { value: Any? -> (value as String).encodeToByteArray() }

        val readBool: (ByteArray) -> Boolean = { value: ByteArray -> value[0] == 1.toByte() }
        val writeBool: (Any?) -> ByteArray =
            { value: Any? -> ByteArray(1).apply { this[0] = if (value as Boolean) 1 else 0 } }
        val readByte: (ByteArray) -> Byte = { value: ByteArray -> value[0] }
        val writeByte: (Any?) -> ByteArray = { value: Any? -> ByteArray(1).apply { this[0] = value as Byte } }
        val readUByte: (ByteArray) -> UByte = { value: ByteArray -> value[0].toUByte() }
        val writeUByte: (Any?) -> ByteArray =
            { value: Any? -> ByteArray(1).apply { this[0] = (value as Byte) } }//take it on faith here
    }
}
