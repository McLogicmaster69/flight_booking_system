package tests.data

import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.*
import data.DatabaseManager
import data.TimezoneData
import data.TimezoneColumns
import data.WhereArgs

class DatabaseTests : BaseDatabaseTest() {
    @Test
    fun `check database has been initialised`() {
        assertTrue(DatabaseManager.dbInitialised)
    }

    @Test
    fun `check query`() {
        val results =
            DatabaseManager.queryTable(
                TimezoneData.EMPTY.tableName,
                TimezoneColumns.COLUMN_NAMES,
            )

        assertNotEquals(results.size, 0)
    }

    @Test
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
