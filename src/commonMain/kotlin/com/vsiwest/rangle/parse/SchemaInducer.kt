package com.vsiwest.rangle.parse

import com.vsiwest.rangle.Series
import com.vsiwest.rangle.j
import com.vsiwest.rangle.sqlite.ColumnMeta
import com.vsiwest.rangle.sqlite.IOMemento

/**
 * Analyzes sample data (rows of strings) to induce a schema (Series<IOMemento>).
 */
class SchemaInducer {
    private val evidenceAccumulators: MutableList<TypeEvidence> = mutableListOf()
    private var processedRowCount: Int = 0
    private var maxColumnsSeen: Int = 0

    /** Ensures the accumulator list has enough entries for the given column count */
    private fun ensureSize(columnCount: Int) {
        maxColumnsSeen = maxOf(maxColumnsSeen, columnCount)
        while (evidenceAccumulators.size < maxColumnsSeen) {
            evidenceAccumulators.add(TypeEvidence())
        }
    }

    /** Process a single cell value for a given column index */
    fun processCell(columnIndex: Int, cellValue: String?) {
        ensureSize(columnIndex + 1)
        val cellEvidence = TypeEvidence.analyzeCell(cellValue)
        evidenceAccumulators[columnIndex].accumulate(cellEvidence)
    }

    /** Process a full row of cell values (represented as strings) */
    fun processRow(row: List<String?>) {
        ensureSize(row.size)
        row.forEachIndexed(::processCell)
        processedRowCount++
    }

    /** Process a full row from a Series */
    fun processRow(row: Series<Any?>) {
        ensureSize(row.size)
        for (i in 0 until row.size) {
            processCell(i, row[i]?.toString()) // Convert to String for analysis
        }
        processedRowCount++
    }

    /** Process an iterable source of rows, limiting to a sample size */
    fun processSample(source: Iterable<List<String?>>, maxSamples: Int = 1000) {
        source.take(maxSamples).forEach(::processRow)
    }

    /** Process an iterable source of Series rows, limiting to a sample size */
    fun processSeriesSample(source: Iterable<Series<Any?>>, maxSamples: Int = 1000) {
        source.take(maxSamples).forEach(::processRow)
    }

    /**
     * Deduce the final schema based on accumulated evidence.
     * @return A Series where each element is the deduced IOMemento for the corresponding column.
     */
    fun getSchema(): Series<IOMemento> {
        if (processedRowCount == 0) {
            // Handle case where no data was processed - maybe return all Strings or empty?
            return evidenceAccumulators.size j { IOMemento.IoString } // Default to String
        }
        return evidenceAccumulators.size j { index ->
            TypeEvidence.deduce(evidenceAccumulators[index])
        }
    }

    /**
     * Create column metadata using the deduced schema and provided column names.
     * @param columnNames Names to use for the columns (defaults to "column_N" if not provided)
     * @return A Series of ColumnMeta objects
     */
    fun createColumnMeta(columnNames: List<String>? = null): Series<ColumnMeta> {
        val schema = getSchema()
        return schema.size j { i ->
            val name = columnNames?.getOrNull(i) ?: "column_$i"
            ColumnMeta(name, schema[i])
        }
    }

    /** Get the maximum number of columns encountered during processing */
    fun getColumnCount(): Int = maxColumnsSeen

    /** Get the number of rows processed */
    fun getProcessedRowCount(): Int = processedRowCount

    /** Get the evidence accumulators for advanced analysis */
    fun getEvidenceAccumulators(): List<TypeEvidence> = evidenceAccumulators.toList()

    override fun toString(): String {
        return "SchemaInducer(processedRows=$processedRowCount, maxCols=$maxColumnsSeen, evidence=$evidenceAccumulators)"
    }
}
