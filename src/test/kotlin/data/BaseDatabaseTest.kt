package tests.data

import java.io.File
import org.junit.jupiter.api.*
import data.DatabaseManager

const val TEST_PATH: String = "data/test/testdatabase.db"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseDatabaseTest {
    @BeforeAll
    fun setupDatabase() {
        DatabaseManager.initialise(TEST_PATH)
        DatabaseManager.createTables()
    }

    @AfterAll
    fun deleteDatabase() {
        DatabaseManager.disconnect()
        val file = File(TEST_PATH)
        file.delete()
    }

    @BeforeEach
    fun startTransaction() {
        DatabaseManager.executeSQL("BEGIN TRANSACTION test")
    }

    @AfterEach
    fun endTransaction() {
        DatabaseManager.executeSQL("ROLLBACK")
    }
}
