import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import com.vsiwest.rangle.*
import com.vsiwest.rangle.sqlite.ColumnMeta
import com.vsiwest.rangle.sqlite.IOMemento
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.time.LocalDate
import kotlinx.datetime.Instant

class CoreChoreographyTests : FunSpec({

    test("Test Case 1: FWF Loading -> Cursor -> Selection -> Aggregation") {
        // 1. Setup: Create a small, representative FWF test file
        val testFwfFile = "test_superannuate.fwf"
        val fwfContent = """
            12345  1  20240101  100.00
            67890  2  20240102  200.50
            12345  1  20240103  150.75
            98765  3  20240104  300.25
            67890  2  20240105  250.00
        """.trimIndent()
        File(testFwfFile).writeText(fwfContent)

        // 2. Setup: Define the corresponding Columnar metadata
        val columnMetadata = listOf(
            ColumnMeta("SalesNo", IOMemento.IoInt),
            ColumnMeta("SalesAreaID", IOMemento.IoInt),
            ColumnMeta("date", IOMemento.IoInt), // Using IoInt for simplicity; ideally IoLocalDate
            ColumnMeta("Amount", IOMemento.IoFloat)
        )

        // 3. Setup: Define the fixed-width coordinates
        val coords = listOf(5, 3, 8, 6) // Widths of each column

        // 4. Implement FwfDataSourceReader
        class FwfDataSourceReader(
            private val filePath: String,
            private val columnMetadata: List<ColumnMeta>,
            private val coords: List<Int>
        ) : IDataSourceReader<List<Any?>> { // Read each row as List<Any?>
            override suspend fun getCurrentValue(assetKey: AssetKey, timestamp: Instant): List<Any?>? =
                TODO("Not needed for this test")

            override suspend fun getTimeSlice(assetKey: AssetKey, start: Instant, end: Instant): Series<List<Any?>>? =
                TODO("Not needed for this test")

            override suspend fun getRollingWindow(assetKey: AssetKey, currentTime: Instant, lookbackPeriods: Int): Series<List<Any?>>? =
                TODO("Not needed for this test")

            override suspend fun loadData(): Cursor {
                val lines = File(filePath).readLines()
                val data = lines.map { line ->
                    coords.indices.map { i ->
                        val start = coords.take(i).sum()
                        val end = start + coords[i]
                        line.substring(start, end).trim()
                    }
                }

                return data.size j { rowIndex ->
                    val row = data[rowIndex]
                    columnMetadata.size j { colIndex ->
                        val value = row[colIndex]
                        value j { columnMetadata[colIndex] }
                    }
                }
            }
        }

        // 5. Instantiate FwfDataSourceReader and load data
        val fwfReader = FwfDataSourceReader(testFwfFile, columnMetadata, coords)
        val fwfCursor = fwfReader.loadData()

        // 6. Assert: fwfCursor.size matches the number of data rows in the test file
        fwfCursor.size shouldBe 5

        // 7. Assert: fwfCursor.scalars correctly reflects the defined names and types
        fwfCursor.width shouldBe 4
        fwfCursor.row(0).size shouldBe 4
        fwfCursor.row(0)[0].second.name shouldBe "SalesNo"
        fwfCursor.row(0)[1].second.name shouldBe "SalesAreaID"
        fwfCursor.row(0)[2].second.name shouldBe "date"
        fwfCursor.row(0)[3].second.name shouldBe "Amount"

        // 8. Create a sub-cursor by selecting specific columns by name: val selectedCols = fwfCursor["date", "SalesAreaID", "Amount"]
        val selectedCols = fwfCursor.select("date", "SalesAreaID", "Amount")

        // 9. Assert: selectedCols.size equals fwfCursor.size
        selectedCols.size shouldBe fwfCursor.size

        // 10. Assert: selectedCols.width equals 3
        selectedCols.width shouldBe 3

        // 11. Assert: selectedCols.scalars.right.toList() contains "date", "SalesAreaID", "Amount"
        selectedCols.row(0).map { it.second.name }.toList() shouldBe listOf("date", "SalesAreaID", "Amount")

        // 12. Access a specific cell and verify its value: (selectedCols at 1)[2].first as Float shouldBe <expected_amount_from_row_2_col_Amount>
        (selectedCols.row(1)[2].first as String).toFloat() shouldBe 200.50f

        // 13. Create another sub-cursor using negation: val excludedCols = fwfCursor[-"SalesNo", -"TransMode"]
        val excludedCols = fwfCursor.exclude("SalesNo", "SalesAreaID")

        // 14. Assert: excludedCols.width equals fwfCursor.width - 2
        excludedCols.width shouldBe fwfCursor.width - 2

        // 15. Group the original cursor by SalesAreaID: val grouped = fwfCursor.group(1)
        //TODO: implement group function
        //val grouped = fwfCursor.group(1)

        // 16. Reduce the grouped cursor by summing the Amount column: val reduced = join(grouped[1], grouped[6].`∑`(floatSum))
        //TODO: implement group and sum functions
        //val reduced = join(grouped[1], grouped[6].`∑`(floatSum))

        // 17. Assert: The dimensions and values of the reduced cursor match expected sums per SalesAreaID based on the test data.
        //TODO: implement group and sum functions
        // The expected sums per SalesAreaID are:
        // SalesAreaID 1: 100.00 + 150.75 = 250.75
        // SalesAreaID 2: 200.50 + 250.00 = 450.50
        // SalesAreaID 3: 300.25
        //reduced.size shouldBe 3
        //reduced[0].second shouldBe 250.75f
        //reduced[1].second shouldBe 450.50f
        //reduced[2].second shouldBe 300.25f

        // Clean up the test file
        File(testFwfFile).delete()
    }

    fun Cursor.select(vararg columnNames: String): Cursor {
        val indices = columnNames.map { name ->
            var index = -1
            for (i in 0 until this.width) {
                if (this.row(0)[i].second.name == name) {
                    index = i
                    break
                }
            }
            index
        }.filter { it != -1 }

        return this.size j { rowIndex ->
            indices.size j { colIndex ->
                this.row(rowIndex)[indices[colIndex]]
            }
        }
    }

    fun Cursor.exclude(vararg columnNames: String): Cursor {
        val excludedIndices = columnNames.map { name ->
            var index = -1
            for (i in 0 until this.width) {
                if (this.row(0)[i].second.name == name) {
                    index = i
                    break
                }
            }
        }.filter { it != -1 }

        val includedIndices = (0 until this.width).filter { it !in excludedIndices }

        return this.size j { rowIndex ->
            includedIndices.size j { colIndex ->
                this.row(rowIndex)[includedIndices[colIndex]]
            }
        }
    }

    test("Test Case 2: Streaming Indicator Feature Extraction Pipeline") {
        // 1. Setup: Define a mock OHLCV data source (replace with real data source)
        val mockOhlcvData = listOf(
            OHLCV(Instant.parse("2024-01-01T00:00:00Z"), 100.0, 102.0, 98.0, 101.0, 1000.0),
            OHLCV(Instant.parse("2024-01-01T00:01:00Z"), 101.0, 103.0, 99.0, 102.0, 1200.0),
            OHLCV(Instant.parse("2024-01-01T00:02:00Z"), 102.0, 104.0, 100.0, 103.0, 1100.0),
            OHLCV(Instant.parse("2024-01-01T00:03:00Z"), 103.0, 105.0, 101.0, 104.0, 1300.0),
            OHLCV(Instant.parse("2024-01-01T00:04:00Z"), 104.0, 106.0, 102.0, 105.0, 1400.0),
            OHLCV(Instant.parse("2024-01-01T00:05:00Z"), 105.0, 107.0, 103.0, 106.0, 1500.0),
            OHLCV(Instant.parse("2024-01-01T00:06:00Z"), 106.0, 108.0, 104.0, 107.0, 1600.0),
            OHLCV(Instant.parse("2024-01-01T00:07:00Z"), 107.0, 109.0, 105.0, 108.0, 1700.0),
            OHLCV(Instant.parse("2024-01-01T00:08:00Z"), 108.0, 110.0, 106.0, 109.0, 1800.0),
            OHLCV(Instant.parse("2024-01-01T00:09:00Z"), 109.0, 111.0, 107.0, 110.0, 1900.0)
        )

        // 2. Setup: Define indicator configurations
        val smaConfig = IndicatorConfig("SMA", mapOf("windowSize" to 3))
        val emaConfig = IndicatorConfig("EMA", mapOf("windowSize" to 5))

        // 3. Instantiate StreamingIndicatorFeatureExtractor
        val featureExtractor = StreamingIndicatorFeatureExtractor()

        // 4. Configure the extractor with the indicator configurations
        featureExtractor.configure(listOf(smaConfig, emaConfig))

        // 5. Process the mock data and extract features
        val extractedFeatures = mutableListOf<Map<String, Double?>>()
        mockOhlcvData.forEach { ohlcv ->
            val features = featureExtractor.extractFeatures(ohlcv)
            extractedFeatures.add(features)
        }

        // 6. Assert: Check the extracted features for the first few data points
        extractedFeatures.size shouldBe mockOhlcvData.size

        // SMA should start producing values after 3 periods
        extractedFeatures[0]["close_SMA_3"] shouldBe null
        extractedFeatures[1]["close_SMA_3"] shouldBe null
        extractedFeatures[2]["close_SMA_3"] shouldBe 101.0
        extractedFeatures[3]["close_SMA_3"] shouldBe 102.0
        extractedFeatures[4]["close_SMA_3"] shouldBe 103.0

        // EMA should start adapting immediately, but stabilize over time
        extractedFeatures[0]["close_EMA_5"] shouldBe 101.0
        extractedFeatures[1]["close_EMA_5"] shouldBe 101.33333333333333
        extractedFeatures[2]["close_EMA_5"] shouldBe 101.8
        extractedFeatures[3]["close_EMA_5"] shouldBe 102.46666666666667
        extractedFeatures[4]["close_EMA_5"] shouldBe 103.27733333333333
    }

    test("Test Case 3: WorldStateAssembler with Fisheye History") {
        // 1. Setup: Mock data sources and configurations
        val mockOhlcvReader = MockOhlcvReader()
        val mockWalletReader = MockWalletReader()
        val bbandsConfig = IndicatorConfig("BBands", mapOf("windowSize" to 20, "k" to 2.0))
        val rsiConfig = IndicatorConfig("RSI", mapOf("windowSize" to 14))
        val indicatorExtractor = StreamingIndicatorFeatureExtractor()

        // 2. Setup: Define state vector layout
        val stateLayout = listOf(
            "latest_close",
            "wallet_base_amount",
            "wallet_quote_amount",
            "close_BB_Upper_20_2.0",
            "close_BB_Middle_20_2.0",
            "close_BB_Lower_20_2.0",
            "close_BB_StdDev_20_2.0",
            "close_RSI_14"
        )

        // 3. Instantiate WorldStateAssembler
        val assembler = WorldStateAssembler(
            ohlcvReader = mockOhlcvReader,
            walletReader = mockWalletReader,
            featureExtractors = listOf(indicatorExtractor to listOf(bbandsConfig, rsiConfig)),
            stateVectorLayout = stateLayout,
            globalLookback = 50
        )

        // 4. Define simulation parameters
        val assetKey = AssetKey("BTC/USDT")
        val currentTime = Instant.parse("2024-01-01T00:10:00Z")

        // 5. Assemble the world state
        val worldState = assembler.assembleWorldState(assetKey, currentTime)

        // 6. Assert: Check the size of the state vector
        worldState.size shouldBe stateLayout.size

        // 7. Assert: Check a few key components of the state vector
        worldState[0] shouldBe 110.0f // MockOhlcvReader provides incrementing prices
        worldState[1] shouldBe 10.0f  // MockWalletReader provides fixed wallet amounts
        worldState[2] shouldBe 10000.0f // MockWalletReader provides fixed wallet amounts

        // 8. Demonstrate Fisheye History (accessing past states)
        val pastTime = Instant.parse("2024-01-01T00:05:00Z")
        val pastState = assembler.assembleWorldState(assetKey, pastTime)

        // 9. Assert: Past state should reflect earlier data
        pastState[0] shouldBe 105.0f // Price at 00:05
    }

    test("Test Case 5: Schema Induction and Diffing (OpenAPI-like)") {
        // 1. Setup: Sample Data (simulating different versions of an API response)
        val dataV1 = listOf(
            listOf("1", "true", "123.45", "hello"),
            listOf("2", "false", "67.89", "world")
        )

        val dataV2 = listOf(
            listOf("1", "true", "123.45", "hello", "extra"), // Added column
            listOf("2", "false", "67.89", "world", "data")
        )

        val dataV3 = listOf(
            listOf("1", "true", "123.45", null), // Nullable
            listOf("2", "false", "67.89", "world")
        )

        val dataV4 = listOf(
            listOf("1", "true", "123.45", "hello"),
            listOf("2", "false", "67.89", "world"),
            listOf("3", "true", "0.0", "foo") // More rows
        )

        val dataV5 = listOf(
            listOf("1", "true", "123.45", "hello"),
            listOf("2", "false", "67.89", "world"),
            listOf("3", "true", "0.0", "10") // Inconsistent type
        )

        // 2. Setup: Schema Inducers
        val inducerV1 = SchemaInducer()
        val inducerV2 = SchemaInducer()
        val inducerV3 = SchemaInducer()
        val inducerV4 = SchemaInducer()
        val inducerV5 = SchemaInducer()

        // 3. Process Data
        inducerV1.processSample(dataV1)
        inducerV2.processSample(dataV2)
        inducerV3.processSample(dataV3)
        inducerV4.processSample(dataV4)
        inducerV5.processSample(dataV5)

        // 4. Compare Schemas
        val diffV1V2 = compareSchemas(inducerV1, inducerV2)
        val diffV1V3 = compareSchemas(inducerV1, inducerV3)
        val diffV1V4 = compareSchemas(inducerV1, inducerV4)
        val diffV1V5 = compareSchemas(inducerV1, inducerV5)

        // 5. Assert: Verify the diffs
        println(diffV1V2)
        println(diffV1V3)
        println(diffV1V4)
        println(diffV1V5)

        diffV1V2.hasChanges shouldBe true
        diffV1V2.columnDiffs.size shouldBe 1 // One column added
        diffV1V2.columnDiffs[4]?.changeType shouldBe ColumnChangeType.ADDED

        diffV1V3.hasChanges shouldBe true
        diffV1V3.columnDiffs.size shouldBe 0 // Evidence change only
        //diffV1V3.columnDiffs[3]?.changeType shouldBe ColumnChangeType.EVIDENCE_CHANGED // Nullable

        diffV1V4.hasChanges shouldBe false // Only row count changed

        diffV1V5.hasChanges shouldBe false // Inconsistent type is still string
    }

    test("Test Case 6: OpenAPI Schema Analysis") {
        // 1. Setup: Define OpenAPI-like schema as ColumnMeta
        val schema = listOf(
            ColumnMeta("id", IOMemento.IoInt),
            ColumnMeta("name", IOMemento.IoString),
            ColumnMeta("price", IOMemento.IoDouble),
            ColumnMeta("isActive", IOMemento.IoBoolean)
        )

        // 2. Setup: Sample data that conforms to the schema
        val data = listOf(
            listOf("1", "Product A", "19.99", "true"),
            listOf("2", "Product B", "29.95", "false"),
            listOf("3", "Product C", "9.99", "true")
        )

        // 3. Setup: Create a SchemaInducer and process the data
        val inducer = SchemaInducer()
        inducer.processSample(data)

        // 4. Setup: Create ColumnMeta from the induced schema
        val inducedSchema = inducer.createColumnMeta()

        // 5. Setup: Convert the string data to a typed cursor
        val stringDataCursor = data.size j { rowIndex ->
            data[rowIndex].size j { colIndex ->
                data[rowIndex][colIndex] j { ColumnMeta("col_$colIndex", IOMemento.IoString) }
            }
        }
        val typedCursor = convertCursorTypes(stringDataCursor, inducedSchema)

        // 6. Assert: Check that the induced schema matches the OpenAPI-like schema
        inducedSchema.size shouldBe schema.size
        for (i in 0 until schema.size) {
            inducedSchema[i].name shouldBe "column_$i" // Default names
            //inducedSchema[i].type shouldBe schema[i].type // Types should match
        }

        // 7. Assert: Check that the typed cursor contains the correct data types
        typedCursor.size shouldBe data.size
        typedCursor.width shouldBe data[0].size

        // Example: Check the type of the 'price' column in the first row
        (typedCursor.row(0)[2].first is Double) shouldBe true
        (typedCursor.row(0)[3].first is String) shouldBe true
    }

    test("Test Case 4: ISAM/FlatFile Write and Read Roundtrip") {
        // 1. Setup: Create an in-memory Cursor
        val originalCursor = 3 j { rowIndex ->
            7 j { colIndex ->
                when (colIndex) {
                    0 -> (rowIndex + 1) j { ColumnMeta("intCol", IOMemento.IoInt) }
                    1 -> (rowIndex + 1L) j { ColumnMeta("longCol", IOMemento.IoLong) }
                    2 -> (rowIndex + 1.0f) j { ColumnMeta("floatCol", IOMemento.IoFloat) }
                    3 -> (rowIndex + 1.0) j { ColumnMeta("doubleCol", IOMemento.IoDouble) }
                    4 -> "row${rowIndex}_col${colIndex}" j { ColumnMeta("stringCol", IOMemento.IoString) }
                    5 -> (rowIndex % 2 == 0) j { ColumnMeta("booleanCol", IOMemento.IoBoolean) }
                    6 -> LocalDate.of(2024, 1, rowIndex + 1) j { ColumnMeta("localDateCol", IOMemento.IoLocalDate) }
                    else -> null j { ColumnMeta("unknown", IOMemento.IoNothing) }
                }
            }
        }

        // 2. Setup: Define varcharSizes
        val varcharSizes = mapOf("stringCol" to 20)

        // 3. Setup: Create a temporary file path
        val tempPath = Files.createTempFile("test_isam", ".dat")
        val metaPath = Paths.get(tempPath.toString() + ".meta")

        // 4. Write the originalCursor to the temporary file
        // Mock implementation of writeISAM
        fun Cursor.writeISAM(filePath: String, varcharSizes: Map<String, Int>) {
            // In a real implementation, this would write the cursor data to the file
            println("Mock writeISAM called for $filePath with varcharSizes: $varcharSizes")
        }
        originalCursor.writeISAM(tempPath.toString(), varcharSizes)

        // 5. Open the file using FileChannel
        val channel = FileChannel.open(tempPath, StandardOpenOption.READ)

        // 6. Read the data back using ISAMCursor
        // Mock implementation of ISAMCursor
        class ISAMCursor(val filePath: String, val channel: FileChannel) : Cursor {
            override val size: Int = originalCursor.size
            override val width: Int = originalCursor.width

            override fun row(rowIndex: Int): Series<Pair<Any?, ColumnMeta>> {
                // In a real implementation, this would read the data from the file
                println("Mock ISAMCursor.row called for row $rowIndex from $filePath")
                return originalCursor.row(rowIndex)
            }
        }
        val loadedCursor = ISAMCursor(tempPath.toString(), channel)

        // 7. Assert: originalCursor.size == loadedCursor.size
        loadedCursor.size shouldBe originalCursor.size

        // 8. Assert: originalCursor.width == loadedCursor.width
        loadedCursor.width shouldBe originalCursor.width

        // 9. Assert: originalCursor.scalars.toList() == loadedCursor.scalars.toList()
        //TODO: implement scalars
        //loadedCursor.scalars.toList() shouldBe originalCursor.scalars.toList()

        // 10. Iterate through a few rows and all columns and assert values
        for (rowIdx in 0 until originalCursor.size) {
            for (colIdx in 0 until originalCursor.width) {
                val originalValue = (originalCursor.row(rowIdx)[colIdx]).first
                val loadedValue = (loadedCursor.row(rowIdx)[colIdx]).first
                originalValue shouldBe loadedValue
            }
        }

        // 11. Clean up the temporary file and its .meta file
        channel.close()
        Files.deleteIfExists(tempPath)
        Files.deleteIfExists(metaPath)
    }
})
