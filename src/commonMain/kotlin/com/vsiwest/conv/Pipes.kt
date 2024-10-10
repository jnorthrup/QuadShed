package com.vsiwest.conv

import kotlin.math.*

class Pipe<T>(val value: T) {
    infix fun <R> `•`(transform: (T) -> R): Pipe<R> = Pipe(transform(value))
}

fun <T> T.startPipe() = Pipe(this)

// Helper functions for testing
fun isEven(n: Int) = n % 2 == 0
fun isOdd(n: Int) = n % 2 != 0
fun square(n: Int) = n * n
fun cube(n: Int) = n * n * n
fun factorial(n: Int): Int = if (n <= 1) 1 else n * factorial(n - 1)
fun isPrime(n: Int): Boolean {
    if (n <= 1) return false
    for (i in 2..sqrt(n.toDouble()).toInt())
        if (n % i == 0) return false
    return true
}

fun main() {
    // Test 1: Complex mathematical operations
    val mathPipeline = 5.startPipe() `•`
            Int::inc `•`
            ::square `•`
            ::isEven `•`
            { if (it) 1 else 0 } `•`
            ::factorial `•`
            ::isPrime

    println("Test 1: ${mathPipeline.value}")  // Should be true (720 is not prime)

    // Test 2: String manipulations
    val stringPipeline = "Hello World".startPipe() `•`
            String::uppercase `•`
            String::reversed  `•`
            { it.replace("A", "@") } `•`
            String::length `•`
            ::isOdd

    println("Test 2: ${stringPipeline.value}")  // Should be true (11 is odd)

    // Test 3: Exception handling
    val exceptionPipeline = { input: String ->
        input.startPipe() `•`
                {
                    try {
                        it.toInt()
                    } catch (e: NumberFormatException) {
                        -1
                    }
                } `•`
                ::square `•`
                { it > 0 }
    }

    println("Test 3a: ${exceptionPipeline("123").value}")  // Should be true
    println("Test 3b: ${exceptionPipeline("abc").value}")  // Should be false

    // Test 4: Pipeline with different types
    val mixedTypePipeline = 30.startPipe() `•`
            Int::toDouble `•`
             ::sin `•`
            { it > 0 } `•`
            { if (it) "Positive" else "Non-positive" } `•`
            String::length `•`
            ::isEven

    println("Test 4: ${mixedTypePipeline.value}")  // Should be true ("Positive" has 8 letters, which is even)

    // Test 5: Empty string and zero handling
    val edgeCasePipeline = { input: String ->
        input.startPipe() `•`
                { if (it.isEmpty()) 0 else it.length } `•`
                ::factorial `•`
                ::isPrime `•`
                { if (it) 1.0 else 0.0 } `•`
                ::cos `•`
                { it == 0.54 }
    }

    println("Test 5a: ${edgeCasePipeline("").value}")     // Should be true (cos(1.0) ≈ 0.54)
    println("Test 5b: ${edgeCasePipeline("a").value}")    // Should be true (1! = 1, which is prime)
    println("Test 5c: ${edgeCasePipeline("abcd").value}") // Should be false (24 is not prime)

    // Test 6: Large number handling
    val largePipeline = 1234567890L.startPipe() `•`
            { it * it } `•`
            { (it % 1000000007).toInt() } `•` // Common modulo operation to handle large numbers
            ::isPrime

    println("Test 6: ${largePipeline.value}" )  // Test with a large number
}