package com.vsiwest.rangle.parse

import com.vsiwest.rangle.sqlite.IOMemento

/**
 * Accumulates evidence about the type of data in a column by analyzing character patterns.
 */
data class TypeEvidence(
    var digits: UShort = 0U,
    var periods: UShort = 0U,
    var exponent: UShort = 0U,
    var signs: UShort = 0U,
    var special: UShort = 0U, // Non-alphanumeric, non-whitespace, non-punctuation typically used in numbers/bools
    var alpha: UShort = 0U,   // General letters
    var truefalse: UShort = 0U, // Letters specifically in 'true'/'false'
    var quotes: UShort = 0U,   // Single quotes
    var dquotes: UShort = 0U,  // Double quotes
    var whitespaces: UShort = 0U,
    var backslashes: UShort = 0U,
    var columnLength: UShort = 0U, // Max length observed
    var isEmpty: Boolean = false // Flag if empty/null was ever encountered
) {
    // Combine evidence from another instance (used for accumulating)
    fun accumulate(other: TypeEvidence) {
        digits = maxOf(digits, other.digits)
        periods = maxOf(periods, other.periods)
        exponent = maxOf(exponent, other.exponent)
        signs = maxOf(signs, other.signs)
        special = maxOf(special, other.special)
        alpha = maxOf(alpha, other.alpha)
        truefalse = maxOf(truefalse, other.truefalse)
        quotes = maxOf(quotes, other.quotes)
        dquotes = maxOf(dquotes, other.dquotes)
        whitespaces = maxOf(whitespaces, other.whitespaces)
        backslashes = maxOf(backslashes, other.backslashes)
        columnLength = maxOf(columnLength, other.columnLength)
        isEmpty = isEmpty || other.isEmpty // If either saw empty, the column might be nullable
    }

    companion object {
        // Function to analyze a single cell value (String)
        fun analyzeCell(cellValue: String?): TypeEvidence {
            val evidence = TypeEvidence()
            if (cellValue == null || cellValue.isEmpty()) {
                evidence.isEmpty = true
                return evidence
            }

            val trimmedValue = cellValue.trim()
            if (trimmedValue.isEmpty()) {
                evidence.isEmpty = true
                // Still record original length if just whitespace
                evidence.columnLength = maxOf(evidence.columnLength, cellValue.length.toUShort())
                evidence.whitespaces = evidence.columnLength // All whitespace
                return evidence
            }

            evidence.columnLength = maxOf(evidence.columnLength, trimmedValue.length.toUShort())

            for (char in trimmedValue) {
                when (char) {
                    in '0'..'9' -> evidence.digits++
                    '.' -> evidence.periods++
                    'e', 'E' -> evidence.exponent++
                    '+', '-' -> evidence.signs++
                    't', 'r', 'u', 'f', 'a', 'l', 's', // case-insensitive check
                    'T', 'R', 'U', 'F', 'A', 'L', 'S' -> evidence.truefalse++
                    in 'a'..'z', in 'A'..'Z' -> evidence.alpha++ // Count general alpha separately
                    '"' -> evidence.dquotes++
                    '\'' -> evidence.quotes++
                    '\\' -> evidence.backslashes++
                    ' ', '\t' -> evidence.whitespaces++ // Count trimmed whitespace if any remains
                    else -> evidence.special++ // Catch other symbols $, %, etc.
                }
            }
            // Refine alpha count if it was purely true/false letters
            if (evidence.alpha == evidence.truefalse && evidence.alpha > 0u) {
                 evidence.alpha = 0u // It wasn't general alpha, just bool letters
            } else {
                 // If alpha exists beyond bool letters, increment it (simplistic)
                 if (evidence.alpha > 0u) evidence.alpha = UShort.MAX_VALUE // Mark as definitely alpha
            }

            return evidence
        }

        // Deduction logic
        fun deduce(evidence: TypeEvidence): IOMemento {
             // If it ever contained non-numeric/non-bool relevant chars, it's string/blob
             if (evidence.alpha > 0u || evidence.special > 0u || evidence.quotes > 0u || evidence.dquotes > 0u || evidence.backslashes > 0u) {
                 // Could distinguish Blob later if needed based on content analysis
                 return IOMemento.IoString
             }

            // Check for boolean possibilities
             if (evidence.truefalse > 0u && evidence.digits == 0u && evidence.periods == 0u && evidence.exponent == 0u && evidence.signs == 0u) {
                 val boolLength = evidence.columnLength.toInt()
                 if (boolLength in 4..5) { // "true" or "false"
                      // A more robust check would involve matching the actual string
                      return IOMemento.IoBoolean
                 }
                 // If true/false letters seen but length wrong or digits present, fallback
                 return IOMemento.IoString
             }

             // If only digits and maybe a sign -> integer types
             if (evidence.periods == 0u && evidence.exponent == 0u && evidence.special == 0u) {
                 if (evidence.digits == 0u && evidence.signs <= 1u && evidence.isEmpty) return IOMemento.IoInt // Default nullable int if only sign seen or empty
                 if (evidence.digits == 0u && evidence.signs > 1u) return IOMemento.IoString // Multiple signs invalid

                 val maxLen = evidence.columnLength.toInt()
                 val hasSign = evidence.signs > 0u

                 // Consider length limits *with* potential sign
                 return when {
                     maxLen <= (if (hasSign) 4 else 3) -> IOMemento.IoByte // -128 to 127
                     maxLen <= (if (hasSign) 6 else 5) -> IOMemento.IoShort // -32768 to 32767
                     maxLen <= (if (hasSign) 11 else 10) -> IOMemento.IoInt   // Approx +/- 2 billion
                     maxLen <= (if (hasSign) 20 else 19) -> IOMemento.IoLong  // Approx +/- 9 quintillion
                     else -> IOMemento.IoString // Too long for Long
                 }
             }

             // Check for floating-point types (allow one period, one exponent, one sign)
             if (evidence.periods <= 1u && evidence.exponent <= 1u && evidence.signs <= 1u && evidence.special == 0u) {
                 // Could refine based on max length vs Float/Double representation limits,
                 // but Double is a safe default for floating point.
                 return IOMemento.IoDouble
             }

             // Default fallback
             return IOMemento.IoString
        }
    }
}
