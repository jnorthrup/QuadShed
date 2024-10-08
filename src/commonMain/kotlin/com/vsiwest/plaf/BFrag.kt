package com.vsiwest.plaf

import com.vsiwest.ByteSeries
import com.vsiwest.Join
import com.vsiwest.Twin
import com.vsiwest.assert
import com.vsiwest.j

typealias BFrag = Join< /**endexclusive range*/ Twin<Int>, ByteArray>

fun BFrag.size(): Int {
    val (bounds) = this
    val (beg, end) = bounds
    return end - beg
}

fun BFrag.isEmpty(): Boolean = run {
    val (bounds) = this@isEmpty
    val (beg, end) = bounds
    end - beg
} == 0

/**slice is 0-based as if beg was 0;*/
fun BFrag.slice(atInclusive: Int, untilExclusive: Int = a.b): BFrag = a. run {
   a + ( atInclusive ) j  untilExclusive } j b

/*
as in ByteBuffer.flip after a read
flip is 0-based as if beg was 0;
*/
fun BFrag.flip(endExclusive: Int): BFrag {
    if (endExclusive == run {
            val (bounds) = this@flip
            val (beg, end) = bounds
            end - beg
        }) return this
    val (beg) = a
    val newEnd = beg + endExclusive
    val buf = b
    return beg j newEnd j buf
}

/**
split1 returns 1 or 2 BFrags.
if the lit is not found, first is null, second is original
if the lit is found, first is up to and including lit.
if remaining bytes is zero, null, else second is the rest
 */
fun BFrag.split1(lit: Byte): Twin<BFrag?> {
    val (bounds, buf) = this
    val (beg, end) = bounds
    var x = beg
    while (x < end && buf[x] != lit) x++ //if token is found x is inclusive

    val ret: Twin<BFrag?> = when (x) {
        end -> null j this
        end.dec() -> this j null
        else -> {
            ++x     //x is now exclusive of token
            x -= beg//the bug that held me captive for 2 days
            val line = flip(x)// flip is 0-based as if beg was 0;
            val tail = slice(x)//slice is 0-based as if beg was 0
            line j tail
        }
    } as Twin<BFrag?>
    assert(ret.a != null || ret.b != null)
    return ret
}

fun BFrag.copyInto(ret: ByteArray, offset: Int) {
    val (bounds, buf) = this
    val (beg, end) = bounds
    buf.copyInto(ret, offset, beg, end)
}

val BFrag.byteSeries: ByteSeries
    get() = ByteSeries(b, a.a, a.b)