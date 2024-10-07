@file:Suppress("DEPRECATION", "UNCHECKED_CAST")

package com.vsiwest.plaf


import com.vsiwest.*
import com.vsiwest.CharSeries
import com.vsiwest.Join
import com.vsiwest.Series
import com.vsiwest.Twin
import com.vsiwest.assert
import com.vsiwest.debug
import com.vsiwest.drop
import com.vsiwest.get
import com.vsiwest.j
import com.vsiwest.last
import com.vsiwest.log
import com.vsiwest.logDebug
import com.vsiwest.meta.IOMemento
import com.vsiwest.meta.IOMemento.IoCharSeries
import com.vsiwest.meta.RecordMeta
import com.vsiwest.plaf.TypeEvidence.*
import com.vsiwest.plaf.TypeEvidence.Companion.deduce
import com.vsiwest.plaf.TypeEvidence.Companion.update
import com.vsiwest.size
import com.vsiwest.toArray
import com.vsiwest.toList
import com.vsiwest.toSeries
import com.vsiwest.α
import com.vsiwest.`↺`
import kotlin.jvm.JvmOverloads
import kotlin.toUShort

/**
 * a versatile range of two unsigned shorts stored as a 32 bit Int value as Inline class
 */
@kotlin.jvm.JvmInline
typealias   DelimitRange= Twin<UShort>


/** forward scanner of commas, quotes, and newlines
 */
object CSVUtil {

    /**
     * read a csv file into a series of segments
     */
//    @JvmStatic
    @JvmOverloads
    fun parseLine(
        /**the source media*/
        file: LongSeries<Byte>,
        /**the first byte offset inclusive*/
        start: Long,
        /**the last offset exclusive.  -1 has an undefined end. */
        end: Long = -1L,
        //this is 1 TypeDeduction per column, for one line. elsewhere, there should be a TypeDeduction holding maximum findings per file/column.
        lineEvidence: MutableList<TypeEvidence>? = null,
    ): Series<> {
        var quote = false
        var doubleQuote = false
        var escape = false
        var ordinal = 0
        var x = start as Long
        val value: Byte = file[x]
        while (x != end && value.toInt().toChar().isWhitespace()) x++ //trim
        var since = x

        val rlist = mutableListOf<DelimitRange>()
        val size = file.size()
        while (x != end && x < size) {
            val c = value
            val char = c.toInt().toChar()
            lineEvidence?.apply {
                //test deduce length and add if needed
                if (ordinal >= lineEvidence.size)
                    lineEvidence.add(TypeEvidence())
                lineEvidence[ordinal] + char
            }
            when {
                escape -> escape = false
                char == '"' -> doubleQuote = !doubleQuote
                char == '\'' -> quote = !quote
                char == '\\' -> escape = !escape
                char == ',' -> if (!quote && !doubleQuote) {
                    val element = DelimitRange((since.toUShort().toInt() shl 16) or x.toUShort().toInt())
                    rlist.add(element)
// these check out                    logDebug { "val${element.pair}: "+ CharSeries(file[ element.asIntRange ].decodeUtf8()).asString() }
                    lineEvidence?.apply {
                        if (since == x)
                            lineEvidence[ordinal].empty++
                        else lineEvidence[ordinal].columnLength = (x - since).toUShort()
                    }
                    ordinal++
                    since = x + 1
                }

                char == '\r' || char == '\n' || end == x.inc() -> {
                    val element = DelimitRange((since.toUShort().toInt() shl 16) or x.toUShort().toInt())
                    rlist.add(element)
                    lineEvidence?.apply {
//                        logDebug { "bookend val${element.pair}: " + CharSeries(file[element.asIntRange].decodeUtf8()).asString() }
                        if (since == x)
                            lineEvidence[ordinal].empty++
                        else lineEvidence[ordinal].columnLength = (x - since).toUShort()
                    }
                    break
                }
            }
            x++
        }
        assert(rlist.isNotEmpty())
        return (rlist.size) j { rlist[it].value }
        // what happens specifically in the above code when we pass in a line with no cr/lf in the above code:

    }


    /**
     * this will do a best-attempt at using the parseSegments output to marshal the types of newMeta passed in.
     *  the meta encode functions of the newMeta must be aligned with CharBuf input of the parseSegments output to
     *  utilize String-ish conversions implied by CSV data
     */
    fun parseConformant(
        file: LongSeries<Byte>,
        newMeta: Series<RecordMeta>? = null,
        fileEvidence: MutableList<TypeEvidence>? = mutableListOf(),
    ): Cursor {
        //first we call parseSegments with our fileEvidence then we trap the RecordMeta child types as a separate meta,
        // then we use the CharSeries cursor features to create a String marshaller per column
        val segments = parseSegments(file, fileEvidence)
        val meta = (newMeta ?: (segments.meta α { (it as RecordMeta).child!! })).debug {
            val l = it.toList()
            logDebug { "parseConformantmeta: $l" }
        }
        return segments.size() j { y ->
            segments.row(y).let { rv: RowVec ->
                rv.size() j { x: Int ->
                    val recordMeta = meta[x]
                    val type = recordMeta.type
                    val any = rv[x].a
                    try {
                        val fromChars = type.fromChars(any as CharSeries)
                        val function = recordMeta.`↺`
                        fromChars j function
                    } catch (e: Exception) {
                        log { "parseConformant: $e col $x row $y " }
                        throw e
                    }
                }
            }
        }
    }

    fun parseSegments(
        file: LongSeries<Byte>,
        fileEvidence: MutableList<TypeEvidence>? = null,
    ): Cursor {
        val upperBound = file.size()
        val hdrParsRes = CSVUtil.parseLine(file, 0, upperBound)
        val header = hdrParsRes α ::DelimitRange
        val headerNames = header α { delimR ->
            val join = file[delimR.a.toLong() until delimR.b.inc().toLong()]
            CharSeries(join.toSeries().decodeUtf8()).asString()
        }
        logDebug { "headerNames: ${headerNames.toList()}" }

        val lines = mutableListOf<Join<Long, Series<RecordMeta>>>()
        val last1 = header.last()
        var datazero1 = last1.b.toLong()

        do {
            val file1 = file.drop(datazero1)
            if (file1.a < headerNames.a.toLong()) break

            val lineEvidence = fileEvidence?.let { mutableListOf<TypeEvidence>() }
            val parsRes = CSVUtil.parseLine(file1, 0, file1.size, lineEvidence)
            lineEvidence?.apply { fileEvidence.update(lineEvidence) }
            val line = parsRes α ::DelimitRange
            val dstart = datazero1
            datazero1 += line.last().b.toLong()

            if (line.a != header.a) {
                logDebug { "line.size: ${line.size()}" }
                logDebug { "header.size: ${header.size()}" }
                logDebug { "headerNames: ${headerNames.toList()}" }
                logDebug { "line: ${line α DelimitRange::pair}" }
                logDebug { "fileStart/End: $datazero1/${file.size()}" }
                throw Exception("line segments do not match header count")
            }

            val toArray = (line α { it.value }).toArray()
            lines.add(dstart j toArray)
        } while (datazero1 < file.size())

        val conversionSegments = fileEvidence?.map { evidence ->
            val deduce = deduce(evidence)
            deduce j (deduce.networkSize ?: evidence.columnLength.toInt())
        }?.toSeries()

        val convertedSegmentLengths = conversionSegments?.right?.toArray()
        val convertedSegments = convertedSegmentLengths?.fold(mutableListOf<DelimitRange>()) { acc, length ->
            val last = acc.lastOrNull()?.b ?: 0.toUShort()
            acc.add(DelimitRange((last.toInt() shl 16) or (last + length.toUInt()).toUShort().toInt()))
            acc
        }

        val successorMeta = convertedSegmentLengths?.let {
            it.indices.map { x ->
                RecordMeta(
                    name = headerNames[x],
                    type = conversionSegments.left[x],
                    begin = convertedSegments?.get(x)?.a?.toInt() ?: -1,
                    end = convertedSegments?.get(x)?.b?.toInt() ?: -1,
                )
            }
        }

        var reporter: FibonacciReporter? = null
        debug { reporter = FibonacciReporter(lines.size) }

        return lines α { line ->
            val lserr = file.drop(line.a)[0 until line.b.size]
            line.b.withIndex() α { (x, b) ->
                val delimitRange = DelimitRange(b)
                CharSeries(
                    lserr[delimitRange.first.toInt() until delimitRange.endInclusive.inc().toInt()].decodeUtf8()
                ) j {
                    val endExclusive = this@DelimitRange.b.toInt().inc()
                    val air = (this@DelimitRange.a.toInt() until this@DelimitRange.b.inc().toInt())
                    RecordMeta(
                        headerNames[x],
                        IoCharSeries,
                        air.first,
                        air.last.inc(),
                        child = successorMeta?.get(x)
                    )
                }
            }.debug { reporter?.report()?.let { rep -> logDebug { rep } } }
        } as Cursor
    }

}


/** list<String>  -> CSV Cursor of strings
 * */
@OptIn(ExperimentalUnsignedTypes::class)
fun simpelCsvCursor(lineList: List<String>): Cursor {
    //take line11 as headers.  the split by ','
    val headerNames = lineList[0].split(",").map { it.trim() }
    val hdrMeta = headerNames.map {
        RecordMeta(
            it, IOMemento.IoString
        )
    }
    //count of fields
    val fieldCount = headerNames.size
    val lines = lineList.drop(1)
    val lineSegments = arrayOfNulls<UShortArray>(lines.size)

    return lines.size j { y ->
        val line = lines[y]
        //lazily create linesegs
        val lineSegs = lineSegments[y] ?: UShortArray(headerNames.size).also { proto ->
            lineSegments[y] = proto
            var f = 0
            for ((x, c) in line.withIndex()) if (c == ',')
                proto[f++] = x.toUShort()
        }

        fieldCount j { x: Int ->
            val start = if (x == 0) 0 else lineSegs[x - 1].toInt() + 1
            val end = if (x == fieldCount - 1) line.length else lineSegs[x].toInt()
            line.substring(start, end) j hdrMeta[x].`↺`
        }
    }
}