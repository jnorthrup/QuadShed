package com.vsiwest.rangle.sqlite

import com.vsiwest.rangle.j
import com.vsiwest.rangle.Series

/**
 * Example demonstrating how to use the SQLiteReader class to parse SQLite files.
 * This shows loading a file, reading the schema, and accessing table data.
 */
object SQLiteExample {
    
    /**
     * Convert a ByteArray to a Series<Byte>
     */
    fun ByteArray.toSeries(): Series<Byte> = this.size j { i -> this[i] }
    
    /**
     * Prints the schema entries found in a SQLite database
     */
    fun printSchema(schema: Series<SchemaDefinition>) {
        println("Schema contains ${schema.size} entries:")
        for (i in 0 until schema.size) {
            val def = schema[i]
            println("  ${def.type}: ${def.name} (page ${def.rootPage})")
        }
    }
    
    /**
     * Prints the content of a table's cursor
     */
    fun printTable(tableName: String, cursor: Cursor, limit: Int = 10) {
        println("\nTable '$tableName' data (${cursor.size} rows):")
        
        // Print at most 'limit' rows
        val rowsToPrint = minOf(cursor.size, limit)
        
        // Print each row
        for (i in 0 until rowsToPrint) {
            val rowVec = cursor[i]
            val rowAccessor = RowAccessor(rowVec)
            
            print("  Row $i: ")
            val values = mutableListOf<String>()
            
            for (j in 0 until rowVec.size) {
                val colMeta = rowVec[j].b.invoke()
                val value = rowVec[j].a
                values.add("${colMeta.name}=${formatValue(value)}")
            }
            
            println(values.joinToString(", "))
        }
        
        if (cursor.size > limit) {
            println("  ... and ${cursor.size - limit} more rows")
        }
    }
    
    /**
     * Format a value for display
     */
    private fun formatValue(value: Any?): String {
        return when (value) {
            null -> "NULL"
            is ByteArray -> "BLOB(${value.size} bytes)"
            is String -> "\"$value\""
            else -> value.toString()
        }
    }
    
    /**
     * Main example usage function
     */
    fun readSQLiteFile(filePath: String) {
        println("Reading SQLite file: $filePath")
        
        try {
            // Read file bytes (platform-specific implementation)
            val fileBytes = readFileBytes(filePath)
            
            // Create reader and parse header
            val reader = SQLiteReader(fileBytes.toSeries())
            
            println("SQLite file version: ${reader.headerInfo.fileFormatWriteVersion}.${reader.headerInfo.fileFormatReadVersion}")
            println("Page size: ${reader.headerInfo.pageSize} bytes")
            println("Encoding: ${getEncodingName(reader.headerInfo.textEncoding)}")
            println("Total pages: ${reader.headerInfo.databaseSizeInPages}")
            
            // Read schema
            val schema = reader.readSchema()
            printSchema(schema)
            
            // Find a specific table (e.g., 'users')
            val tableName = "users" // Change this to match a table in your database
            val tableSchema = reader.findTableSchema(schema, tableName)
            
            if (tableSchema != null) {
                println("\nFound '$tableName' table at page ${tableSchema.rootPage}")
                println("SQL: ${tableSchema.sqlDefinition}")
                
                // Define column metadata for the table (must be provided by the caller)
                // In a real application, this would be derived from the CREATE TABLE statement
                val columnMetas = listOf(
                    ColumnMeta("id", IOMemento.IoInt),
                    ColumnMeta("name", IOMemento.IoString),
                    ColumnMeta("email", IOMemento.IoString)
                    // Add more columns as needed for your table
                ).toSeries()
                
                // Read the table data
                val tableCursor = reader.readTable(tableName, tableSchema.rootPage, columnMetas)
                printTable(tableName, tableCursor)
            } else {
                println("\nTable '$tableName' not found in the schema.")
                
                // Alternatively, try reading the first table we find
                if (schema.size > 0) {
                    for (i in 0 until schema.size) {
                        val def = schema[i]
                        if (def.type == "table" && def.name != "sqlite_sequence") {
                            println("\nTrying to read table '${def.name}' instead")
                            
                            // This is just an example using a generic schema - in reality,
                            // you should provide the correct column metadata for the table
                            val genericMetas = listOf(
                                ColumnMeta("col1", IOMemento.IoInt),
                                ColumnMeta("col2", IOMemento.IoString),
                                ColumnMeta("col3", IOMemento.IoString)
                            ).toSeries()
                            
                            try {
                                val genericCursor = reader.readTable(def.name, def.rootPage, genericMetas)
                                printTable(def.name, genericCursor)
                                break
                            } catch (e: Exception) {
                                println("Error reading table '${def.name}': ${e.message}")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("Error processing SQLite file: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Get the name of the text encoding
     */
    private fun getEncodingName(encoding: Int): String {
        return when (encoding) {
            1 -> "UTF-8"
            2 -> "UTF-16LE"
            3 -> "UTF-16BE"
            else -> "Unknown ($encoding)"
        }
    }
    
    /**
     * Platform-specific file reading implementation.
     * This would need to be implemented differently for different platforms.
     */
    private fun readFileBytes(filePath: String): ByteArray {
        // This is a stub - in a real application, you would implement this
        // using platform-specific file I/O.
        // For example, on JVM:
        // return java.io.File(filePath).readBytes()
        
        throw NotImplementedError("Platform-specific file reading not implemented")
    }
}