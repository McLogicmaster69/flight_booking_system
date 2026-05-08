package tests.data

import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.*
import data.DatabaseManager
import data.TimezoneData
import data.TimezoneColumns
import data.WhereArgs

const val TEST_PATH: String = "data/test/testdatabase.db"

class DatabaseTests {
    companion object {
        @JvmStatic
        @BeforeAll
        fun setupDatabase() {
            DatabaseManager.initialise(TEST_PATH)
            DatabaseManager.createTables()
        }

        @JvmStatic
        @AfterAll
        fun deleteDatabase() {
            DatabaseManager.disconnect()
            val file = File(TEST_PATH)
            file.delete()
        }
    }

    @Test
    @Order(1)
    fun `check database has been initialised`() {
        assertTrue(DatabaseManager.dbInitialised)
    }

    @Test
    @Order(2)
    fun `check query`() {
        val results =
            DatabaseManager.queryTable(
                TimezoneData.EMPTY.tableName,
                TimezoneColumns.COLUMN_NAMES,
            )

        assertNotEquals(results.size, 0)
    }

    @Test
    @Order(3)
    fun `check insert`() {
        val results =
            DatabaseManager.insertIntoTable(
                TimezoneData.EMPTY.tableName,
                mapOf(
                    TimezoneColumns.NAME to "TEST",
                    TimezoneColumns.TIME_OFFSET to 10f,
                ),
            )

        assertNotEquals(results, -1)
    }

    @Test
    @Order(4)
    fun `check update`() {
        DatabaseManager.insertIntoTable(
            TimezoneData.EMPTY.tableName,
            mapOf(
                TimezoneColumns.NAME to "TO_UPDATE",
                TimezoneColumns.TIME_OFFSET to 10f,
            ),
        )

        val results =
            DatabaseManager.updateTable(
                TimezoneData.EMPTY.tableName,
                mapOf(
                    TimezoneColumns.TIME_OFFSET to 5f,
                ),
                WhereArgs(
                    "${TimezoneColumns.NAME.name} = ?",
                    listOf("TO_UPDATE"),
                ),
            )

        assertNotEquals(results, 0)
    }

    @Test
    @Order(5)
    fun `check delete`() {
        DatabaseManager.insertIntoTable(
            TimezoneData.EMPTY.tableName,
            mapOf(
                TimezoneColumns.NAME to "TO_DELETE",
                TimezoneColumns.TIME_OFFSET to 10f,
            ),
        )

        val results =
            DatabaseManager.deleteFromTable(
                TimezoneData.EMPTY.tableName,
                WhereArgs(
                    "${TimezoneColumns.NAME.name} = ?",
                    listOf("TO_DELETE"),
                ),
            )

        assertNotEquals(results, 0)
    }
}
