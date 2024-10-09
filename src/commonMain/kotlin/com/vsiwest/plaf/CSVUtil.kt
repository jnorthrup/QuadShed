@file:Suppress("DEPRECATION", "UNCHECKED_CAST", "USELESS_CAST")

package com.vsiwest.plaf


import com.vsiwest.*
import com.vsiwest.CharSeries
import com.vsiwest.Join
import com.vsiwest.drop
import com.vsiwest.get
import com.vsiwest.j
import com.vsiwest.last
import com.vsiwest.meta.IOMemento
import com.vsiwest.meta.RecordMeta
import com.vsiwest.plaf.TypeEvidence.*
import com.vsiwest.size
import com.vsiwest.α
import com.vsiwest.`↺`
import kotlin.toUShort

typealias DelimitRange = Join<Long, Long>

object CSVUtil {
    fun parseLine(
        file: MetaSeries<Long, Byte>,
        start: Long,
        end: Long = -1L,
    ): MetaSeries<Int, DelimitRange> {
        var quote = false
        var doubleQuote = false
        var escape = false
        var x = start
        val size = file.size()
        val endIndex = if (end == -1L) size else minOf(end, size)

        val segments = mutableListOf<DelimitRange>()
        var since = x

        while (x < endIndex) {
            val char = file[x].toInt().toChar()
            when {
                escape -> escape = false
                char == '"' -> doubleQuote = !doubleQuote
                char == '\'' -> quote = !quote
                char == '\\' -> escape = true
                char == ',' -> if (!quote && !doubleQuote) {
                    segments.add(since j x)
                    since = x + 1
                }

                char == '\r' || char == '\n' || x == endIndex - 1 -> {
                    segments.add(since j x)
                    break
                }
            }
            x++
        }

        return segments.size j { segments[it] }
    }

    fun parseSegments(file: MetaSeries<Long, Byte>): Cursor {
        val header = parseLine(file, 0)
        val headerNames = header.α { range ->
            file[range.a until range.b].toSeries().decodeUtf8().asString().trim()
        }

        val lines = mutableListOf<Join<Long, MetaSeries<Int, RecordMeta>>>()
        var datazero = header.last().b + 1

        while (datazero < file.size()) {
            val line = parseLine(file, datazero)

            if (line.size() != header.size()) {
                throw Exception("Line segments do not match header count")
            }

            val element = datazero j (line.α { range ->
                RecordMeta(
                    headerNames[range.a.toInt()],
                    IOMemento.IoCharSeries,
                    range.a.toInt(),
                    range.b.toInt()
                )
            })

            lines.add(element)
            datazero = file.drop(datazero).`⏵`.indexOfFirst { it.toInt().toChar() == '\n' }.let {
                if (it == -1) file.size() else datazero + it + 1
            }
        }

        return lines.size j { y ->
            val line = lines[y]
            line.b.size() j { x:Int ->
                val meta = line.b[x]
                val range = meta.begin.toLong() until meta.end.toLong()
                val join = file[range]
                val decodeUtf8: Series<Char> = join.toSeries().decodeUtf8()

                decodeUtf8.asString().trim() j { meta as ColumnMeta }

            }
        }
    }
}



fun main() {
    // Test data
    val csvData = """
        Name,Age,City
        John,30,New York
        Alice,25,London
        Bob,35,Paris
    """.trimIndent()

    // Convert test data to MetaSeries<Long, Byte>
    val file: MetaSeries<Long, Byte> =
        csvData.encodeToByteArray().size.toLong() j { i -> csvData.encodeToByteArray()[i.toInt()] }

    // Parse the CSV data
    val cursor = CSVUtil.parseSegments(file)

    // Print the parsed data
    println("Parsed CSV Data:")
    println("----------------")

    cursor.`⏵`.forEach { row ->
        row.`⏵`.forEach { (value: Any?, meta: () -> ColumnMeta)->
            val (m1,m2)=meta as RecordMeta  //cast to RecordMeta
            print("${meta.name}: $value | ")
        }
        println()
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