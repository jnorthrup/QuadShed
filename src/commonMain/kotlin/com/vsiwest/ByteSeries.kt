package com.vsiwest

import com.vsiwest.bitops.CZero.nz
import com.vsiwest.bitops.reversed

fun Series<Byte>.decodeUtf8(charArray: CharArray? = null): Series<Char> {
    return charArray?.let { it: CharArray ->
        decodeDirtyUtf8(it)
    } ?: if (isDirtyUTF8()) decodeDirtyUtf8() else {
        val join: Join<Int, (Int) -> Char> = a j b α (Byte::toInt `•` Int::toChar)
        (join)
    }
}

fun Series<Byte>.decodeDirtyUtf8(charArray: CharArray = CharArray(a)): Series<Char> {
    //does not use StringBuilder, but is faster than String(bytes, Charsets.UTF_8)
    var y = 0
    var w = 0
    while (y < a && w < charArray.size) {
        val c = this[y++].toInt()/* 0xxxxxxx */
        when (c shr 4) {
            in 0..7 -> charArray[w++] = c.toChar() // 0xxxxxxx

            /*12, 13*/ 0x0C, 0x0D -> {
            // 110x xxxx   10xx xxxx
            val c2 = this[y++].toInt()
            charArray[w++] = ((c and 0x1F) shl 6 or (c2 and 0x3F)).toChar()
        }

            /*14*/ 0x0E -> {
            // 1110 xxxx  10xx xxxx  10xx xxxx
            val c2 = this[y++].toInt()
            val c3 = this[y++].toInt()
            charArray[w++] = ((c and 0x0F) shl 12 or (c2 and 0x3F) shl 6 or (c3 and 0x3F)).toChar()
        }
        }
    }
    return w j charArray::get
}



/**
 * byte based spiritual successor to ByteBuffer for parsing
 */
class ByteSeries(
    buf: Series<Byte>,

    /** the mutable position accessor */
    var pos: Int = 0,

    /** the limit accessor */
    var limit: Int = buf.a, //initialized to size

    /** the mark accessor */
    var mark: Int = -1,
) : Series<Byte> by buf { //delegate to the underlying series

    /** get, the verb - the char at the current position and increment position */
    inline val get: Byte
        get() {
            if (!hasRemaining) throw IndexOutOfBoundsException("pos: $pos, limit: $limit")
            val c = get(pos); pos++; return c
        }

    //string ctor
    constructor(s: String) : this(s.encodeToByteArray().toSeries())

    constructor(buf: ByteArray, pos: Int = 0, limit: Int = buf.size) : this(
        limit j buf::get, pos
    )

    /**remaining chars*/
    val rem: Int get() = limit - pos

    /** immutable max capacity of this buffer, alias for size*/
    val cap: Int get() = a

    /** boolean indicating if there are remaining chars */
    val hasRemaining: Boolean get() = rem.nz

    /** mark, the verb - marks the current position */
    val mk: ByteSeries
        get() = apply {
            mark = pos
        }

    /** reset pos to mark */
    val res: ByteSeries
        get() = apply {
            pos = if (mark < 0) pos else mark
        }

    /** flip the buffer, limit becomes pos, pos becomes 0 -- made into a function for possible side effects in debugger */
    fun flip(): ByteSeries = apply {
        limit = pos
        pos = 0
        mark = -1
    }

    /**rewind to 0*/
    val rew: ByteSeries
        get() = apply {
            pos = 0
        }

    /** clears the mark,pos, and sets limit to size */
    val clr: ByteSeries
        get() = apply {
            pos = 0
            limit = this.a
            mark = -1
        }

    /** position, the verb - holds the position that will be returned by the next get */
    fun pos(p: Int): ByteSeries = apply {
        pos = p
    }

    /** slice creates/returns a subrange ByteSeries from pos until limit */
    val slice: ByteSeries
        get() {
            val pos1 = this.pos
            val limit1 = this.limit
            val intRange = pos1 until limit1
            val buf = (this)[intRange]
            return ByteSeries(buf)
        }

    /** limit, the verb - redefines the last position accessable by get and redefines remaining accordingly*/
    fun lim(i: Int): ByteSeries = apply { limit = i }

    /** skip whitespace */
    val skipWs: ByteSeries get() = apply { while (hasRemaining && mk.get.toInt().toChar().isWhitespace()); res }

    val rtrim: ByteSeries get() = apply { while (rem > 0 && b(limit - 1).toInt().toChar().isWhitespace()) limit-- }


    fun clone(): ByteSeries = ByteSeries(a j b).also { it.pos = pos; it.limit = limit; it.mark = mark }


    /** a hash of contents only. not position, limit, mark */
    val cacheCode: Int
        get() {
            var h = 1
            for (i in pos until limit) {
                h = 31 * h + b(i).hashCode()
            }
            return h
        }

    override fun equals(other: Any?): Boolean {
        when {
            this === other -> return true
            other !is ByteSeries -> return false
            pos != other.pos -> return false
            limit != other.limit -> return false
            mark != other.mark -> return false
            this.a != other.a -> return false
            else -> {
                for (i in 0 until this.a) if (b(i) != other.b(i)) return false
                return true
            }
        }
    }

    /** idempotent, a cache can contain this hash and safely deduce the result from previous inserts */
    override fun hashCode(): Int {
        var result = pos
        result = 31 * result + limit
        result = 31 * result + mark
        result = 31 * result + this.a
//include cachecode
        result = 31 * result + cacheCode
        return result
    }


    fun asString(upto: Int = Int.MAX_VALUE): String = toArray().decodeToChars().asString().take(upto)

    override fun toString(): String {
        val take = asString().take(4)
        return "ByteSeries(position=$pos, limit=$limit, mark=$mark, cacheCode=$cacheCode,take-4=${take})"
    }

    /** skipws and rtrim */
    val trim: ByteSeries
        get() = apply {
            var p = pos
            var l = limit
            while (p < l && (0xff and get(p).toInt()).toChar().isWhitespace()) p++
            while (l > p && (0xff and get(l.dec()).toInt()).toChar().isWhitespace()) l--
            lim(l)
            pos(p)
        }


    //isEmpty override
    val isEmpty: Boolean get() = pos == limit

    /** success move position to the char after found (exclusive) and returns true.
     *  fail returns false and leaves position unchanged */
    fun seekTo(
        /**target*/
        target: Byte,
    ): Boolean {
        val anchor = pos
        while (hasRemaining) {
            val c = get
            if (c == target) return true
        }
        pos = anchor
        return false
    }

    /** success move position to the char after found and returns true.
     *  fail returns false and leaves position unchanged */
    fun seekTo(
        /**target*/
        target: Byte,
        /**if present this escapes one char*/
        escape: Byte,
    ): Boolean {
        val anchor = pos
        var escaped = false
        while (hasRemaining) get.let { c ->
            if (escaped) escaped = false
            else when (c) {
                target -> return true
                escape -> escaped = true
            }
        }
        pos = anchor
        return false
    }

    fun seekTo(lit: Series<Byte>): Boolean {
        val anchor = pos
        var i = 0
        while (hasRemaining) {
            if (get == lit[i]) {
                i++
                if (i == lit.a) return true
            } else {
                i = 0
            }
        }
        pos = anchor
        return false
    }

    /**backtrack 1*/
    operator fun dec(): ByteSeries = apply { require(pos > 0) { "Underflow" }; pos-- }

    /** advance 1*/
    operator fun inc(): ByteSeries = apply { require(hasRemaining) { "Overflow" };pos++ }

    /**
     * this rewrites the Series default toArray() to use the position and limit
     */
    fun toArray(): ByteArray = ByteArray(rem, ::get)

}


fun Series<Byte>.isDirtyUTF8(): Boolean {
    var dirty = false
    val bsz = a
    //if thereis one more byte to test and the first byte is in the range of 110x xxxx
    //what shr 4 proves: 110x xxxx
    val barLen = bsz.dec()
    for (b in 0 until barLen) if ((this[b].toInt() shr 4) in 0x0C..0x0E) {
        // what shr 4 proves: 1110 xxxx
        val byte = this[b.inc()]
        if ((byte.toInt() shr 6) == 0x02) {
            dirty = true
            break
            //what shr 6 proves: 10xx xxxx
        }
    }
    return dirty
}

//opposite method to build a charSeries from byte[]
fun ByteArray.decodeToChars(): Series<Char> = toSeries().decodeUtf8(/*CharArray(size)*/)

fun Series<Char>.asString(upto: Int = Int.MAX_VALUE): String = this.take(upto).encodeToByteArray().decodeToString()


fun ByteSeries.decodeToString() = decodeUtf8().asString()

fun Series<Byte>.startsWith(s: String): Boolean {
    val join = s.encodeToByteArray().toSeries()
    return join.a <= a && join.`⏵`.zip(this.`⏵`).all { it.first == it.second }
}

fun Series<Byte>.endsWith(s: String): Boolean {
    val join = s.encodeToByteArray().toSeries()
    return join.a <= a && join.`⏵`.zip(this.reversed().`⏵`).all {
        it.run { first == second }
    }
}

operator fun Series<Byte>.div(delim: Byte): Series<Series<Byte>> { //lazy split
    val intList = mutableListOf<Int>()
    for (x in 0 until a) if (this[x] == delim) intList.add(x)

    /**
     * iarr is an index of delimitted endings of the ByteSeries.
     */
    val iarr: IntArray = intList.toIntArray()

    return iarr α { v: Int ->
        val p = if (v == 0) 0 else iarr[v.dec()].inc() //start of next
        val l = //is v last index?
            if (v == iarr.lastIndex) a
            else iarr[v].dec()
        this[p until l]
    }
}
