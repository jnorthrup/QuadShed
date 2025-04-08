package com.vsiwest.rangle.packing

import com.vsiwest.rangle.IOMemento
import com.vsiwest.rangle.Series
import com.vsiwest.rangle.get
import com.vsiwest.rangle.size

/**
 * Describes how a Series was packed into register(s).
 * Includes necessary info for unpacking.
 */
sealed class PackingInfo(val memento: IOMemento, val count: Int) {
    // Cases that fit within a single 64-bit register
    data class SingleLong(val m: IOMemento = IOMemento.IoLong) : PackingInfo(m, 1)
    data class SingleDouble(val m: IOMemento = IOMemento.IoDouble) : PackingInfo(m, 1)
    data class SingleFloat(val m: IOMemento = IOMemento.IoFloat) : PackingInfo(m, 1) // Packed in lower 32 bits
    data class SingleInt(val m: IOMemento = IOMemento.IoInt) : PackingInfo(m, 1) // Packed in lower 32 bits
    // ... (add SingleShort, SingleByte if needed, packing into lowest bits)

    data class TwoFloats(val m: IOMemento = IOMemento.IoFloat) : PackingInfo(m, 2)
    data class TwoInts(val m: IOMemento = IOMemento.IoInt) : PackingInfo(m, 2)
    data class FourShorts(val m: IOMemento = IOMemento.IoShort) : PackingInfo(m, 4)
    data class EightBytes(val m: IOMemento = IOMemento.IoByte) : PackingInfo(m, 8)
    // ... (add other common 64-bit layouts: e.g., FourUShorts, EightUBytes)

    // Cases requiring two 64-bit registers (128 bits total)
    data class TwoLongs128(val m: IOMemento = IOMemento.IoLong) : PackingInfo(m, 2)
    data class TwoDoubles128(val m: IOMemento = IOMemento.IoDouble) : PackingInfo(m, 2)
    data class FourFloats128(val m: IOMemento = IOMemento.IoFloat) : PackingInfo(m, 4)
    data class FourInts128(val m: IOMemento = IOMemento.IoInt) : PackingInfo(m, 4)
    data class EightShorts128(val m: IOMemento = IOMemento.IoShort) : PackingInfo(m, 8)
    data class SixteenBytes128(val m: IOMemento = IOMemento.IoByte) : PackingInfo(m, 16)
     // ... (add other common 128-bit layouts)

    // Fallback: Indicates the series couldn't be packed using predefined layouts.
    data class Unpackable(val m: IOMemento, val c: Int) : PackingInfo(m, c)
}

/**
 * Represents a Series packed into one or two 64-bit registers.
 *
 * @param register1 The primary (or only) 64-bit register holding packed data.
 * @param register2 Optional second 64-bit register for layouts requiring > 64 bits.
 * @param packingInfo Metadata describing how the data was packed, essential for unpacking.
 */
data class PackedRegisters(
    val register1: Long,
    val register2: Long?, // Null if only 64 bits were needed
    val packingInfo: PackingInfo
) {
    /**
     * Attempts to unpack the data back into a List.
     * Returns null if packingInfo is Unpackable.
     * Note: Returning a Series requires reconstructing the lambda, which is tricky.
     *       Returning a List is more straightforward for unpacking.
     */
    fun <V> unpack(): List<V>? {
        // Implementation will use packingInfo to determine how to extract
        // values from register1 and register2.
        @Suppress("UNCHECKED_CAST")
        return when (val info = packingInfo) {
            is PackingInfo.Unpackable -> null // Cannot unpack this representation

            // --- Single Register Cases (64-bit) ---
            is PackingInfo.SingleLong -> listOf(register1 as V)
            is PackingInfo.SingleDouble -> listOf(Double.fromBits(register1) as V)
            is PackingInfo.SingleFloat -> listOf(Float.fromBits(register1.toInt()) as V) // Lower 32 bits
            is PackingInfo.SingleInt -> listOf(register1.toInt() as V) // Lower 32 bits
            // Add SingleShort, SingleByte extraction

            is PackingInfo.TwoFloats -> listOf(
                Float.fromBits((register1 ushr 32).toInt()), // High 32
                Float.fromBits(register1.toInt())            // Low 32
            ) as List<V>
            is PackingInfo.TwoInts -> listOf(
                (register1 ushr 32).toInt(),
                register1.toInt()
            ) as List<V>
            is PackingInfo.FourShorts -> List(4) { i ->
                (register1 ushr (16 * (3-i)) and 0xFFFFL).toShort()
            } as List<V>
             is PackingInfo.EightBytes -> List(8) { i ->
                (register1 ushr (8 * (7-i)) and 0xFFL).toByte()
            } as List<V>
            // Add other 64-bit unpackers

            // --- Dual Register Cases (128-bit) ---
            is PackingInfo.TwoLongs128 -> listOf(register1, register2!!) as List<V> // R1 = Item 0, R2 = Item 1
            is PackingInfo.TwoDoubles128 -> listOf(
                Double.fromBits(register1),
                Double.fromBits(register2!!)
            ) as List<V>
            is PackingInfo.FourFloats128 -> listOf( // F0, F1 in R1; F2, F3 in R2
                Float.fromBits((register1 ushr 32).toInt()),
                Float.fromBits(register1.toInt()),
                Float.fromBits((register2!! ushr 32).toInt()),
                Float.fromBits(register2.toInt())
            ) as List<V>
            is PackingInfo.FourInts128 -> listOf( // I0, I1 in R1; I2, I3 in R2
                (register1 ushr 32).toInt(),
                register1.toInt(),
                (register2!! ushr 32).toInt(),
                register2.toInt()
            ) as List<V>
            is PackingInfo.EightShorts128 -> List(8) { i ->
                 val reg = if (i < 4) register1 else register2!!
                 val shift = 16 * (3 - (i % 4))
                 (reg ushr shift and 0xFFFFL).toShort()
            } as List<V>
            is PackingInfo.SixteenBytes128 -> List(16) { i ->
                 val reg = if (i < 8) register1 else register2!!
                 val shift = 8 * (7 - (i % 8))
                 (reg ushr shift and 0xFFL).toByte()
            } as List<V>
            // Add other 128-bit unpackers

            // Should not happen if sealed class is exhaustive
            //else -> null
        }
    }
}

/**
 * Attempts to pack a Series into 1 or 2 64-bit registers based on its
 * element type (IOMemento) and size.
 *
 * @param series The Series to pack. Requires element type V to have a mapping in IOMemento.
 * @param memento The IOMemento corresponding to the element type V. Crucial for determining packing strategy.
 * @return PackedRegisters containing the register value(s) and PackingInfo,
 *         or PackedRegisters with PackingInfo.Unpackable if no suitable layout is found.
 */
fun <V : Comparable<V>> packToRegisters(
    series: Series<V>,
    memento: IOMemento // Explicitly require the type information
): PackedRegisters {
    val count = series.size

    // Handle single element case (the "array of default length" convention)
    if (count == 1) {
        val value = series[0]
        return when (memento) {
            IOMemento.IoLong -> PackedRegisters(value as Long, null, PackingInfo.SingleLong())
            IOMemento.IoDouble -> PackedRegisters(Double.toBits(value as Double), null, PackingInfo.SingleDouble())
            IOMemento.IoInt -> PackedRegisters((value as Int).toLong() and 0xFFFFFFFFL, null, PackingInfo.SingleInt())
            IOMemento.IoFloat -> PackedRegisters(Float.toBits(value as Float).toLong() and 0xFFFFFFFFL, null, PackingInfo.SingleFloat())
            // Add Short, Byte, UInt, ULong etc. packing into lower bits of register1 if needed
            else -> PackedRegisters(0L, null, PackingInfo.Unpackable(memento, count)) // Can't pack single non-primitive easily
        }
    }

    // Handle multi-element cases based on type and count
    return when (memento to count) {
        // --- 64-bit Layouts ---
        IOMemento.IoFloat to 2 -> {
            val r1 = (Float.toBits(series[0] as Float).toLong() shl 32) or
                     (Float.toBits(series[1] as Float).toLong() and 0xFFFFFFFFL)
            PackedRegisters(r1, null, PackingInfo.TwoFloats())
        }
        IOMemento.IoInt to 2 -> {
             val r1 = ((series[0] as Int).toLong() shl 32) or
                      ((series[1] as Int).toLong() and 0xFFFFFFFFL)
             PackedRegisters(r1, null, PackingInfo.TwoInts())
        }
        IOMemento.IoShort to 4 -> {
             var r1 = 0L
             for(i in 0..3) {
                 r1 = r1 or ((series[i] as Short).toLong() and 0xFFFFL shl (16 * (3-i)))
             }
             PackedRegisters(r1, null, PackingInfo.FourShorts())
        }
         IOMemento.IoByte to 8 -> {
             var r1 = 0L
             for(i in 0..7) {
                 r1 = r1 or ((series[i] as Byte).toLong() and 0xFFL shl (8 * (7-i)))
             }
             PackedRegisters(r1, null, PackingInfo.EightBytes())
         }
        // Add UShort, UByte layouts

        // --- 128-bit Layouts ---
        IOMemento.IoLong to 2 -> PackedRegisters(series[0] as Long, series[1] as Long, PackingInfo.TwoLongs128())
        IOMemento.IoDouble to 2 -> PackedRegisters(
            Double.toBits(series[0] as Double),
            Double.toBits(series[1] as Double),
            PackingInfo.TwoDoubles128()
        )
        IOMemento.IoFloat to 4 -> {
            val r1 = (Float.toBits(series[0] as Float).toLong() shl 32) or
                     (Float.toBits(series[1] as Float).toLong() and 0xFFFFFFFFL)
            val r2 = (Float.toBits(series[2] as Float).toLong() shl 32) or
                     (Float.toBits(series[3] as Float).toLong() and 0xFFFFFFFFL)
            PackedRegisters(r1, r2, PackingInfo.FourFloats128())
        }
         IOMemento.IoInt to 4 -> {
             val r1 = ((series[0] as Int).toLong() shl 32) or
                      ((series[1] as Int).toLong() and 0xFFFFFFFFL)
             val r2 = ((series[2] as Int).toLong() shl 32) or
                      ((series[3] as Int).toLong() and 0xFFFFFFFFL)
             PackedRegisters(r1, r2, PackingInfo.FourInts128())
         }
        IOMemento.IoShort to 8 -> {
             var r1 = 0L
             var r2 = 0L
             for(i in 0..7) {
                 val value = (series[i] as Short).toLong() and 0xFFFFL
                 if (i < 4) {
                     r1 = r1 or (value shl (16 * (3-i)))
                 } else {
                     r2 = r2 or (value shl (16 * (3-(i%4))))
                 }
             }
             PackedRegisters(r1, r2, PackingInfo.EightShorts128())
         }
          IOMemento.IoByte to 16 -> {
             var r1 = 0L
             var r2 = 0L
             for(i in 0..15) {
                 val value = (series[i] as Byte).toLong() and 0xFFL
                 if (i < 8) {
                     r1 = r1 or (value shl (8 * (7-i)))
                 } else {
                     r2 = r2 or (value shl (8 * (7-(i%8))))
                 }
             }
             PackedRegisters(r1, r2, PackingInfo.SixteenBytes128())
         }
        // Add UInt, ULong etc. 128-bit layouts

        // --- Fallback ---
        else -> PackedRegisters(0L, null, PackingInfo.Unpackable(memento, count))
    }
}