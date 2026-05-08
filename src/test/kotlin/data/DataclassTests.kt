package tests.data

import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.*
import data.DatabaseManager
import data.TimezoneData

class DataclassTests : BaseDatabaseTest() {
    @Test
    fun `check database has been initialised`() {
        assertTrue(DatabaseManager.dbInitialised)
    }

    @Test
    fun `check dataclasses can query`() {
        for (dataclass in DatabaseManager.dataClasses) {
            dataclass.queryDatabase()
        }
    }

    @Test
    fun `check insert`() {
        val results =
            TimezoneData(
                name = "TEST",
                timeOffset = 10f,
            ).insertIntoDatabase()

        assertNotEquals(results, -1)
    }

    @Test
    fun `check update`() {
        val id =
            TimezoneData(
                name = "TO_UPDATE",
                timeOffset = 10f,
            ).insertIntoDatabase()

        val results =
            TimezoneData(
                id = id,
                name = "TO_UPDATE",
                timeOffset = 5f,
            ).update()

        assertNotEquals(results, 0)
    }

    @Test
    fun `check delete`() {
        val id =
            TimezoneData(
                name = "TO_DELETE",
                timeOffset = 10f,
            ).insertIntoDatabase()

        val results =
            TimezoneData(
                id = id,
                name = "TO_DELETE",
                timeOffset = 10f,
            ).delete()

        assertNotEquals(results, 0)
    }
}
