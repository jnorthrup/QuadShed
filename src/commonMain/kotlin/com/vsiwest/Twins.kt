@file:Suppress("UNCHECKED_CAST")

package com.vsiwest

inline infix fun < reified A: Comparable<A>,  reified B:Comparable<B>> A.j(other: B): Join<A, B> {
    val allocA = getAllocation<A>()
    val allocB = getAllocation<B>()
    val totalBits = allocA.bits + allocB.bits

    return if (totalBits <= 64) {
        object : Join<A, B>, BackingStore<ULong> {
            override val p: ULong = (allocA.encode(this@j) shl allocB.bits) or allocB.encode(other)
            override val a: A get() = allocA.decode(p shr allocB.bits)
            override val b: B get() = allocB.decode(p and ((1UL shl allocB.bits) - 1UL))
        }
    } else {
        object : Join<A, B> {
            override val a: A = this@j
            override val b: B = other
        }
    }
}

infix fun <A, B, C, D> BackingStore<*>.j(other: Join<C, D>): Join<Join<A, B>, Join<C, D>> {
    return when {
        this is BackingStore<*> && other is BackingStore<*> -> {
            val thisP = this.p as ULong
            val otherP = other.p as ULong
            object : Join<Join<A, B>, Join<C, D>>, BackingStore<ULong> {
                override val p: ULong = (thisP shl 32) or (otherP and 0xFFFFFFFFUL)
                override val a: Join<A, B> get() = this@j as Join<A, B>
                override val b: Join<C, D> get() = other
            }
        }
        else -> object : Join<Join<A, B>, Join<C, D>> {
            override val a: Join<A, B> get() = this@j as Join<A, B>
            override val b: Join<C, D> get() = other
        }
    }
}

fun main() {
    // Demonstrate joining of different types
    val joinIntChar: Join<Int, Char> = 1000000 j 'A'
    println("Join of Int and Char: ${joinIntChar.a} and ${joinIntChar.b}")
    if (joinIntChar is BackingStore<*>) {
        println("Backing store: ${(joinIntChar as BackingStore<ULong>).p.toString(2)}")
    } else {
        println("No backing store available")
    }

    val joinShortByte: Join<Short, Byte> = 12345.toShort() j 100.toByte()
    println("Join of Short and Byte: ${joinShortByte.a} and ${joinShortByte.b}")
    if (joinShortByte is BackingStore<*>) {
        println("Backing store: ${(joinShortByte as BackingStore<ULong>).p.toString(2)}")
    } else {
        println("No backing store available")
    }

    // Demonstrate nesting of Joins
    val nestedJoin: Join<Join<Int, Char>, Join<Short, Byte>> = joinIntChar j joinShortByte
    println("Nested Join: ((${nestedJoin.a.a}, ${nestedJoin.a.b}), (${nestedJoin.b.a}, ${nestedJoin.b.b}))")
    if (nestedJoin is BackingStore<*>) {
        println("Backing store: ${(nestedJoin as BackingStore<ULong>).p.toString(2)}")
    } else {
        println("No backing store available")
    }

    // Demonstrate Twin (Join of same type)
    val twinInt: Twin<Int> = 1000000 j 2000000
    println("Twin of Int: ${twinInt.a} and ${twinInt.b}")
    if (twinInt is BackingStore<*>) {
        println("Backing store: ${(twinInt as BackingStore<ULong>).p.toString(2)}")
    } else {
        println("No backing store available")
    }

    // Demonstrate nesting of Twins
    val nestedTwin: Twin<Twin<Int>> = twinInt j (3000000 j 4000000)
    println("Nested Twin: ((${nestedTwin.a.a}, ${nestedTwin.a.b}), (${nestedTwin.b.a}, ${nestedTwin.b.b}))")
    if (nestedTwin is BackingStore<*>) {
        println("Backing store: ${(nestedTwin as BackingStore<ULong>).p.toString(2)}")
    } else {
        println("No backing store available")
    }
}