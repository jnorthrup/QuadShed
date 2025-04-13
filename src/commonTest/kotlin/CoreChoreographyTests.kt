import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import com.vsiwest.rangle.*
import com.vsiwest.rangle.sqlite.ColumnMeta
import com.vsiwest.rangle.sqlite.IOMemento
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

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
}
