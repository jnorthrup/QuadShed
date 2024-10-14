typealias MetaSeries<K, V> = Join<K, (K) -> V>

interface Join<out A, out B> {
    val a: A
    val b: B
}

// Safe conversion extension function
@Suppress("UNCHECKED_CAST")
inline fun <A : Comparable<A>, P : Comparable<P>> A.safeConvert(foreign: P): A = when (this) {
    is Boolean -> when (foreign) {
        is Number -> foreign.toInt() != 0
        is Boolean -> foreign
        else -> throw UnsupportedOperationException("Cannot convert ${foreign::class.simpleName} to Boolean")
    } as A
    is Byte -> when (foreign) {
        is Number -> foreign.toByte()
        is Char -> foreign.code.toByte()
        else -> throw UnsupportedOperationException("Cannot convert ${foreign::class.simpleName} to Byte")
    } as A
    is Short -> when (foreign) {
        is Number -> foreign.toShort()
        is Char -> foreign.code.toShort()
        else -> throw UnsupportedOperationException("Cannot convert ${foreign::class.simpleName} to Short")
    } as A

    is Int -> when (foreign) {
        is Number -> foreign.toInt()
        is Char -> foreign.code
        else -> throw UnsupportedOperationException("Cannot convert ${foreign::class.simpleName} to Int")
    } as A
    is Long -> when (foreign) {
        is Number -> foreign.toLong()
        is Char -> foreign.code.toLong()
        else -> throw UnsupportedOperationException("Cannot convert ${foreign::class.simpleName} to Long")
    } as A
    is Float -> when (foreign) {
        is Number -> foreign.toFloat()
        is Char -> foreign.code.toFloat()
        else -> throw UnsupportedOperationException("Cannot convert ${foreign::class.simpleName} to Float")
    } as A
    is Double -> when (foreign) {
        is Number -> foreign.toDouble()
        is Char -> foreign.code.toDouble()
        else -> throw UnsupportedOperationException("Cannot convert ${foreign::class.simpleName} to Double")
    } as A
    is Char -> when (foreign) {
        is Number -> foreign.toInt().toChar()
        is Char -> foreign
        else -> throw UnsupportedOperationException("Cannot convert ${foreign::class.simpleName} to Char")
    } as A
    else -> throw UnsupportedOperationException("Unsupported conversion to: ${this::class.simpleName}")
}

// Flexible get function for MetaSeries
inline operator fun <K : Comparable<K>, V, P : Comparable<P>> MetaSeries<K, V>.get(index: P): V {
    val convertedIndex: K = this.a.safeConvert(index)
    return b(convertedIndex)
}

// Example usage
fun main() {
    // Create MetaSeries for each primitive type
    val booleanSeries: MetaSeries<Boolean, String> = object : Join<Boolean, (Boolean) -> String> {
        override val a: Boolean = true
        override val b: (Boolean) -> String = { if (it) "True" else "False" }
    }

    val byteSeries: MetaSeries<Byte, Int> = object : Join<Byte, (Byte) -> Int> {
        override val a: Byte = 127
        override val b: (Byte) -> Int = { it.toInt() * 2 }
    }

    val shortSeries: MetaSeries<Short, Double> = object : Join<Short, (Short) -> Double> {
        override val a: Short = 32767
        override val b: (Short) -> Double = { it.toDouble() / 2 }
    }

    val intSeries: MetaSeries<Int, String> = object : Join<Int, (Int) -> String> {
        override val a: Int = 1000000
        override val b: (Int) -> String = { "Value $it" }
    }

    val longSeries: MetaSeries<Long, Float> = object : Join<Long, (Long) -> Float> {
        override val a: Long = 1000000000L
        override val b: (Long) -> Float = { it.toFloat() / 1000 }
    }

    val floatSeries: MetaSeries<Float, Long> = object : Join<Float, (Float) -> Long> {
        override val a: Float = 3.14f
        override val b: (Float) -> Long = { (it * 1000).toLong() }
    }

    val doubleSeries: MetaSeries<Double, Int> = object : Join<Double, (Double) -> Int> {
        override val a: Double = 3.14159265359
        override val b: (Double) -> Int = { (it * 100).toInt() }
    }

    val charSeries: MetaSeries<Char, String> = object : Join<Char, (Char) -> String> {
        override val a: Char = 'Z'
        override val b: (Char) -> String = { it.toString().repeat(3) }
    }

    // Test with various index types
    println("Boolean series:")
    println("  with Int 0: ${booleanSeries[0]}")
    println("  with Int 1: ${booleanSeries[1]}")
    println("  with Double 0.0: ${booleanSeries[0.0]}")
    println("  with Double 0.1: ${booleanSeries[0.1]}")

    println("\nByte series:")
    println("  with Int: ${byteSeries[64]}")
    println("  with Double: ${byteSeries[64.5]}")

    println("\nShort series:")
    println("  with Int: ${shortSeries[1000]}")
    println("  with Long: ${shortSeries[1000L]}")

    println("\nInt series:")
    println("  with Int: ${intSeries[500000]}")
    println("  with Double: ${intSeries[500000.75]}")

    println("\nLong series:")
    println("  with Int: ${longSeries[500000000]}")
    println("  with Float: ${longSeries[500000000f]}")

    println("\nFloat series:")
    println("  with Int: ${floatSeries[3]}")
    println("  with Double: ${floatSeries[3.14]}")

    println("\nDouble series:")
    println("  with Int: ${doubleSeries[3]}")
    println("  with Float: ${doubleSeries[3.14f]}")

    println("\nChar series:")
    println("  with Int: ${charSeries[65]}")  // ASCII for 'A'
    println("  with Char: ${charSeries['B']}")
}