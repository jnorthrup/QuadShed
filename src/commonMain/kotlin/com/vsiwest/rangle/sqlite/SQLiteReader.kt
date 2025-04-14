@file:Suppress("UNCHECKED_CAST")

package com.vsiwest.rangle.sqlite

import com.vsiwest.rangle.Join
import com.vsiwest.rangle.Series
import com.vsiwest.rangle.j

/**
 * A basic SQLite file decoder implementation using QuadShed's idioms.
 * This is a "bitbanging tool" that directly interprets the SQLite file format.
 */

// --- Constants ---
const val SQLITE_HEADER_SIZE = 100
const val SQLITE_MAGIC_STRING = "SQLite format 3\u0000"

// --- Data Structures ---

/** Basic info parsed from the 100-byte SQLite header */
data class SQLiteHeaderInfo(
    val pageSize: Int,
    val fileFormatWriteVersion: Int,
    val fileFormatReadVersion: Int,
    val textEncoding: Int, // 1: UTF-8, 2: UTF-16le, 3: UTF-16be
    val schemaCookie: Int, // Used for schema change detection
    val fileChangeCounter: Int,
    val databaseSizeInPages: Int,
    val firstFreelistTrunkPage: Int,
    val totalFreelistPages: Int
)

/** Info parsed from a B-Tree page header */
data class PageHeaderInfo(
    val pageType: Byte, // 0x02: Interior Index, 0x05: Interior Table, 0x0a: Leaf Index, 0x0d: Leaf Table
    val firstFreeblockOffset: Int,
    val cellCount: Int,
    val cellContentAreaOffset: Int, // Start of cell content area
    val fragmentedFreeBytes: Int,
    val rightMostPointer: Int? // Only for interior pages (page number)
)

/** Represents an entry found in the sqlite_schema table */
data class SchemaDefinition(
    val type: String, // "table", "index", "view", "trigger"
    val name: String,
    val tableName: String, // Associated table name
    val rootPage: Int, // Starting page number for the object
    val sqlDefinition: String // Original CREATE statement (NOT PARSED FURTHER HERE)
)

/** Typealias for a parsed cell: RowID (if table leaf) and Payload bytes */
typealias ParsedCell = Join<Long?, Series<Byte>> // RowID (Nullable Long) j Payload (Series<Byte>)

/** Typealias for Varint parsing result */
typealias VarintResult = Join<Long, Int> // Value j BytesRead

/** Column metadata for describing table structure */
data class ColumnMeta(
    val name: String,
    val type: IOMemento
)

/** Represents an I/O mapping for different SQL data types */
enum class IOMemento {
    IoNull,
    IoInt,
    IoLong,
    IoDouble,
    IoString,
    IoBlob;
    
    val isSigned: Boolean get() = this == IoInt || this == IoLong || this == IoDouble
    val isText: Boolean get() = this == IoString
    val isBlob: Boolean get() = this == IoBlob
}

/** A row vector in a result set */
typealias RowVec = Series<Join<Any?, () -> ColumnMeta>>

/** A cursor for navigating result sets */
typealias Cursor = Series<RowVec>

// --- Parsing Logic ---

/** Helper to read big-endian Short (UInt16) from a Series<Byte> */
fun Series<Byte>.readUInt16BE(offset: Long): Int {
    require(offset + 1 < this.size) { "Read out of bounds for UInt16BE at offset $offset" }
    // Manual Big Endian conversion
    return ((this[offset.toInt()].toInt() and 0xFF) shl 8) or (this[(offset + 1).toInt()].toInt() and 0xFF)
}

/** Helper to read big-endian Int (UInt32) from a Series<Byte> */
fun Series<Byte>.readUInt32BE(offset: Long): Int {
    require(offset + 3 < this.size) { "Read out of bounds for UInt32BE at offset $offset" }
    // Manual Big Endian conversion
    return ((this[offset.toInt()].toInt() and 0xFF) shl 24) or
           ((this[(offset + 1).toInt()].toInt() and 0xFF) shl 16) or
           ((this[(offset + 2).toInt()].toInt() and 0xFF) shl 8) or
           (this[(offset + 3).toInt()].toInt() and 0xFF)
}

/** Helper to read big-endian Long (UInt64) from a Series<Byte> */
fun Series<Byte>.readUInt64BE(offset: Long): Long {
    require(offset + 7 < this.size) { "Read out of bounds for UInt64BE at offset $offset" }
    var value = 0L
    for (i in 0..7) {
        value = (value shl 8) or (this[(offset + i).toInt()].toLong() and 0xFF)
    }
    return value
}

/**
 * Parses the main 100-byte SQLite header.
 */
fun parseSQLiteHeader(fileBytes: Series<Byte>): SQLiteHeaderInfo {
    require(fileBytes.size >= SQLITE_HEADER_SIZE) { "File too small for SQLite header (${fileBytes.size} bytes)" }

    // Slice and decode magic string
    val magicBytes = ByteArray(16) { fileBytes[it].toByte() }.decodeToString()
    require(magicBytes == SQLITE_MAGIC_STRING) { "Invalid SQLite magic string: $magicBytes" }

    val pageSize = fileBytes.readUInt16BE(16L)
    val writeVersion = fileBytes[18].toInt()
    val readVersion = fileBytes[19].toInt()
    val fileChangeCounter = fileBytes.readUInt32BE(24L)
    val dbSizeInPages = fileBytes.readUInt32BE(28L)
    val firstFreelistPage = fileBytes.readUInt32BE(32L)
    val totalFreelistPages = fileBytes.readUInt32BE(36L)
    val schemaCookie = fileBytes.readUInt32BE(40L)
    val textEncoding = fileBytes.readUInt32BE(56L)

    // Basic validation
    require(pageSize == 1 || (pageSize >= 512 && pageSize <= 65536 && pageSize % 2 == 0)) { 
        "Invalid page size: $pageSize" 
    }
    require(textEncoding in 1..3) { "Unsupported text encoding: $textEncoding" }
    val actualPageSize = if (pageSize == 1) 65536 else pageSize

    return SQLiteHeaderInfo(
        pageSize = actualPageSize,
        fileFormatWriteVersion = writeVersion,
        fileFormatReadVersion = readVersion,
        textEncoding = textEncoding,
        schemaCookie = schemaCookie,
        fileChangeCounter = fileChangeCounter,
        databaseSizeInPages = dbSizeInPages,
        firstFreelistTrunkPage = firstFreelistPage,
        totalFreelistPages = totalFreelistPages
    )
}

/**
 * Parses the header of a B-Tree page.
 * Assumes the pageBytes Series starts exactly at the beginning of the page content.
 */
fun parsePageHeader(pageBytes: Series<Byte>, headerOffset: Int): PageHeaderInfo {
    require(pageBytes.size >= headerOffset + 8) { "Page data too small for header" }

    val pageType = pageBytes[headerOffset]
    val firstFreeblock = pageBytes.readUInt16BE((headerOffset + 1).toLong())
    val cellCount = pageBytes.readUInt16BE((headerOffset + 3).toLong())
    val cellContentArea = pageBytes.readUInt16BE((headerOffset + 5).toLong())
    val fragmentedBytes = pageBytes[headerOffset + 7].toInt()

    val rightMostPointer = if (pageType == 0x02.toByte() || pageType == 0x05.toByte()) {
        // Interior pages have a right-most pointer
        require(pageBytes.size >= headerOffset + 12) { "Page data too small for interior page header" }
        pageBytes.readUInt32BE((headerOffset + 8).toLong())
    } else {
        null
    }
    
    // 0 means 65536 for cell content area offset
    val actualCellContentArea = if (cellContentArea == 0) 65536 else cellContentArea

    return PageHeaderInfo(
        pageType = pageType,
        firstFreeblockOffset = firstFreeblock,
        cellCount = cellCount,
        cellContentAreaOffset = actualCellContentArea,
        fragmentedFreeBytes = fragmentedBytes,
        rightMostPointer = rightMostPointer
    )
}

/**
 * Reads the array of cell offsets within a given page.
 */
fun readCellPointerArray(pageBytes: Series<Byte>, pageHeader: PageHeaderInfo, headerSize: Int): Series<Int> {
    val cellCount = pageHeader.cellCount
    val pointerArrayStart = headerSize.toLong()

    return cellCount j { i ->
        val pointerOffset = pointerArrayStart + i * 2 // Each pointer is 2 bytes
        pageBytes.readUInt16BE(pointerOffset)
    }
}

/**
 * Parses a Varint from the byte series starting at the given offset.
 * Returns a Join of the parsed value (Long) and the number of bytes consumed (Int).
 */
fun Series<Byte>.parseVarint(offset: Long): VarintResult {
    var value = 0L
    var bytesRead = 0
    for (i in 0..8) { // Max 9 bytes for varint
        val currentOffset = offset + i
        require(currentOffset < this.size) { "Varint read out of bounds at offset $offset" }
        val byte = this[currentOffset.toInt()]
        bytesRead++
        val lower7bits = (byte.toLong() and 0x7F)
        value = (value shl 7) or lower7bits // Shift previous value left and OR in new 7 bits
        if ((byte.toInt() and 0x80) == 0) { // Check MSB (continuation bit)
            break // End of varint
        }
        if (i == 8) { // The 9th byte includes all 8 bits directly
            // The logic above already shifted the previous 8 * 7 bits.
            // Now, we need to incorporate the full last byte.
            val lastByteVal = this[currentOffset.toInt()].toLong() and 0xFF
            // Shift the accumulated value (which holds 8*7 bits) left by 8 bits
            // and OR the full last byte value.
            value = (value shl 1) or (lastByteVal and 0x01)
            break
        }
    }
    return value j bytesRead
}

/**
 * Parses a cell from a leaf table page (type 0x0d).
 * Returns a Join of RowID (nullable Long) and the payload Series<Byte>.
 */
fun parseLeafTableCell(cellBytes: Series<Byte>, hasRowId: Boolean): ParsedCell {
    var currentOffset = 0L

    // 1. Parse payload size (varint)
    val (payloadSize, payloadSizeBytes) = cellBytes.parseVarint(currentOffset)
    currentOffset += payloadSizeBytes

    // 2. RowID (Varint)
    val rowIdResult = if (hasRowId) {
        cellBytes.parseVarint(currentOffset)
    } else {
        (null as Long?) j 0
    }
    
    val rowId = rowIdResult.a
    currentOffset += rowIdResult.b

    // 3. Payload begins after RowID and payload size
    // We limit the payload to the actual declared size
    val endOffset = minOf(currentOffset + payloadSize, cellBytes.size.toLong())
    val payload = slice(cellBytes, currentOffset, endOffset)

    return rowId j payload
}

/**
 * Parses the Record Format - the core data within a payload.
 * Returns a RowVec suitable for the Cursor.
 */
fun parseRecord(payload: Series<Byte>, columnMetas: Series<ColumnMeta>, textEncoding: Int): RowVec {
    var cursor = 0L
    val (headerSize, headerBytesRead) = payload.parseVarint(cursor)
    cursor += headerBytesRead

    // Parse serial types (header)
    val serialTypes = mutableListOf<Int>()
    var headerCursor = cursor
    while (headerCursor < cursor + headerSize) {
        val (typeCode, typeBytesRead) = payload.parseVarint(headerCursor)
        serialTypes.add(typeCode.toInt())
        headerCursor += typeBytesRead
    }

    // Ensure serial types list size matches columnMetas size
    while (serialTypes.size < columnMetas.size) serialTypes.add(0) // Pad with NULL type
    if (serialTypes.size > columnMetas.size) {
        serialTypes.subList(columnMetas.size, serialTypes.size).clear() // Truncate extra types
    }

    // Data starts after the header
    var dataCursor = cursor + headerSize 

    // Parse values based on serial types
    val values = mutableListOf<Any?>()
    
    for (serialType in serialTypes) {
        val value: Any? = when {
            serialType == 0 -> null // NULL
            serialType == 1 -> payload[dataCursor.toInt()].toByte().also { dataCursor += 1 } // Int8
            serialType == 2 -> { // Int16
                val result = ((payload[dataCursor.toInt()].toInt() and 0xFF) or 
                        ((payload[(dataCursor + 1).toInt()].toInt() and 0xFF) shl 8)).toShort()
                dataCursor += 2
                result
            }
            serialType == 3 -> { // Int24
                val b1 = payload[dataCursor.toInt()].toInt() and 0xFF
                val b2 = payload[(dataCursor + 1).toInt()].toInt() and 0xFF
                val b3 = payload[(dataCursor + 2).toInt()].toInt() and 0xFF
                var v = b1 or (b2 shl 8) or (b3 shl 16)
                if ((b3 and 0x80) != 0) v = v or 0xFF000000.toInt() // Sign extend
                dataCursor += 3
                v
            }
            serialType == 4 -> { // Int32
                val result = payload.readUInt32BE(dataCursor)
                dataCursor += 4
                result
            }
            serialType == 5 -> { // Int48
                var v = 0L
                for (i in 0..5) {
                    v = (v shl 8) or (payload[(dataCursor + i).toInt()].toLong() and 0xFF)
                }
                // Sign extend if needed
                if ((payload[dataCursor.toInt()].toInt() and 0x80) != 0) {
                    v = v or 0xFFFF000000000000L
                }
                dataCursor += 6
                v
            }
            serialType == 6 -> { // Int64
                val result = payload.readUInt64BE(dataCursor)
                dataCursor += 8
                result
            }
            serialType == 7 -> { // Float64 (IEEE-754)
                val bits = payload.readUInt64BE(dataCursor)
                dataCursor += 8
                Double.fromBits(bits)
            }
            serialType == 8 -> 0 // Constant 0
            serialType == 9 -> 1 // Constant 1
            serialType >= 12 && serialType % 2 == 0 -> { // BLOB
                val length = ((serialType - 12) / 2)
                val bytes = ByteArray(length) { 
                    payload[(dataCursor + it).toInt()] 
                }
                dataCursor += length
                bytes
            }
            serialType >= 13 && serialType % 2 == 1 -> { // TEXT
                val length = ((serialType - 13) / 2)
                val bytes = ByteArray(length) { 
                    payload[(dataCursor + it).toInt()] 
                }
                dataCursor += length
                
                when (textEncoding) {
                    1 -> bytes.decodeToString() // UTF-8
                    2 -> "[UTF-16LE]" + bytes.decodeToString() // UTF-16LE (simplified)
                    3 -> "[UTF-16BE]" + bytes.decodeToString() // UTF-16BE (simplified)
                    else -> "[Enc:$textEncoding]" + bytes.decodeToString() // Fallback
                }
            }
            else -> "[Type:$serialType]" // Reserved or invalid types
        }
        values.add(value)
    }

    // Convert to RowVec (Series<Join<Any?, () -> ColumnMeta>>)
    return columnMetas.size j { i ->
        val columnMeta = columnMetas[i]
        val value = if (i < values.size) values[i] else null
        value j { columnMeta }
    }
}

/**
 * Main class to orchestrate SQLite file reading.
 */
class SQLiteReader(private val fileBytes: Series<Byte>) {

    val headerInfo: SQLiteHeaderInfo = parseSQLiteHeader(fileBytes)

    /** Reads the sqlite_schema table (always on page 1) */
    fun readSchema(): Series<SchemaDefinition> {
        // Define the fixed schema for sqlite_schema
        val schemaTableMeta = listOf(
            ColumnMeta("type", IOMemento.IoString),
            ColumnMeta("name", IOMemento.IoString),
            ColumnMeta("tbl_name", IOMemento.IoString),
            ColumnMeta("rootpage", IOMemento.IoInt),
            ColumnMeta("sql", IOMemento.IoString)
        ).toSeries()

        val schemaCursor = readTableInternal("sqlite_schema", 1, schemaTableMeta)

        // Convert the generic Cursor to a Series<SchemaDefinition>
        // Use temporary list to collect schema definitions
        val schemaList = mutableListOf<SchemaDefinition>()
        for (i in 0 until schemaCursor.size) {
            val rowVec = schemaCursor[i]
            val rowAccessor = RowAccessor(rowVec)
            schemaList.add(SchemaDefinition(
                type = rowAccessor.getString(0) ?: "",
                name = rowAccessor.getString(1) ?: "",
                tableName = rowAccessor.getString(2) ?: "",
                rootPage = rowAccessor.getInt(3) ?: 0,
                sqlDefinition = rowAccessor.getString(4) ?: ""
            ))
        }

        // Convert back to an idiomatic Series
        return schemaList.size j { i -> schemaList[i] }
    }

    /** Finds a specific table definition in the parsed schema */
    fun findTableSchema(schema: Series<SchemaDefinition>, tableName: String): SchemaDefinition? {
        for (i in 0 until schema.size) {
            val def = schema[i]
            if (def.type == "table" && def.name == tableName) {
                return def
            }
        }
        return null
    }

    /**
     * Reads a user table given its root page and EXPECTED column metadata.
     * Limitation: Does not parse SQL to determine schema automatically.
     */
    fun readTable(tableName: String, rootPageNum: Int, columnMetas: Series<ColumnMeta>): Cursor {
        require(rootPageNum > 0) { "Invalid root page number: $rootPageNum" }
        require(columnMetas.size > 0) { "Column metadata must be provided for table '$tableName'" }
        return readTableInternal(tableName, rootPageNum, columnMetas)
    }

    /** Internal helper to read data from a table's leaf page */
    private fun readTableInternal(
        tableName: String,
        pageNum: Int,
        columnMetas: Series<ColumnMeta>
    ): Cursor {
        val pageOffset = (pageNum - 1L) * headerInfo.pageSize
        require(pageOffset >= 0 && pageOffset + headerInfo.pageSize <= fileBytes.size) {
            "Calculated page offset for page $pageNum is out of file bounds."
        }

        // Page 1 has a 100-byte file header offset.
        val pageHeaderOffset = if (pageNum == 1) SQLITE_HEADER_SIZE else 0
        val pageBytes = slice(fileBytes, pageOffset, pageOffset + headerInfo.pageSize)

        val pageHeader = parsePageHeader(pageBytes, pageHeaderOffset)

        // --- Simplified: Assume it's a Leaf Table Page (0x0d) ---
        require(pageHeader.pageType == 0x0d.toByte()) {
            "Page $pageNum for table '$tableName' is not a leaf table page (type ${pageHeader.pageType})."
        }

        // Calculate actual page header size (8 bytes normally, 12 for interior)
        val actualHeaderSize = pageHeaderOffset + 8 // 8 for leaf page header itself

        val cellPointerOffsets = readCellPointerArray(pageBytes, pageHeader, actualHeaderSize)

        // Use a temporary mutable list for rows during parsing
        val rows = mutableListOf<RowVec>()

        for (i in 0 until cellPointerOffsets.size) {
            val cellOffset = cellPointerOffsets[i].toLong()
            
            // Validate the cell offset
            if (cellOffset >= pageBytes.size) {
                break // Stop processing cells for this page if offset is invalid
            }
            
            val cellBytes = slice(pageBytes, cellOffset, pageBytes.size.toLong())

            try {
                // Parse the cell (RowID + Payload)
                // true = table with rowid (standard)
                val (_, payload) = parseLeafTableCell(cellBytes, true)

                // Parse the record payload into a RowVec
                val rowVec = parseRecord(payload, columnMetas, headerInfo.textEncoding)
                rows.add(rowVec)
            } catch (e: Exception) {
                // Skip corrupt cells
                println("Error parsing cell $i in page $pageNum: ${e.message}")
            }
        }

        // Convert the collected rows to the idiomatic Cursor (Series<RowVec>)
        return rows.size j { i -> rows[i] }
    }
}

// --- Helper Classes and Extensions ---

/** RowAccessor helper for working with RowVec data */
class RowAccessor(private val row: RowVec) {
    @Suppress("UNCHECKED_CAST")
    fun <T> getValue(index: Int): T? {
        if (index < 0 || index >= row.size) return null
        return row[index].a as? T
    }
    
    fun getString(index: Int): String? = getValue<String>(index)
    fun getInt(index: Int): Int? = getValue<Int>(index)
    fun getLong(index: Int): Long? = getValue<Long>(index)
    fun getDouble(index: Int): Double? = getValue<Double>(index)
    fun getBlob(index: Int): ByteArray? = getValue<ByteArray>(index)
}

// Helper Functions

/** 
 * Creates a Series from a List
 */
fun <T> List<T>.toSeries(): Series<T> = this.size j { i -> this[i] }

/**
 * Creates a slice of a Series
 */
fun <T> slice(series: Series<T>, start: Long, endExclusive: Long): Series<T> {
    require(start >= 0) { "Start index must be non-negative" }
    require(endExclusive >= start) { "End index must be >= start index" }
    
    val size = minOf(endExclusive - start, series.size.toLong() - start)
    if (size <= 0) return 0 j { throw IndexOutOfBoundsException("Empty slice") }
    
    return size.toInt() j { i -> 
        val sourceIndex = start + i
        if (sourceIndex < series.size) series[sourceIndex.toInt()] 
        else throw IndexOutOfBoundsException("Index out of bounds: $sourceIndex")
    }
}

/** Simple min function to avoid using kotlin.math */
private fun minOf(a: Long, b: Long): Long = if (a <= b) a else b