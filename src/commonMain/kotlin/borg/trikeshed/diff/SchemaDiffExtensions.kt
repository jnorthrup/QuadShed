package borg.trikeshed.diff

import com.vsiwest.rangle.Series
import com.vsiwest.rangle.get
import com.vsiwest.rangle.lib.size
import com.vsiwest.rangle.parse.SchemaInducer
import com.vsiwest.rangle.parse.TypeEvidence
import kotlin.math.abs

/** Compares two TypeEvidence instances and returns a list of descriptive changes */
fun compareEvidenceDetails(old: TypeEvidence, new: TypeEvidence): List<String> {
    val changes = mutableListOf<String>()
    if (old.columnLength != new.columnLength) {
        changes.add("maxLength: ${old.columnLength} -> ${new.columnLength}")
    }
    if (old.isEmpty != new.isEmpty) {
        changes.add("nullable: ${old.isEmpty} -> ${new.isEmpty}")
    }
    // Check for significant character class changes
    if (old.alpha == 0u && new.alpha > 0u) changes.add("+alpha")
    if (old.periods == 0u && new.periods > 0u) changes.add("+periods")
    if (old.exponent == 0u && new.exponent > 0u) changes.add("+exponent")
    if (old.signs > 1u && new.signs <= 1u) changes.add("signs normalized") // Less likely but possible
    if (old.signs <= 1u && new.signs > 1u) changes.add("multiple signs appeared") // More likely indicates non-numeric
    if (old.special == 0u && new.special > 0u) changes.add("+special chars")
    if (old.quotes == 0u && new.quotes > 0u) changes.add("+single quotes")
    if (old.dquotes == 0u && new.dquotes > 0u) changes.add("+double quotes")
    if (old.backslashes == 0u && new.backslashes > 0u) changes.add("+backslashes")

    // Could add checks for disappearance too, e.g., if old.alpha > 0u && new.alpha == 0u
    // Could add checks for significant changes in digit counts if needed

    return changes
}

/**
 * Compares two schemas derived from SchemaInducers.
 *
 * @param oldInducer The SchemaInducer for the "before" state.
 * @param newInducer The SchemaInducer for the "after" state.
 * @param oldHeaders Optional list of header names for the old schema.
 * @param newHeaders Optional list of header names for the new schema.
 * @param includeNoChange Whether to include columns marked as NO_CHANGE in the result map.
 * @return A SchemaDiff object detailing the differences.
 */
fun compareSchemas(
    oldInducer: SchemaInducer,
    newInducer: SchemaInducer,
    oldHeaders: List<String>? = null,
    newHeaders: List<String>? = null,
    includeNoChange: Boolean = false
): SchemaDiff {
    val oldEvidenceList = oldInducer.getEvidenceAccumulators()
    val newEvidenceList = newInducer.getEvidenceAccumulators()
    val oldColCount = oldInducer.getColumnCount()
    val newColCount = newInducer.getColumnCount()
    val diffMap = mutableMapOf<Int, ColumnDiff>()

    val maxCols = maxOf(oldColCount, newColCount)

    for (i in 0 until maxCols) {
        val oldEv = oldEvidenceList.getOrNull(i)
        val newEv = newEvidenceList.getOrNull(i)
        val oldName = oldHeaders?.getOrNull(i)
        val newName = newHeaders?.getOrNull(i)

        val diff = when {
            oldEv != null && newEv != null -> { // Column potentially exists in both
                val oldType = TypeEvidence.deduce(oldEv)
                val newType = TypeEvidence.deduce(newEv)
                if (oldType != newType) {
                    ColumnDiff(i, ColumnChangeType.TYPE_CHANGED, oldName, newName, oldType, newType, oldEv, newEv)
                } else {
                    val details = compareEvidenceDetails(oldEv, newEv)
                    if (details.isNotEmpty() || oldName != newName) { // Include name changes as evidence change
                         ColumnDiff(i, ColumnChangeType.EVIDENCE_CHANGED, oldName, newName, oldType, newType, oldEv, newEv, details)
                    } else {
                         ColumnDiff(i, ColumnChangeType.NO_CHANGE, oldName, newName, oldType, newType, oldEv, newEv)
                    }
                }
            }
            oldEv == null && newEv != null -> { // Column Added
                val newType = TypeEvidence.deduce(newEv)
                ColumnDiff(i, ColumnChangeType.ADDED, oldName, newName, null, newType, null, newEv)
            }
            oldEv != null && newEv == null -> { // Column Removed
                val oldType = TypeEvidence.deduce(oldEv)
                ColumnDiff(i, ColumnChangeType.REMOVED, oldName, newName, oldType, null, oldEv, null)
            }
            else -> null // Should not happen if maxCols is calculated correctly
        }

        if (diff != null && (includeNoChange || diff.changeType != ColumnChangeType.NO_CHANGE)) {
            diffMap[i] = diff
        }
    }

    return SchemaDiff(diffMap, oldColCount, newColCount)
}
