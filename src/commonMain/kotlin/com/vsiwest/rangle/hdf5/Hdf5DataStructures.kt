package com.vsiwest.rangle.hdf5

import com.vsiwest.rangle.Series

// Simplified Superblock Info (Version 0)
internal data class Hdf5SuperblockInfo(
    val baseAddress: Long, // Base Address field
    val rootGroupSymbolTableAddress: Long, // Addr of Root Group Symbol Table Entry
    val offsetsSize: Int, // Size of offsets in the file
    val lengthsSize: Int // Size of lengths in the file
)

// Simplified Object Header Info
internal data class Hdf5ObjectHeaderInfo(
    val headerVersion: Int,
    val totalHeaderMessages: Int,
    val messages: Series<Hdf5HeaderMessage> // Lazy series of messages
)

// Simplified Header Message Info (only handling a few types)
internal data class Hdf5HeaderMessage(
    val messageType: Int, // e.g., 0x0001 (Dataspace), 0x0003 (Datatype), 0x0008 (DataLayout)
    val messageSize: Int,
    val messageFlags: Byte,
    val messageDataOffset: Long // Offset within the file to the message data
)

// Simplified Dataspace Info (only dimensionality)
internal data class Hdf5DataspaceInfo(
    val dimensionality: Int,
    val dimensions: Series<Long> // Series of dimension sizes
)

// Simplified Datatype Info (basic types)
internal data class Hdf5DatatypeInfo(
    val typeClass: Int, // e.g., 0 (Fixed-Point), 1 (Floating-Point)
    val sizeBytes: Int, // Size of the datatype in bytes
    val isSigned: Boolean, // For fixed-point
    val isLittleEndian: Boolean // HDF5 stores endianness
)

// Simplified Data Layout Info
internal data class Hdf5DataLayoutInfo(
    val layoutClass: Int, // 0 (Compact), 1 (Contiguous), 2 (Chunked)
    val dataAddress: Long?, // Address if Contiguous
    val dataSize: Long? // Size if Contiguous or Compact
)

// Simplified Symbol Table Entry (for finding objects in Root Group)
internal data class Hdf5SymbolTableEntry(
    val linkNameOffset: Long, // Offset to the object name
    val objectHeaderAddress: Long // Offset to the object's header
)

// Represents a located dataset ready for reading
internal data class LocatedDatasetInfo(
    val name: String,
    val dataspace: Hdf5DataspaceInfo,
    val datatype: Hdf5DatatypeInfo,
    val layout: Hdf5DataLayoutInfo
)
