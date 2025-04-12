package borg.trikeshed.diff // Or your preferred package

import com.vsiwest.rangle.sqlite.IOMemento
import com.vsiwest.rangle.parse.TypeEvidence

/** Represents the type of change detected for a specific column */
enum class ColumnChangeType {
    ADDED,      // Column exists in the new schema but not the old
    REMOVED,    // Column exists in the old schema but not the new
    TYPE_CHANGED, // Deduced IOMemento changed
    EVIDENCE_CHANGED, // Deduced type same, but underlying evidence differs significantly
    NO_CHANGE   // Column exists in both and is considered unchanged (optional to include)
}

/** Holds the details of changes for a single column */
data class ColumnDiff(
    val columnIndex: Int,
    val changeType: ColumnChangeType,
    val oldColumnName: String? = null, // If headers were provided/available
    val newColumnName: String? = null, // If headers were provided/available
    val oldType: IOMemento?,
    val newType: IOMemento?,
    val oldEvidence: TypeEvidence?,
    val newEvidence: TypeEvidence?,
    val detailedChanges: List<String> = emptyList() // Descriptions of specific evidence changes
) {
    override fun toString(): String {
        val nameInfo = when {
            oldColumnName != null && newColumnName != null && oldColumnName != newColumnName -> " (Name: '$oldColumnName' -> '$newColumnName')"
            oldColumnName != null && newColumnName == null -> " (Name: '$oldColumnName' -> <REMOVED>)" // Should align with REMOVED type
            oldColumnName == null && newColumnName != null -> " (Name: <ADDED> -> '$newColumnName')" // Should align with ADDED type
            newColumnName != null -> " (Name: '$newColumnName')"
            else -> ""
        }
        val typeInfo = when (changeType) {
            ColumnChangeType.TYPE_CHANGED -> "Type: $oldType -> $newType"
            ColumnChangeType.EVIDENCE_CHANGED -> "Type: $newType (Evidence Changed)"
            ColumnChangeType.ADDED -> "Type: $newType"
            ColumnChangeType.REMOVED -> "Type: $oldType"
            else -> "Type: $newType"
        }
        val details = if (detailedChanges.isNotEmpty()) "\n    Details: ${detailedChanges.joinToString(", ")}" else ""
        return "Col[$columnIndex]$nameInfo: $changeType - $typeInfo$details"
    }
}

/** Container for the overall schema comparison result */
data class SchemaDiff(
    val columnDiffs: Map<Int, ColumnDiff>, // Map from column index to its diff info
    val oldColumnCount: Int,
    val newColumnCount: Int
) {
    val hasChanges: Boolean
        get() = oldColumnCount != newColumnCount || columnDiffs.values.any { it.changeType != ColumnChangeType.NO_CHANGE }

    override fun toString(): String {
        if (!hasChanges) return "Schemas are identical (Columns: $newColumnCount)."
        val sb = StringBuilder()
        sb.appendLine("Schema Diff Report:")
        sb.appendLine("  Column Count: $oldColumnCount -> $newColumnCount")
        val changedCols = columnDiffs.values.filter { it.changeType != ColumnChangeType.NO_CHANGE }
        if (changedCols.isNotEmpty()) {
            sb.appendLine("  Changes:")
            changedCols.sortedBy { it.columnIndex }.forEach { diff ->
                sb.appendLine("    $diff")
            }
        } else if (oldColumnCount != newColumnCount) {
             sb.appendLine("  (Only column count changed)")
        }
        return sb.toString()
    }
}
