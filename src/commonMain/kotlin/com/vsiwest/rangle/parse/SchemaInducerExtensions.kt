package com.vsiwest.rangle.parse

import com.vsiwest.rangle.Cursor
import com.vsiwest.rangle.Series
import com.vsiwest.rangle.j
import com.vsiwest.rangle.sqlite.ColumnMeta
import com.vsiwest.rangle.sqlite.IOMemento

/**
 * Converts a cursor of string values to a typed cursor based on the provided schema.
 * 
 * @param stringCursor The cursor containing string values
 * @param targetSchema The schema to convert to (as ColumnMeta Series)
 * @return A new cursor with values converted to the target types
 */
fun convertCursorTypes(stringCursor: Cursor, targetSchema: Series<ColumnMeta>): Cursor {
    return stringCursor.size j { rowIndex ->
        val stringRow = stringCursor.row(rowIndex)
        targetSchema.size j { colIndex ->
            val targetMeta = targetSchema[colIndex]
            val stringValue = stringRow[colIndex].a as? String // Assuming original cursor has strings

            val typedValue: Any? = try {
                if (stringValue.isNullOrBlank()) {
                    null // Or default value based on type?
                } else {
                    // Convert string to target type based on IOMemento
                    convertStringToType(stringValue, targetMeta.type)
                }
            } catch (e: Exception) {
                println("Warning: Type conversion failed for row $rowIndex, col $colIndex ('${targetMeta.name}'): value='$stringValue', target=${targetMeta.type}. Error: ${e.message}")
                null // Or throw, or use a default?
            }

            typedValue j { targetMeta } // Keep the target meta
        }
    }
}

/**
 * Converts a string value to the specified type based on IOMemento.
 */
private fun convertStringToType(value: String, type: IOMemento): Any? {
    return when (type) {
        IOMemento.IoBoolean -> value.lowercase() == "true"
        IOMemento.IoByte -> value.toByteOrNull()
        IOMemento.IoShort -> value.toShortOrNull()
        IOMemento.IoInt -> value.toIntOrNull()
        IOMemento.IoLong -> value.toLongOrNull()
        IOMemento.IoFloat -> value.toFloatOrNull()
        IOMemento.IoDouble -> value.toDoubleOrNull()
        IOMemento.IoString -> value
        // Add other types as needed
        else -> value // Default to string for unsupported types
    }
}

/**
 * Determines appropriate varchar sizes for string columns based on the evidence accumulators.
 * 
 * @param inducer The SchemaInducer that processed the data
 * @param columnMeta The column metadata
 * @param paddingFactor Optional padding factor (default 1.2 = 20% extra space)
 * @return A map of column names to varchar sizes
 */
fun determineVarcharSizes(
    inducer: SchemaInducer, 
    columnMeta: Series<ColumnMeta>,
    paddingFactor: Double = 1.2
): Map<String, Int> {
    val evidenceAccumulators = inducer.getEvidenceAccumulators()
    val result = mutableMapOf<String, Int>()
    
    for (i in 0 until columnMeta.size) {
        val meta = columnMeta[i]
        if (meta.type == IOMemento.IoString) {
            val evidence = evidenceAccumulators.getOrNull(i)
            val maxLength = evidence?.columnLength?.toInt() ?: 64
            // Add padding and ensure minimum size
            val varcharSize = maxOf(16, (maxLength * paddingFactor).toInt())
            result[meta.name] = varcharSize
        }
    }
    
    return result
}
