package com.vsiwest.db.sqlite

typealias Page = ByteArray
typealias Header = ByteArray
typealias Cell = ByteArray

data class DatabaseHeader(
    val magicHeader: String,
    val pageSize: Int,
    val fileFormatWriteVersion: Int,
    val fileFormatReadVersion: Int,
    val reservedSpace: Int,
    val maxPayloadFraction: Int,
    val minPayloadFraction: Int,
    val leafPayloadFraction: Int,
    val fileChangeCounter: Int,
    val databaseSizeInPages: Int,
    val firstFreelistTrunkPage: Int,
    val totalFreelistPages: Int,
    val schemaCookie: Int,
    val schemaFormatNumber: Int,
    val defaultPageCacheSize: Int,
    val largestRootBTreePage: Int,
    val textEncoding: Int,
    val userVersion: Int,
    val incrementalVacuumMode: Boolean,
    val applicationId: Int,
    val reservedForExpansion: ByteArray,
    val versionValidForNumber: Int,
    val sqliteVersionNumber: Int
)

data class BTreePage(
    val pageType: Int,
    val firstFreeblock: Int,
    val numberOfCells: Int,
    val cellContentOffset: Int,
    val fragmentedFreeBytes: Int,
    val rightMostPointer: Int?,
    val cells: List<Cell>
)

fun parseDatabaseHeader(bytes: ByteArray): DatabaseHeader {
    // Parse the byte array to extract the database header fields
    // Return a DatabaseHeader instance
    return DatabaseHeader(
        magicHeader = String(bytes.sliceArray(0 until 16)),
        pageSize = bytes.sliceArray(16 until 18).toInt(),
        fileFormatWriteVersion = bytes[18].toInt(),
        fileFormatReadVersion = bytes[19].toInt(),
        reservedSpace = bytes[20].toInt(),
        maxPayloadFraction = bytes[21].toInt(),
        minPayloadFraction = bytes[22].toInt(),
        leafPayloadFraction = bytes[23].toInt(),
        fileChangeCounter = bytes.sliceArray(24 until 28).toInt(),
        databaseSizeInPages = bytes.sliceArray(28 until 32).toInt(),
        firstFreelistTrunkPage = bytes.sliceArray(32 until 36).toInt(),
        totalFreelistPages = bytes.sliceArray(36 until 40).toInt(),
        schemaCookie = bytes.sliceArray(40 until 44).toInt(),
        schemaFormatNumber = bytes.sliceArray(44 until 48).toInt(),
        defaultPageCacheSize = bytes.sliceArray(48 until 52).toInt(),
        largestRootBTreePage = bytes.sliceArray(52 until 56).toInt(),
        textEncoding = bytes.sliceArray(56 until 60).toInt(),
        userVersion = bytes.sliceArray(60 until 64).toInt(),
        incrementalVacuumMode = bytes.sliceArray(64 until 68).toInt() != 0,
        applicationId = bytes.sliceArray(68 until 72).toInt(),
        reservedForExpansion = bytes.sliceArray(72 until 92),
        versionValidForNumber = bytes.sliceArray(92 until 96).toInt(),
        sqliteVersionNumber = bytes.sliceArray(96 until 100).toInt()
    )
}

fun parseBTreePage(bytes: ByteArray): BTreePage {
    // Parse the byte array to extract the b-tree page fields
    // Return a BTreePage instance
    val pageType = bytes[0].toInt()
    val firstFreeblock = bytes.sliceArray(1 until 3).toInt()
    val numberOfCells = bytes.sliceArray(3 until 5).toInt()
    val cellContentOffset = bytes.sliceArray(5 until 7).toInt()
    val fragmentedFreeBytes = bytes[7].toInt()
    val rightMostPointer = if (pageType == 2 || pageType == 5) bytes.sliceArray(8 until 12).toInt() else null
    val cells = mutableListOf<Cell>()
    // Logic to parse cells from the byte array
    return BTreePage(pageType, firstFreeblock, numberOfCells, cellContentOffset, fragmentedFreeBytes, rightMostPointer, cells)
}

class SQLiteFileIndexer(private val fileBytes: ByteArray) {
    private val header: DatabaseHeader = parseDatabaseHeader(fileBytes.sliceArray(0 until 100))
    private val pages: List<Page> = fileBytes.drop(100).chunked(header.pageSize).map { it.toByteArray() }

    fun getPage(index: Int): Page = pages[index - 1]

    fun getBTreePage(index: Int): BTreePage = parseBTreePage(getPage(index))

    // Additional methods to navigate and interpret the file format
}
