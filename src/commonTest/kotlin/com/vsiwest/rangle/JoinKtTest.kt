package com.vsiwest.rangle

import Join
import MetaSeries
import get
import kotlin.test.*

class JoinKtTest {

    @Test
    fun safeConvert() {
    }

    @Test
    fun get() {
    }

    @Test
    fun testMain() {
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
        println("  with Int 0: ${booleanSeries [0] }")
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
        println("  with Char: ${charSeries['B']}")//
//    //an exmaple where Join<A,B> will fail
//    val stringSeries: MetaSeries<String, Int> = object : Join<String, (String) -> Int> {
//        override val a: String = "Hello"
//        override val b: (String) -> Int = { it.length }
//    }
////    println("\nString series:")
//    println("  with Int: ${stringSeries[5]}")
//    println("  with Char: ${stringSeries['H']}") //Hello
//    println("  with Double: ${stringSeries[5.0]}") // This will fail
//    println("  with Boolean: ${stringSeries[true]}") // This will fail
    }
}