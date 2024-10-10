@file:Suppress("UNCHECKED_CAST", "USELESS_CAST")

package com.vsiwest.plaf

import com.vsiwest.*
import com.vsiwest.Series
import com.vsiwest.Series2
import com.vsiwest.j
import com.vsiwest.meta.IOMemento.*
import com.vsiwest.meta.IOMemento.IoLong
import com.vsiwest.conv.conversion
import com.vsiwest.meta.ioMemento
import com.vsiwest.`⏵`
import kotlin.jvm.*
import kotlin.jvm.JvmInline
import kotlin.math.*
import kotlin.random.Random
import kotlin.reflect.*

typealias RowVec = Series2<Any?, () -> ColumnMeta>
//val RowVec.left get() =  this α Join<*, () -> RecordMeta>::a

/** Cursors are a columnar abstraction composed of Series of Joined value+meta pairs (RecordMeta) */
typealias Cursor = Series<RowVec>

///**
// * overload unary minus operator for Cursor to strip out the meta and return a series of values-only
// *
// * apparently this duplicates the unaryMinus() function above it, but it's not clear how to get the compiler to use that one
// */
//operator fun Cursor.unaryMinus(): Series<Series<*>> = this α { it α Join<*, () -> RecordMeta>::a }

/** Operator Cursor '/' Class<A>
 *
 * returns Series<Series<A?>>> where the meta is stripped out and the values are cast using
 *
 * it "as?" A return only A values and null for non-A values */
inline operator fun <A : Any, IR : Any?, SrInnr : Series<Join<A, *>>, SrOutr : Series<SrInnr>, RC : KClass<A?>> SrOutr.div(
    c: KClass<out A>,
): Series<Series<A?>> = this α { it α Join<A, *>::a } α { it α { it } } α { it α { it } }


/** cursor get by IntRange -- return a Cursor with the columns specified by the IntRange */
operator fun Cursor.get(i: IntRange): Cursor {
    require(i.first >= 0) { "index ${i.first} out of bounds for cursor of size $a" }
    require(i.last < a) { "index ${i.last} out of bounds for cursor of size $a" }
    return a j { y ->
        // get the size of range
        val rangeSize = i.last - i.first + 1
        rangeSize j { x ->
            row(y)[i.first + x]
        }
    }
}

/** get meta for a cursor from row 0 */
val Cursor.meta: Series<ColumnMeta>
    get() = row(0) α { (_, b): Join<*, () -> ColumnMeta> ->
        b()
    }

/** create an Intarray of cursor meta by Strings of column names */
fun Cursor.meta(vararg s: String): Series<Int> {
    val meta: Series<ColumnMeta> = meta
    return s.size j { i ->
        meta.`⏵`.indexOfFirst { columnMeta: ColumnMeta -> columnMeta.name == s[i] }
    }
}

  inline fun <  reified K : Comparable<K>,   reified V> MetaSeries<K, V>.get(index: Series<Int>): Series<V> {

    return index.a  j  { x: Int ->
     val invoke: K = index.a.ioMemento.conversion<V, K>(a.ioMemento).invoke(index[x] as V)
     b(invoke as K)

 }
}

/** cursor get by String vararg -- return a Cursor with the columns specified by the vararg */
fun Cursor.get(vararg s: String): Cursor = (this as Series<RowVec>).get(meta(*s))

/** ColumnExclusion value class
 *
 * used to exclude columns from a cursor by name
 *
 * @param name the name of the column to exclude */
@JvmInline
value class ColumnExclusion(val name: String) {
    override fun toString(): String = "ColumnExclusion($name)"
}

/** create operator unary minus for ColumnExclusion on string */
operator fun String.unaryMinus(): ColumnExclusion = ColumnExclusion(this)

/** Return cursor with columns excluded by indexes */
operator fun Cursor.minus(killbag: Series<Int>): Cursor {
    val toSet = (0 until meta.a).toSet()
    val ints = (toSet - killbag.toSet())
    val get = (this as Series<RowVec>).get(ints.toIntArray())
    return get as Cursor

}

/** cursor get by ColumnExclusion vararg -- return a Cursor with the columns excluded by the vararg */
fun Cursor.get(s: Series<ColumnExclusion>): Cursor {

    val exclusionBag = mutableSetOf<Int>()
    s.`⏵`.forEachIndexed { i: Int, it: ColumnExclusion ->
        exclusionBag.add(meta.`⏵`.indexOfFirst { it.name == it.name })
    }
    val retained = ((0 until meta.a).toSet() - exclusionBag).toIntArray()
    return this[retained]
}

//in columnar project this is meta.right
val Series<ColumnMeta>.names get() = this α ColumnMeta::name

/** head default 5 rows
 * just like unix head - print default 5 lines from cursor contents to stdout */
@JvmOverloads
fun Cursor.head(last: Int = 5): Unit = show(0 until (max(0, min(last, a))))

/** run head starting at random index */
fun Cursor.showRandom(n: Int = 5) {
    head(0);repeat(n) {
        if (a > 0) showValues(Random.nextInt(0, a).let { it..it })
    }
}

/** simple printout macro*/
fun Cursor.show(range: IntRange = 0 until a) {
    val meta: Series<ColumnMeta> = meta
    println("rows:$a" to meta.names.toList())
    showValues(range)
}

fun Cursor.showValues(range: IntRange) {
    try {
        range.forEach { x: Int ->
            val row: RowVec = row(x)

            val show = row α { (c, d) ->
                val meta = d()
                meta.name to c
                when (meta.type) {
                    IoCharSeries -> meta.name to (c as Series<Char>).asString()
                    else -> c
                }


            }

            println(show.toList())
        }
    } catch (e: NoSuchElementException) {
        println("cannot fully access range $range")
    }
}


/** gets the RowVec at y or if y is negative then -y from last */
infix fun Cursor.at(y: Int): RowVec = b(if (y < 0) a - y else y)
infix fun Cursor.row(y: Int): RowVec = at(y)

/** Cursor get by Int vararg -- return a Cursor with the columns specified by the vararg */
operator fun Cursor.get(vararg i: Int): Cursor = a j { y: Int ->
    i.size j { x: Int ->
        row(y)[i[x]]
    }
}


/** IsNumerical
 * iterate the meta enum types and check if all are numerical
 *
 * IoByte,IoShort,IoInt,IoDouble,IoLong   qualify as numerical
 *
 * kotlin enumset is not available in JS
 *
 */
val Cursor.isNumerical: Boolean
    get() = meta.`⏵`.all {
        when (it.type) {
            IoByte, IoShort, IoInt, IoFloat, IoDouble, IoLong -> true
            else -> false
        }
    }

val Cursor.isHomoMorphic: Boolean get() = !meta.`⏵`.any { it.type != meta[0].type }
