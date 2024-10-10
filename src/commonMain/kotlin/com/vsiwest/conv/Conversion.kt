@file:Suppress("UNCHECKED_CAST")

package com.vsiwest.conv


import com.vsiwest.*
import com.vsiwest.bitops.CZero.bool
import com.vsiwest.bitops.CZero.nz
import com.vsiwest.meta.IOMemento
import com.vsiwest.meta.ioMemento
import kotlin.math.abs

/**
 * Converts an instance of type [A] to type [B] using the specified [IOMemento] instances for encoding and decoding.
 *
 * This function assumes:
 * - Numericals are in the same byte order.
 * - Byte arrays from numbers are binary and not [toString].
 * - Booleans are represented as false = 0, null, unit; true = 1; reversible as needed, but also 't', 'f' in any numerical form as well as '0', '1' respectively.
 * - Doubles and floats will do thenonce comparison and not exact comparison.
 * - Booleans serve back binary numerical scalar values only but will read the options above.
 * - Destinations to series, arrays, strings, will use [CharSequence] bytes by any means available e.g. [toString].
 *
 * @param A The source type.
 * @param B The target type.
 * @param to The [IOMemento] instance used for the target type.
 * @return A function that converts an instance of type [A] to type [B].
 */
inline fun <reified A,reified B> IOMemento.conversion(to: IOMemento): (A) -> B = { a: A ->
    val from = this
    val fromEncoder = from.createEncoder(from.networkSize ?: 0)
    val fromDecoder = from.createDecoder(from.networkSize ?: 0)
    val toEncoder = to.createEncoder(to.networkSize ?: 0)
    val toDecoder = to.createDecoder(to.networkSize ?: 0)

    // Special handling for boolean input
    val fromBytes = when {
        from == IOMemento.IoBoolean && a is Boolean -> byteArrayOf(if (a) 1 else 0)
        else -> fromEncoder(a)
    }

    // Intermediate conversion, handling special cases
    val intermediateValue = when {
        to == IOMemento.IoBoolean -> when {
            fromBytes.isEmpty() -> false
            fromBytes.size == 1 -> fromBytes[0] != 0.toByte()
            else -> {
                val str = fromBytes.decodeToString().lowercase()
                str == "true" || str == "1" || str == "t"
            }
        }

        else -> fromDecoder(fromBytes)
    }

    // Special handling for boolean output
    val toBytes = when {
        to == IOMemento.IoBoolean -> byteArrayOf(if (intermediateValue as Boolean) 1 else 0)
        else -> toEncoder(intermediateValue)
    }

    @Suppress("UNCHECKED_CAST") val result = toDecoder(toBytes) as B

    // Handle approximate equality for floating-point types
    when {
        (from == IOMemento.IoFloat || from == IOMemento.IoDouble) && (to == IOMemento.IoFloat || to == IOMemento.IoDouble) -> {
            val epsilon = 1e-6
            val aDouble = (a as? Float)?.toDouble() ?: a as Double
            val resultDouble = (result as? Float)?.toDouble() ?: result as Double
            if (abs(aDouble - resultDouble) > epsilon) {
                throw IllegalArgumentException("Floating-point conversion mismatch")
            }
        }
    }

    // Handle special cases for destination types
    when (result) {
        is CharArray -> {
            (result as CharArray).concatToString() as B
        }

        is ByteArray -> (result as? CharSequence)?.toString()?.encodeToByteArray() ?: result as B
        is String -> (result as? CharSequence)?.toString() ?: result.toString() as B
        else -> result
    }
} as (A) -> B
fun <K : Comparable<K>, V> MetaSeries<K, V>.last() = b.invoke(a.duckDec(a))

// Example usage and test cases
fun main() {
    // Test boolean conversions
    val boolToInt = IOMemento.IoBoolean.conversion<Boolean, Int>(IOMemento.IoInt)
    println("Boolean to Int: ${boolToInt(true)}, ${boolToInt(false)}")

    val intToBool = IOMemento.IoInt.conversion<Int, Boolean>(IOMemento.IoBoolean)
    println("Int to Boolean: ${intToBool(1)}, ${intToBool(0)}")

    // Test numeric conversions
    val intToLong = IOMemento.IoInt.conversion<Int, Long>(IOMemento.IoLong)
    println("Int to Long: ${intToLong(42)}")

    val doubleToFloat = IOMemento.IoDouble.conversion<Double, Float>(IOMemento.IoFloat)
    println("Double to Float: ${doubleToFloat(3.14159265359)}")

    // Test string and char sequence conversions
    val stringToCharArray = IOMemento.IoString.conversion<String, CharArray>(IOMemento.IoCharSeries)
    println("String to CharArray: ${stringToCharArray("Hello").contentToString()}")

    val charArrayToString = IOMemento.IoCharSeries.conversion<CharArray, String>(IOMemento.IoString)
    println("CharArray to String: ${charArrayToString(charArrayOf('W', 'o', 'r', 'l', 'd'))}")

    // Test boolean string representations
    val stringToBool = IOMemento.IoString.conversion<String, Boolean>(IOMemento.IoBoolean)
    println(
        "String to Boolean: ${stringToBool("true")}, ${stringToBool("false")}, ${stringToBool("1")}, ${
            stringToBool(
                "0"
            )
        }, ${stringToBool("t")}, ${stringToBool("f")}"
    )

    // Test byte array conversions
    val stringToByteArray = IOMemento.IoString.conversion<String, ByteArray>(IOMemento.IoByteArray)
    println("String to ByteArray: ${stringToByteArray("Hello").contentToString()}")

    val byteArrayToString = IOMemento.IoByteArray.conversion<ByteArray, String>(IOMemento.IoString)
    println("ByteArray to String: ${byteArrayToString(byteArrayOf(72, 101, 108, 108, 111))}")

    // Test approximate float/double comparison
    val floatToDouble = IOMemento.IoFloat.conversion<Float, Double>(IOMemento.IoDouble)
    println("Float to Double: ${floatToDouble(3.14f)}")

    try {
        val invalidBoolString = IOMemento.IoString.conversion<String, Boolean>(IOMemento.IoBoolean)
        invalidBoolString("invalid")
    } catch (e: IllegalArgumentException) {
        println("Caught expected exception for invalid boolean string: ${e.message}")
    }
}

/* uses conversion on K to Int */
inline fun <reified K : Comparable<K>, V> MetaSeries<K, V>.toSeries(): Series<V> = when (a) {

         conversion(a.ioMemento,Int.ioMemento)