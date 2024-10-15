@file:Suppress("UNCHECKED_CAST")

package com.vsiwest.rangle

import Join
import j
import kotlin.experimental.or
import kotlin.jvm.JvmStatic


/** RecordMeta is a data class that describes a column of an Isam record
 *
 * @param name the name of the column
 * @param type the type of the column
 * @param begin the byte offset of the beginning of the column
 * @param end the byte offset of the end of the column
 * @param decoder a lambda that converts a byte[]  to downstream, often but not necessarily the IoMemento utility
 * @param encoder a lambda that produces a byte[] for marshalling to disk or elsewhere
 * @param child a child RecordMeta for a child record, for instance, CSV conversion to ISAM might define two RecordMetas for two steps
 */

class RecordMeta(
//    /** column name*/
    val name: String,
    /** enum-resident Type describing byte marshalling strategies - a specialization of TypeMemento */
    val type: IOMemento,
    /** context-specific byte offset beginning*/
    val begin: Int = -1,
    /** context-specific byte offset ending*/
    val end: Int = -1,
    /** a lambda that converts a byte[]  to downstream, often but not necessarily the IoMemento utility */
    val decoder: (ByteArray) -> Any? = type.createDecoder(end - begin),
    /** a lambda that produces a byte[] for marshalling to disk or elsewhere */
    val encoder: (Any?) -> ByteArray = type.createEncoder(end - begin),
    /** open to interpretation, for instance, CSV conversion to ISAM might define two RecordMetas for two steps*/
    var child: RecordMeta? = null,
) : ColumnMeta by (name j (type as TypeMemento)){
    override fun toString(): String = "RecordMeta(name='$name', type=$type, begin=$begin, end=$end, decoder=$decoder, encoder=$encoder, child=$child)"
}

typealias ColumnMeta = Join<String, TypeMemento>

//mix-in for name
val ColumnMeta.name: String get() = this.a

//mix-in for type
val ColumnMeta.type: TypeMemento get() = this.b

typealias RowVec = Series2<Any?, () -> ColumnMeta>
//val RowVec.left get() =  this α Join<*, () -> RecordMeta>::a

/** Cursors are a columnar abstraction composed of Series of Joined value+meta pairs (RecordMeta) */
typealias Cursor = Series<RowVec>

object WireProto{
    fun writeToBuffer(
        rowVec:RowVec,
        rowBuf: ByteArray,
        meta: Series<RecordMeta>,
    ): ByteArray {
        val rowData = rowVec.left

        for (x in 0 until  meta.size) {
            val colMeta: RecordMeta = meta[x]
            val colData  = rowData[x]

            val pos=(colMeta.begin)

            // val debugMe = colMeta::encoder
            val colBytes = colMeta.encoder(colData)

            colBytes.copyInto(rowBuf, pos, 0, colBytes.size)


            if (meta[x].type.networkSize == null && pos + colBytes.size < meta[x].end)
                rowBuf[pos + colBytes.size] = 0
        }
        return   rowBuf
    }
}
interface TypeMemento {
    val networkSize: Int?
}

interface PlatformCodec {
    val readLong: (ByteArray) -> Long
    val readInt: (ByteArray) -> Int

    val readShort: (ByteArray) -> Short
    val writeLong: (Long) -> ByteArray
    val writeInt: (Int) -> ByteArray
    val writeShort: (Short) -> ByteArray
    val writeDouble: (Double) -> ByteArray get() = { writeLong(it.toBits()) }
    val writeFloat: (Float) -> ByteArray get() = { writeInt(it.toBits()) }
    val readDouble: (ByteArray) -> Double get() = { Double.fromBits(readLong(it)) }
    val readFloat: (ByteArray) -> Float get() = { Float.fromBits(readInt(it)) }
    val readUShort: (ByteArray) -> UShort
    val readUInt: (ByteArray) -> UInt
    val readULong: (ByteArray) -> ULong
    val writeUShort: (UShort) -> ByteArray
    val writeUInt: (UInt) -> ByteArray
    val writeULong: (ULong) -> ByteArray

    companion object {
        @JvmStatic
        val isNetworkEndian: Boolean by lazy {
            val i = 0x01020304
            val b = i.toByte()
            b == 0x01.toByte()

        }

        @JvmStatic
        val isLittleEndian: Boolean get() = !isNetworkEndian

        object currentPlatformCodec : PlatformCodec {
            override val readShort: (ByteArray) -> Short by lazy {
                if (isLittleEndian) {
                    { ((it[1].toInt() and 0xFF) shl 8).toShort() or (it[0].toInt() and 0xFF).toShort() }
                } else {
                    { (it[0].toInt() and 0xFF shl 8 or (it[1].toInt() and 0xFF)).toShort() }
                }
            }
            override val readInt: (ByteArray) -> Int by lazy {
                if (isLittleEndian) {
                    {
                        (it[3].toUByte().toUInt() shl 24 or
                                (it[2].toUByte().toUInt() shl 16) or
                                (it[1].toUByte().toUInt() shl 8) or
                                (it[0].toUByte()).toUInt()).toInt()

                    }
                } else {
                    {
                        (it[0].toUByte().toUInt() shl 24 or
                                (it[1].toUByte().toUInt() shl 16) or
                                (it[2].toUByte().toUInt() shl 8) or
                                (it[3].toUByte()).toUInt()).toInt()
                    }
                }

            }
            override val readLong: (ByteArray) -> Long by lazy {

                if (isLittleEndian) {
                    {
                        (it[7].toUByte().toULong() shl 56 or
                                (it[6].toUByte().toULong() shl 48) or
                                (it[5].toUByte().toULong() shl 40) or
                                (it[4].toUByte().toULong() shl 32) or
                                (it[3].toUByte().toUInt() shl 24 or
                                        (it[2].toUByte().toUInt() shl 16) or
                                        (it[1].toUByte().toUInt() shl 8) or
                                        (it[0].toUByte()).toUInt()).toULong()).toLong()
                    }
                } else {
                    {
                        (it[0].toUByte().toULong() shl 56 or
                                (it[1].toUByte().toULong() shl 48) or
                                (it[2].toUByte().toULong() shl 40) or
                                (it[3].toUByte().toULong() shl 32) or
                                (it[4].toUByte().toUInt() shl 24 or
                                        (it[5].toUByte().toUInt() shl 16) or
                                        (it[6].toUByte().toUInt() shl 8) or
                                        (it[7].toUByte()).toUInt()).toULong()).toLong()

                    }
                }
            }
            override val writeShort: (Short) -> ByteArray by lazy {

                if (isLittleEndian) {
                    {
                        byteArrayOf(
                            (it.toUByte()).toByte(),
                            ((it.toUInt() shr 8).toUByte()).toByte()
                        )
                    }
                } else {
                    {
                        byteArrayOf(
                            ((it.toUInt() shr 8).toUByte()).toByte(),
                            (it.toUByte()).toByte()
                        )
                    }
                }
            }
            override val writeInt: (Int) -> ByteArray by lazy {
                if (isLittleEndian) {
                    {
                        byteArrayOf(
                            (it.toUByte()).toByte(),
                            ((it shr 8).toUByte()).toByte(),
                            ((it shr 16).toUByte()).toByte(),
                            ((it shr 24).toUByte()).toByte()
                        )
                    }
                } else {
                    {
                        byteArrayOf(
                            ((it shr 24).toUByte()).toByte(),
                            ((it shr 16).toUByte()).toByte(),
                            ((it shr 8).toUByte()).toByte(),
                            (it.toUByte()).toByte()
                        )
                    }
                }
            }
            override val writeLong: (Long) -> ByteArray by lazy {
                if (isLittleEndian) {
                    {
                        byteArrayOf(
                            (it.toUByte()).toByte(),
                            ((it shr 8).toUByte()).toByte(),
                            ((it shr 16).toUByte()).toByte(),
                            ((it shr 24).toUByte()).toByte(),
                            ((it shr 32).toUByte()).toByte(),
                            ((it shr 40).toUByte()).toByte(),
                            ((it shr 48).toUByte()).toByte(),
                            ((it shr 56).toUByte()).toByte()
                        )
                    }
                } else {
                    {
                        byteArrayOf(
                            ((it shr 56).toUByte()).toByte(),
                            ((it shr 48).toUByte()).toByte(),
                            ((it shr 40).toUByte()).toByte(),
                            ((it shr 32).toUByte()).toByte(),
                            ((it shr 24).toUByte()).toByte(),
                            ((it shr 16).toUByte()).toByte(),
                            ((it shr 8).toUByte()).toByte(),
                            (it.toUByte()).toByte()
                        )
                    }
                }
            }
            //6 kotlin unsigned adapters below for the above 6
            override val readUShort: (ByteArray) -> UShort ={it-> readShort(it).toUShort()}
            override val readUInt: (ByteArray) -> UInt ={it-> readInt(it).toUInt()}
            override val readULong: (ByteArray) -> ULong ={it-> readLong(it).toULong()}
            override val writeUShort: (UShort) -> ByteArray ={it-> writeShort(it.toShort()) }
            override val writeUInt: (UInt) -> ByteArray ={it-> writeInt(it.toInt()) }
            override val writeULong: (ULong) -> ByteArray ={it-> writeLong(it.toLong()) }

        }
    }

}

