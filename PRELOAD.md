# Typealias Cheatsheet for Trikeshed Project

## Core Tenets

1. Clarity over brevity
2. Composition over inheritance
3. Immutability by default
4. Lazy evaluation when possible
5. Leverage Series and Join as fundamental building blocks

## Rules

1. Always include full signatures in typealiases
2. Prefer typealiases over direct type definitions for complex types
3. Use meaningful names that describe the concept, not just the underlying type
4. Avoid nested typealiases when possible; flatten them if it improves readability

## Priorities

1. Type safety and compile-time checks
2. Code readability and maintainability
3. Performance optimization through lazy evaluation and immutable structures
4. Flexibility for extension and composition

## Subsumptions

1. Series subsumes List and Array functionalities
2. Join subsumes Pair and Triple
3. Immutable data structures subsume mutable ones (provide mutable interfaces when necessary)

## Tradeoffs

1. Verbosity vs. Inference: Prefer explicit types over relying on Kotlin's type inference
2. Abstraction vs. Concreteness: Balance between high-level concepts and low-level implementations
3. Generality vs. Specificity: Aim for reusable components without sacrificing domain-specific optimizations

## Key Typealiases to Remember

```kotlin
typealias Series<T> = Join<Int, (Int) -> T>
typealias Twin<T> = Join<T, T>
typealias Bucket<T> = Series<T>
typealias Version = Int
typealias HashBody<T> = Join<Version, Series<Bucket<T>>>
```

## Design Patterns

1. Use Join for pairing concepts
2. Use Series for sequences and collections
3. Implement immutable cores with mutable wrappers when needed
4. Utilize lazy evaluation through lambda expressions in Join and Series

## Refactoring Guidelines

1. Look for opportunities to replace standard library collections with Series
2. Convert complex generic types to typealiases for improved readability
3. Refactor mutable structures to immutable ones with versioning (like HashBody)
4. Use extension functions on typealiases to add functionality without modifying core structures

Certainly! Let's walk through the process of creating a 64-bit TwinTwinTwinByte using the nested Twin implementation. We'll break it down step by step.



Let's start with the definition of our nested Twin implementation:

```kotlin
inline infix fun <T> Twin<T>.j(other: Twin<T>): Twin<Twin<T>> = object : Twin<Twin<T>> {
    override val a: Twin<T> get() = this@j
    override val b: Twin<T> get() = other
}
```

Now, let's create a 64-bit TwinTwinTwinByte:

1. First, let's create four Twin<Byte> instances (each using 16 bits):

   ```kotlin
   val twin1: Twin<Byte> = 1.toByte() j 2.toByte()  // 16 bits
   val twin2: Twin<Byte> = 3.toByte() j 4.toByte()  // 16 bits
   val twin3: Twin<Byte> = 5.toByte() j 6.toByte()  // 16 bits
   val twin4: Twin<Byte> = 7.toByte() j 8.toByte()  // 16 bits
   ```

2. Now, let's combine twin1 and twin2 to create a Twin<Twin<Byte>>:

   ```kotlin
   val twinTwin1: Twin<Twin<Byte>> = twin1 j twin2  // 32 bits
   ```

   This uses our nested Twin implementation. `twinTwin1.a` is `twin1`, and `twinTwin1.b` is `twin2`.

3. Similarly, let's combine twin3 and twin4:

   ```kotlin
   val twinTwin2: Twin<Twin<Byte>> = twin3 j twin4  // 32 bits
   ```

4. Finally, we'll combine twinTwin1 and twinTwin2 to create our TwinTwinTwinByte:

   ```kotlin
   val twinTwinTwin: Twin<Twin<Twin<Byte>>> = twinTwin1 j twinTwin2  // 64 bits
   ```

   This again uses our nested Twin implementation. `twinTwinTwin.a` is `twinTwin1`, and `twinTwinTwin.b` is `twinTwin2`.

So, our final structure looks like this:

```
twinTwinTwin
|
+-- a: twinTwin1
|   |
|   +-- a: twin1 (1, 2)
|   |
|   +-- b: twin2 (3, 4)
|
+-- b: twinTwin2
    |
    +-- a: twin3 (5, 6)
    |
    +-- b: twin4 (7, 8)
```

This structure efficiently packs 8 bytes into 64 bits. Each original Twin<Byte> uses 16 bits, two of these are combined into a Twin<Twin<Byte>> using 32 bits, and finally, two Twin<Twin<Byte>> are combined into a Twin<Twin<Twin<Byte>>> using 64 bits.

The nested Twin implementation allows us to create this structure without allocating any additional storage beyond the original Twin<Byte> instances. Instead, it provides a way to access the nested structure through the `a` and `b` properties.

To access a specific byte, you would navigate through the structure. For example, to get the 5th byte:

```kotlin
val fifthByte: Byte = twinTwinTwin.b.a.a
```

This implementation allows us to efficiently pack multiple bytes into a single 64-bit structure while maintaining the ability to access individual bytes through the nested Twin structure.