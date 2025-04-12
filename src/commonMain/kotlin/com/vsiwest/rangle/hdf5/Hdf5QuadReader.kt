package com.vsiwest.rangle.hdf5

import com.vsiwest.rangle.*
import com.vsiwest.rangle.io.FileBuffer
import com.vsiwest.rangle.sqlite.ColumnMeta
import com.vsiwest.rangle.sqlite.IOMemento

/**
 * A basic HDF5 file reader for QuadShed, focusing on contiguous, numeric datasets.
 * Does NOT support chunking, compression, filters, complex types, or full B-Tree navigation.
 */
class Hdf5QuadReader(private val fileBuffer: FileBuffer) : Usable {

    // Ensure FileBuffer is usable (already conforms)
    override fun open() = fileBuffer.open()
    override fun close() = fileBuffer.close()

    private val fileBytes: Series<Byte> = fileBuffer // Use the Series view

    // Lazy parsing of the superblock
    private val superblock: Hdf5SuperblockInfo by lazy {
        parseSuperblock(fileBytes)
    }

    // Mapping from HDF5 type info to QuadShed IOMemento
    private fun mapDatatype(dt: Hdf5DatatypeInfo): IOMemento {
        // Basic mapping, needs refinement for endianness and signedness
        return when (dt.typeClass) {
            0 -> when (dt.sizeBytes) { // Fixed-Point (Integers)
                1 -> IOMemento.IoInt // Treat Byte/Short as Int for simplicity
                2 -> IOMemento.IoInt
                4 -> IOMemento.IoInt
                8 -> IOMemento.IoLong
                else -> throw UnsupportedOperationException("Unsupported integer size: ${dt.sizeBytes}")
            }
            1 -> when (dt.sizeBytes) { // Floating-Point
                4 -> IOMemento.IoFloat // Assumed Float32
                8 -> IOMemento.IoDouble // Assumed Float64
                else -> throw UnsupportedOperationException("Unsupported float size: ${dt.sizeBytes}")
            }
            // Add String/Blob later if needed and simple cases exist
            else -> throw UnsupportedOperationException("Unsupported HDF5 datatype class: ${dt.typeClass}")
        }
    }

    /**
     * Reads a specific dataset as a QuadShed Cursor.
     * Currently only supports 1D or 2D contiguous, numeric datasets found via the root group.
     *
     * @param datasetPath Path within the HDF5 file (e.g., "/my_data"). Currently only supports root-level datasets.
     * @return A QuadShed Cursor representing the dataset.
     */
    fun readDatasetAsCursor(datasetPath: String): Cursor {
        require(datasetPath.startsWith("/") && !datasetPath.contains('/', 1)) {
            "Simplified reader only supports root-level datasets (e.g., '/dataset_name'). Path provided: $datasetPath"
        }
        val datasetName = datasetPath.substring(1)

        // 1. Find the dataset object header address (simplified)
        val locatedInfo = findDataset(datasetName)
            ?: throw NoSuchElementException("Dataset '$datasetName' not found in root group symbol table.")

        // 2. Validate Layout and Datatype
        require(locatedInfo.layout.layoutClass == 1) { // 1 == Contiguous
            "Dataset '$datasetName' is not stored contiguously. Layout class: ${locatedInfo.layout.layoutClass}. Chunked/Compact reading not implemented."
        }
        require(locatedInfo.layout.dataAddress != null && locatedInfo.layout.dataSize != null) {
            "Contiguous dataset '$datasetName' missing data address or size."
        }

        val qdShedType = try {
            mapDatatype(locatedInfo.datatype)
        } catch (e: UnsupportedOperationException) {
            throw UnsupportedOperationException("Dataset '$datasetName' uses an unsupported HDF5 datatype. ${e.message}")
        }

        // 3. Validate Dataspace (Dimensions)
        require(locatedInfo.dataspace.dimensionality in 1..2) {
            "Dataset '$datasetName' has unsupported dimensionality: ${locatedInfo.dataspace.dimensionality}. Only 1D or 2D supported."
        }

        // 4. Prepare Column Metadata for Cursor
        val columnMetas: Series<ColumnMeta> = if (locatedInfo.dataspace.dimensionality == 1) {
            1 j { _ -> ColumnMeta(datasetName, qdShedType) } // Single column named after dataset
        } else {
            val numCols = locatedInfo.dataspace.dimensions[1].toInt() // Second dimension is columns
            numCols j { colIdx -> ColumnMeta("col_$colIdx", qdShedType) }
        }

        // 5. Read Raw Data Bytes
        val dataAddress = locatedInfo.layout.dataAddress!! + superblock.baseAddress // Absolute address
        val dataSize = locatedInfo.layout.dataSize!!
        val rawDataBytes = fileBytes.slice(dataAddress, dataAddress + dataSize)

        // 6. Create the Cursor (Lazy Access)
        val numRows = if (locatedInfo.dataspace.dimensionality == 1) locatedInfo.dataspace.dimensions[0].toInt() else locatedInfo.dataspace.dimensions[0].toInt()
        val numCols = columnMetas.size
        val bytesPerElement = locatedInfo.datatype.sizeBytes
        val rowStride = if (locatedInfo.dataspace.dimensionality == 1) bytesPerElement else numCols * bytesPerElement

        return numRows j { rowIndex ->
            val rowOffset = rowIndex * rowStride
            numCols j { colIndex ->
                val elementOffset = rowOffset + colIndex * bytesPerElement
                val elementBytes = rawDataBytes.slice(elementOffset.toLong(), (elementOffset + bytesPerElement).toLong())

                // Decode the element based on type
                val value = decodeElement(elementBytes.toArray(), locatedInfo.datatype)

                value j { columnMetas[colIndex] } // Pair value with its metadata getter
            }
        }
    }

    // --- Internal Parsing Helpers ---

    private fun parseSuperblock(bytes: Series<Byte>): Hdf5SuperblockInfo {
        // Minimal parsing for version 0 superblock
        require(bytes.size >= 100) { "File too small for HDF5 Superblock" }
        val magic = ByteArray(8) { bytes[it] }
        // First 4 bytes are 89 48 44 0D (\211 H D \r), next 4 are 0A 1A 0A 00 (\n \032 \n \000)
        require(magic[0] == 0x89.toByte() && magic[1] == 0x48.toByte() &&
                magic[2] == 0x44.toByte() && magic[3] == 0x0D.toByte() &&
                magic[4] == 0x0A.toByte() && magic[5] == 0x1A.toByte() &&
                magic[6] == 0x0A.toByte() && magic[7] == 0x00.toByte()) {
            "Invalid HDF5 signature."
        }

        val superblockVersion = bytes[8].toInt()
        require(superblockVersion == 0) { "Only HDF5 Superblock version 0 is supported" }

        val offsetsSize = bytes[13].toInt()
        val lengthsSize = bytes[14].toInt()
        require(offsetsSize == 8 && lengthsSize == 8) { "Simplified reader requires 8-byte offsets and lengths" }

        val baseAddress = bytes.readLong(24L, PlatformCodec.currentPlatformCodec.isLittleEndian) 
        val rootGroupSymbolTableAddress = bytes.readLong(40L, PlatformCodec.currentPlatformCodec.isLittleEndian)

        return Hdf5SuperblockInfo(
            baseAddress = baseAddress,
            rootGroupSymbolTableAddress = rootGroupSymbolTableAddress,
            offsetsSize = offsetsSize,
            lengthsSize = lengthsSize
        )
    }

    // Simplified: Find dataset only in root group via symbol table entry
    private fun findDataset(name: String): LocatedDatasetInfo? {
        val rootSymTableEntryAddr = superblock.rootGroupSymbolTableAddress + superblock.baseAddress
        
        try {
            val firstEntry = parseSymbolTableEntry(rootSymTableEntryAddr) // HACK
            val entryName = readObjectName(firstEntry.linkNameOffset)     // HACK
            if (entryName == name) {
                 // Now parse the object header found
                 val objHeaderInfo = parseObjectHeader(firstEntry.objectHeaderAddress)
                 // Extract required messages
                 val dataspace = parseDataspaceMessage(findMessage(objHeaderInfo, 1)!!) // 1 = Dataspace
                 val datatype = parseDatatypeMessage(findMessage(objHeaderInfo, 3)!!)   // 3 = Datatype
                 val layout = parseDataLayoutMessage(findMessage(objHeaderInfo, 8)!!) // 8 = Data Layout
                 return LocatedDatasetInfo(name, dataspace, datatype, layout)
            }
        } catch (e: Exception) {
             println("Warning: Simplified HDF5 dataset lookup failed. Needs full B-tree/Symbol Table parsing. ${e.message}")
        }

        // Proper implementation requires parsing the symbol table structure (likely B-Tree v1)
        // and the local/global heaps for object names.
        println("Warning: Simplified dataset lookup failed for '$name'. Returning null.")
        return null // Indicate not found with this simplified method
    }

    // HACK: Read only the first symbol table entry directly
    private fun parseSymbolTableEntry(address: Long): Hdf5SymbolTableEntry {
         require(address >= 0 && address + 32 <= fileBytes.size) { "Symbol table entry address out of bounds"} // Approx size
         val linkNameOffset = fileBytes.readLong(address + 0, PlatformCodec.currentPlatformCodec.isLittleEndian)
         val objectHeaderAddress = fileBytes.readLong(address + 8, PlatformCodec.currentPlatformCodec.isLittleEndian)
         // Skipping cache type and reserved
         // Skipping Scratch pad union
         return Hdf5SymbolTableEntry(linkNameOffset, objectHeaderAddress)
    }

    // HACK: Read name from Global Heap (Highly simplified, assumes direct offset)
    private fun readObjectName(heapOffset: Long): String {
        // Proper implementation requires parsing the Global Heap structure.
        // This is a placeholder assuming a null-terminated string at the offset.
        val buffer = mutableListOf<Byte>()
        var currentOffset = heapOffset + superblock.baseAddress
        try {
            while (currentOffset < fileBytes.size) {
                val byte = fileBytes[currentOffset.toInt()]
                if (byte == 0.toByte()) break
                buffer.add(byte)
                currentOffset++
            }
            return buffer.toByteArray().decodeToString()
        } catch (e: IndexOutOfBoundsException) {
            println("Warning: Simplified object name reading failed.")
            return "unknown_object"
        }
    }

    private fun parseObjectHeader(address: Long): Hdf5ObjectHeaderInfo {
        val absoluteAddress = address + superblock.baseAddress
        require(absoluteAddress >= 0 && absoluteAddress + 8 <= fileBytes.size) { "Object header address out of bounds"}

        val version = fileBytes[absoluteAddress.toInt()].toInt()
        // Version 1 header parsing (simplified)
        require(version == 1) { "Only Object Header version 1 supported" }
        val numMessages = fileBytes.readUInt16(absoluteAddress + 2, PlatformCodec.currentPlatformCodec.isLittleEndian)
        // Skipping ref count and header size for now

        var currentMessageOffset = absoluteAddress + 8 // Start after fixed part of v1 header

        val messages = numMessages j { _ ->
            require(currentMessageOffset + 8 <= fileBytes.size) { "Object header message pointer out of bounds"}
            val msgType = fileBytes.readUInt16(currentMessageOffset + 0, PlatformCodec.currentPlatformCodec.isLittleEndian)
            val msgSize = fileBytes.readUInt16(currentMessageOffset + 2, PlatformCodec.currentPlatformCodec.isLittleEndian)
            val msgFlags = fileBytes[currentMessageOffset + 4]
            // Skipping reserved bytes
            val msgDataOffset = currentMessageOffset + 8 // Data follows header fixed part
            currentMessageOffset += (8 + msgSize) // Move to the next message header start
            // Align to 8 bytes
            currentMessageOffset = (currentMessageOffset + 7) and -8L

            Hdf5HeaderMessage(msgType, msgSize, msgFlags, msgDataOffset)
        }

        return Hdf5ObjectHeaderInfo(version, numMessages, messages)
    }

    private fun findMessage(headerInfo: Hdf5ObjectHeaderInfo, type: Int): Hdf5HeaderMessage? {
       for (i in 0 until headerInfo.messages.size) {
           val msg = headerInfo.messages[i]
           if (msg.messageType == type) return msg
       }
       return null
    }

    private fun parseDataspaceMessage(message: Hdf5HeaderMessage): Hdf5DataspaceInfo {
        val dataOffset = message.messageDataOffset
        val version = fileBytes[dataOffset.toInt()].toInt()
        val dimensionality = fileBytes[(dataOffset + 1).toInt()].toInt()
        // Skipping flags

        val dimensionDataOffset = dataOffset + 8 // Skip version, dimensionality, flags, reserved

        val dimensions = dimensionality j { i ->
            fileBytes.readLong(dimensionDataOffset + i * superblock.lengthsSize, PlatformCodec.currentPlatformCodec.isLittleEndian)
        }
        return Hdf5DataspaceInfo(dimensionality, dimensions)
    }

    private fun parseDatatypeMessage(message: Hdf5HeaderMessage): Hdf5DatatypeInfo {
        val dataOffset = message.messageDataOffset
        val versionAndClass = fileBytes[dataOffset.toInt()].toInt()
        val version = versionAndClass shr 4
        val typeClass = versionAndClass and 0x0F
        // Ignoring bitfield bytes 1-3 for standard types

        val sizeBytes = fileBytes.readUInt32(dataOffset + 4, PlatformCodec.currentPlatformCodec.isLittleEndian)

        // Determine endianness and signedness (simplified)
        val isLittleEndian: Boolean
        val isSigned: Boolean
        if (typeClass == 0) { // Fixed-point
             val bitfield1 = fileBytes[(dataOffset + 1).toInt()].toInt()
             isLittleEndian = (bitfield1 and 0x01) == 0 // Bit 0: 0=LE, 1=BE
             isSigned = (bitfield1 and 0x08) != 0       // Bit 3: 0=Unsigned, 1=Signed
        } else if (typeClass == 1) { // Floating-point
             val bitfield1 = fileBytes[(dataOffset + 1).toInt()].toInt()
             isLittleEndian = (bitfield1 and 0x01) == 0 // Bit 0: 0=LE, 1=BE
             isSigned = true // Floats are always signed in representation
        } else {
            throw UnsupportedOperationException("Unsupported datatype class $typeClass in simplified reader")
        }

        return Hdf5DatatypeInfo(typeClass, sizeBytes, isSigned, isLittleEndian)
    }

    private fun parseDataLayoutMessage(message: Hdf5HeaderMessage): Hdf5DataLayoutInfo {
        val dataOffset = message.messageDataOffset
        val version = fileBytes[dataOffset.toInt()].toInt()
        val layoutClass = fileBytes[(dataOffset + 1).toInt()].toInt()

        return when (layoutClass) {
            1 -> { // Contiguous
                val address = fileBytes.readLong(dataOffset + 2, PlatformCodec.currentPlatformCodec.isLittleEndian)
                val size = fileBytes.readLong(dataOffset + 2 + superblock.offsetsSize, PlatformCodec.currentPlatformCodec.isLittleEndian)
                Hdf5DataLayoutInfo(layoutClass, address, size)
            }
            0, 2 -> Hdf5DataLayoutInfo(layoutClass, null, null) // Compact/Chunked not fully parsed
            else -> throw IllegalArgumentException("Unknown data layout class: $layoutClass")
        }
    }

    // Decode element bytes based on HDF5 type and platform endianness
    private fun decodeElement(bytes: ByteArray, datatype: Hdf5DatatypeInfo): Any {
        // Get the codec required by the HDF5 data's specified endianness
        val codec = PlatformCodecEnhanced.get(datatype.isLittleEndian)

        return when (datatype.typeClass) {
            0 -> when (datatype.sizeBytes) { // Fixed-Point
                 1 -> bytes[0] // Byte is endian-neutral
                 2 -> codec.readShort(bytes)
                 4 -> codec.readInt(bytes)
                 8 -> codec.readLong(bytes)
                 else -> "[Int${datatype.sizeBytes}]".also { println("Warn: Int size ${datatype.sizeBytes}") }
            }
            1 -> when (datatype.sizeBytes) { // Floating-Point
                4 -> codec.readFloat(bytes) // Use codec's float method
                8 -> codec.readDouble(bytes) // Use codec's double method
                else -> "[Float${datatype.sizeBytes}]".also { println("Warn: Float size ${datatype.sizeBytes}") }
            }
            else -> "[TypeClass ${datatype.typeClass}]"
        }
    }
}
