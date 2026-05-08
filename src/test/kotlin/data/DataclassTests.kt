package tests.data

import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.*
import data.DatabaseManager
import data.TimezoneData

class DataclassTests {
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
    fun `check dataclasses can query`() {
        for (dataclass in DatabaseManager.dataClasses) {
            dataclass.queryDatabase()
        }
    }

    @Test
    @Order(3)
    fun `check insert`() {
        val results =
            TimezoneData(
                name = "TEST",
                timeOffset = 10f,
            ).insertIntoDatabase()

        assertNotEquals(results, -1)
    }

    @Test
    @Order(4)
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
    @Order(5)
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
