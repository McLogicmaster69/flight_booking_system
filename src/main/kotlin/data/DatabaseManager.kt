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
            AdminData.EMPTY,
            AssignedFlightStaffData.EMPTY,
            BookedSeatData.EMPTY,
            BookerData.EMPTY,
            BookingData.EMPTY,
            ClassData.EMPTY,
            DestinationData.EMPTY,
            FlightData.EMPTY,
            GuestData.EMPTY,
            LoginData.EMPTY,
            ManufacturerData.EMPTY,
            PaymentMethodData.EMPTY,
            PlaneData.EMPTY,
            PlaneModelData.EMPTY,
            RemainingSeatData.EMPTY,
            RouteData.EMPTY,
            SeatData.EMPTY,
            StaffData.EMPTY,
            StaffPositionData.EMPTY,
            TicketTypeData.EMPTY,
            TwoFAData.EMPTY,
            UserData.EMPTY
        )

        for (data in dataClasses) {
            DatabaseManager.createTable(
                data.tableName,
                data.tableCreateSQL
            )
        }
    }

    fun insertIntoTable(
        table : String,
        values : Map<Column<*>, Any?>
    ) : Int {

        if (values.size == 0) return -1

        val entries = values.entries.toList()
        val columns = values.keys.joinToString(", ") { it.name }
        val placeholders = List(values.size) { "?" }.joinToString(", ")

        val sql = "INSERT INTO $table ($columns) VALUES ($placeholders)"
        var id : Int = -1

        connection.prepareStatement(sql).use { stmt ->
            entries.forEachIndexed { index, entry ->
                stmt.setObject(index + 1, entry.value)
            }
            stmt.executeUpdate()

            stmt.generatedKeys.use { keys ->
                if (keys.next()) {
                    id = keys.getInt(1)
                }
            }
        }

        return id
    }

    fun updateTable(
        table : String,
        values : Map<Column<*>, Any?>,
        whereArgs : WhereArgs
    ): Int {
        require(values.isNotEmpty()) { "No values where given to update" }
        
        val entries = values.entries.toList()
        val setClause = entries.joinToString(", ") { "${it.key.name} = ?" }
        val sql = "UPDATE $table SET $setClause WHERE ${whereArgs.whereClause}"

        connection.prepareStatement(sql).use { stmt ->
            var index = 1

            entries.forEach {
                stmt.setObject(index++, it.value)
            }

            whereArgs.whereArgs.forEach {
                stmt.setObject(index++, it)
            }
            
            return stmt.executeUpdate()
        }
    }

    fun deleteFromTable(
        table : String,
        whereArgs : WhereArgs
    ) : Int {
        require(whereArgs.whereClause.isNotBlank()) { "DELETE requires a WHERE clause" }

        val sql = "DELETE FROM $table WHERE ${whereArgs.whereClause}"

        connection.prepareStatement(sql).use { stmt ->
            whereArgs.whereArgs.forEachIndexed { i, value ->
                stmt.setObject(i + 1, value)
            }

            return stmt.executeUpdate()
        }
    }

    fun queryTable(
        table : String,
        columns : List<String>,
        joinArgs : JoinArgs? = null,
        whereArgs : WhereArgs? = null
    ): List<Array<Any?>> {

        var columnStr = columns.joinToString(", ") { "$table.$it" }
        if (joinArgs != null) columnStr += ", " + joinArgs.joinSelectColumns.joinToString(", ") { "${joinArgs.joinTable}.$it" }

        val sql = buildString {
            append("SELECT $columnStr FROM $table")
            if (joinArgs != null) append(" ${joinArgs.joinType} JOIN ${joinArgs.joinTable} ON $table.${joinArgs.joinTable1Column} = ${joinArgs.joinTable}.${joinArgs.joinTable2Column}")
            if (whereArgs != null) append(" WHERE ${whereArgs.whereClause}")
        }

        var totalSize = columns.size
        if (joinArgs != null)
            totalSize += joinArgs.joinSelectColumns.size

        connection.prepareStatement(sql).use { stmt ->
            if (whereArgs != null) {
                whereArgs.whereArgs.forEachIndexed { i, value ->
                    stmt.setObject(i + 1, value)
                }
            }

            stmt.executeQuery().use { rs ->
                val results = mutableListOf<Array<Any?>>()

                while (rs.next()) {
                    val row = Array(totalSize) { index ->
                        rs.getObject(index + 1)
                    }
                    results.add(row)
                }

                return results
            }
        }
    }
}
