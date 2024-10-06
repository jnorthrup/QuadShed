package com.vsiwest


/**fun assert(value: Boolean)
(JVM source) (Native source)
For JVM
Throws an AssertionError if the value is false and runtime assertions have been enabled on the JVM using the -ea JVM option.

For Native
Throws an AssertionError if the value is false and runtime assertions have been enabled during compilation.*/

expect fun assert(value: kotlin.Boolean)

        @Throws(AssertionError::class)
 expect inline fun assert(value: kotlin.Boolean, lazyMessage: () -> Any)
