package com.vsiwest.rangle

import Series
import get
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

enum class IOMemento(override val networkSize: Int? = null, val fromChars: (
        Series<Char>) -> Any) : TypeMemento {
    IoBoolean(1, {
        when (it[0]) {
            't' -> true
            'f' -> false
            else -> throw IllegalArgumentException("invalid boolean: $it")
        }
    }) {
        override fun createEncoder(i: Int): (Any?) -> ByteArray = writeBool
        override fun createDecoder(size: Int): (ByteArray) -> Any? = readBool
    },
    IoByte(1, {
        it.parseLong().toByte()
    }) {
        override fun createEncoder(i: Int): (Any?) -> ByteArray = writeByte
        override fun createDecoder(size: Int): (ByteArray) -> Any? = readByte
    },
    IoUByte(1, {
        it.parseLong().toUByte()
    }) {
        override fun createEncoder(i: Int): (Any?) -> ByteArray = writeUByte
        override fun createDecoder(size: Int): (ByteArray) -> Any? = readUByte
    },

    IoShort(2, { it.parseLong().toShort() }) {
        override fun createEncoder(i: Int): (Any?) -> ByteArray = PlatformCodec.Companion.currentPlatformCodec.writeShort as (Any?) -> ByteArray
        override fun createDecoder(size: Int): (ByteArray) -> Any? =
            PlatformCodec.Companion.currentPlatformCodec.readShort
    },

    IoInt(4, { it.parseLong().toInt() }) {
        override fun createEncoder(i: Int): (Any?) -> ByteArray = PlatformCodec.Companion.currentPlatformCodec.writeInt as (Any?) -> ByteArray
        override fun createDecoder(size: Int): (ByteArray) -> Any? =
            PlatformCodec.Companion.currentPlatformCodec.readInt
    },
    IoLong(8, Series<Char>::parseLong) {
        override fun createEncoder(i: Int): (Any?) -> ByteArray = PlatformCodec.Companion.currentPlatformCodec.writeLong as (Any?) -> ByteArray
        override fun createDecoder(size: Int): (ByteArray) -> Any? =
            PlatformCodec.Companion.currentPlatformCodec.readLong
    },
    IoUShort(2, { it.parseLong().toUShort() }) {
        override fun createEncoder(i: Int): (Any?) -> ByteArray =
            PlatformCodec.Companion.currentPlatformCodec.writeUShort as (Any?) -> ByteArray

        override fun createDecoder(size: Int): (ByteArray) -> Any? =
            PlatformCodec.Companion.currentPlatformCodec.readUShort
    },

    IoUInt(4, { it.parseLong().toUInt() }) {
        override fun createEncoder(i: Int): (Any?) -> ByteArray = PlatformCodec.Companion.currentPlatformCodec.writeUInt as (Any?) -> ByteArray
        override fun createDecoder(size: Int): (ByteArray) -> Any? =
            PlatformCodec.Companion.currentPlatformCodec.readUInt
    },
    IoULong(8, { it.parseLong().toULong() }) {
        override fun createEncoder(i: Int): (Any?) -> ByteArray = PlatformCodec.Companion.currentPlatformCodec.writeULong as (Any?) -> ByteArray
        override fun createDecoder(size: Int): (ByteArray) -> Any? =
            PlatformCodec.Companion.currentPlatformCodec.readULong
    },
    IoFloat(4, { it.parseDouble().toFloat() }) {
        override fun createEncoder(i: Int): (Any?) -> ByteArray = PlatformCodec.Companion.currentPlatformCodec.writeFloat as (Any?) -> ByteArray
        override fun createDecoder(size: Int): (ByteArray) -> Any? = PlatformCodec.Companion.currentPlatformCodec.readFloat
    },
    IoDouble(8, { it.parseDouble() }) {
        override fun createEncoder(i: Int): (Any?) -> ByteArray =
            PlatformCodec.Companion.currentPlatformCodec.writeDouble as (Any?) -> ByteArray

        override fun createDecoder(size: Int): (ByteArray) -> Any? = PlatformCodec.Companion.currentPlatformCodec.readDouble
    },
    IoLocalDate(8, { it.parseIsoDateTime() }) {
        override fun createEncoder(i: Int): (Any?) -> ByteArray = {
            //try a cast elvis first with Instant then with LocalDate
            val date = (it as? Instant)?.toLocalDateTime(TimeZone.UTC)?.date ?: it as LocalDate
//
//            val toEpochDays = (it as LocalDate).toEpochDays()
//            writeLong (toEpochDays.toLong())
            PlatformCodec.Companion.currentPlatformCodec.writeLong(date.toEpochDays().toLong())


        }

        override fun createDecoder(size: Int): (ByteArray) -> Any? = {
            val fromEpochDays =
                LocalDate.fromEpochDays(PlatformCodec.Companion.currentPlatformCodec.readLong(it).toInt())
            fromEpochDays
        }
    },

    /**
     * 12 bytes of storage, first epoch seconds Long , then nanos Int
     */
    IoInstant(12,
        { Instant.parse(it.toString()) }) {
        override fun createEncoder(i: Int): (Any?) -> ByteArray = { inst: Any? ->
            val instant = inst as Instant
            val epochSeconds = instant.epochSeconds
            val nanoAdjustment = instant.nanosecondsOfSecond
            PlatformCodec.Companion.currentPlatformCodec.writeLong(epochSeconds) + PlatformCodec.Companion.currentPlatformCodec.writeInt(
                nanoAdjustment
            )
        }

        override fun createDecoder(size: Int): (ByteArray) -> Any? = { bytes: ByteArray ->
            val epochSeconds = PlatformCodec.Companion.currentPlatformCodec.readLong(bytes)
            val nanoAdjustment = PlatformCodec.Companion.currentPlatformCodec.readInt(bytes.sliceArray(8..11))
            Instant.fromEpochSeconds(epochSeconds, nanoAdjustment)
        }
    },
    IoString(null, { it.asString() }) {
        override fun createEncoder(i: Int): (Any?) -> ByteArray = writeString

        override fun createDecoder(size: Int): (ByteArray) -> Any? = readString
    },
    IoCharSeries(null, ::CharSeries) {
        override fun createEncoder(i: Int): (Any?) -> ByteArray = writeCharSeries
        override fun createDecoder(size: Int): (ByteArray) -> Any? = readCharSeries
    },

    IoByteArray(null, { it.encodeToByteArray() }) {
        override fun createEncoder(i: Int): (Any?) -> ByteArray = writeByteArray
        override fun createDecoder(size: Int): (ByteArray) -> Any? = readByteArray
    },
    IoNothing(null, { "" }) {
        override fun createEncoder(i: Int): (Any?) -> ByteArray = { ByteArray(0) }
        override fun createDecoder(size: Int): (ByteArray) -> Any? = { ByteArray(0) }
    }
    ;

    abstract fun createEncoder(i: Int): (Any?) -> ByteArray
    abstract fun createDecoder(size: Int): (ByteArray) -> Any?

    companion object {
        val readCharSeries: (ByteArray) -> Series<Char> = { value: ByteArray -> value.decodeToChars() }
        val writeCharSeries: (Any?) -> ByteArray = { value: Any? -> (value as Series<Char>).encodeToByteArray() }
        val readByteSeries: (ByteArray) -> Series<Byte> = { value: ByteArray -> value.toSeries() }
        val writeByteSeries: (Any?) -> ByteArray = { value: Any? -> (value as Series<Byte>).toArray() }
        val readByteArray: (ByteArray) -> ByteArray = { value: ByteArray -> value }
        val writeByteArray: (Any?) -> ByteArray = { value: Any? -> value as ByteArray }

        val readString: (ByteArray) -> String = { value: ByteArray -> value.decodeToString() }
        val writeString: (Any?) -> ByteArray = { value: Any? -> (value as String).encodeToByteArray() }

        val readBool: (ByteArray) -> Boolean = { value: ByteArray -> value[0] == 1.toByte() }
        val writeBool: (Any?) -> ByteArray =
            { value: Any? -> ByteArray(1).apply { this[0] = if (value as Boolean) 1 else 0 } }
        val readByte: (ByteArray) -> Byte = { value: ByteArray -> value[0] }
        val writeByte: (Any?) -> ByteArray = { value: Any? -> ByteArray(1).apply { this[0] = value as Byte } }
        val readUByte: (ByteArray) -> UByte = { value: ByteArray -> value[0].toUByte() }
        val writeUByte: (Any?) -> ByteArray =
            { value: Any? -> ByteArray(1).apply { this[0] = (value as Byte) } }//take it on faith here
    }
}