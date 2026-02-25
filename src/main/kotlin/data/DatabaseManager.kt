package data

import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

fun anyToBool(i : Any?) : Boolean? = (i as? Int)?.let { it != 0}

object DatabaseManager {
    private val dbFilePath : String = "data/database.db"
    private val connection : Connection

    init {
        connection = connect()
    }

    fun connect(): Connection {
        val dbPath = File(dbFilePath)
        println("SQLite DB absolute path: ${dbPath.absolutePath}")
        dbPath.parentFile?.mkdirs()
        val url = "jdbc:sqlite:$dbFilePath"
        val conn = DriverManager.getConnection(url)

        conn.createStatement().use {
            it.execute("PRAGMA foreign_keys = ON;")
        }

        return conn
    }

    fun executeSQL(sql : String) {
        connection.createStatement().use { stmt ->
            stmt.execute(sql)
        }
    }

    fun createTable(table : String, columns : String) {
        if (table.isBlank()) return;

        val sql = """
            CREATE TABLE IF NOT EXISTS $table (
                $columns
            );
        """.trimIndent()

        executeSQL(sql)
    }

    fun createTables() {
        val dataClasses: List<DataClass<*>> = listOf(
            UserData.EMPTY,
            BookerData.EMPTY,
            BookingData.EMPTY
        )

        for (data in dataClasses) {
            DatabaseManager.createTable(
                data.tableName,
                data.tableCreateSQL
            )
        }
    }

    fun insertIntoTable(table : String, values : Map<Column<*>, Any?>) {
        if (values.size == 0) return

        val entries = values.entries.toList()
        val columns = values.keys.joinToString(", ") { it.name }
        val placeholders = List(values.size) { "?" }.joinToString(", ")

        val sql = "INSERT INTO $table ($columns) VALUES ($placeholders)"

        connection.prepareStatement(sql).use { stmt ->
            entries.forEachIndexed { index, entry ->
                stmt.setObject(index + 1, entry.value)
            }
            stmt.executeUpdate()
        }
    }

    fun queryTable(
        table : String,
        columns : List<String>,
        whereClause : String? = null,
        whereArgs : List<Any?> = emptyList()): List<Array<Any?>> {

        val columnStr = columns.joinToString(", ")
        val sql = buildString {
            append("SELECT $columnStr FROM $table")
            if (whereClause != null) append(" WHERE $whereClause")
        }

        connection.prepareStatement(sql).use { stmt ->
            whereArgs.forEachIndexed { i, value ->
                stmt.setObject(i + 1, value)
            }

            stmt.executeQuery().use { rs ->
                val results = mutableListOf<Array<Any?>>()

                while (rs.next()) {
                    val row = Array(columns.size) { index ->
                        rs.getObject(index + 1)
                    }
                    results.add(row)
                }

                return results
            }
        }
    }
}
