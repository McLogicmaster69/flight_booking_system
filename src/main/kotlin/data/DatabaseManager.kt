package data

import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import org.mindrot.jbcrypt.BCrypt
import org.json.JSONObject

fun anyToBool(i : Any?) : Boolean? = (i as? Int)?.let { it != 0}

object DatabaseManager {
    private val dbFilePath : String = "data/database.db"
    private val adminJSONFilePath : String = "data/admin.json"
    private val connection : Connection
    private var initialisedTables : MutableList<DataClass<*>> = mutableListOf()

    private val dataClasses: List<DataClass<*>> = listOf(
        AdminData.EMPTY,
        AdminSessionData.EMPTY,
        AssignedFlightStaffData.EMPTY,
        BookedSeatData.EMPTY,
        BookerData.EMPTY,
        BookingData.EMPTY,
        CartItemData.EMPTY,
        ClassData.EMPTY,
        CountryData.EMPTY,
        DestinationData.EMPTY,
        FlightData.EMPTY,
        FlightSearchData.EMPTY,
        FlightSearchFlightData.EMPTY,
        GuestData.EMPTY,
        LoginData.EMPTY,
        ManufacturerData.EMPTY,
        PaymentMethodData.EMPTY,
        PlaneData.EMPTY,
        PlaneModelData.EMPTY,
        PlaneSeatData.EMPTY,
        RemainingSeatData.EMPTY,
        RouteData.EMPTY,
        SeatData.EMPTY,
        SessionData.EMPTY,
        StaffData.EMPTY,
        StaffPositionData.EMPTY,
        StaffSessionData.EMPTY,
        TicketTypeData.EMPTY,
        TimezoneData.EMPTY,
        TwoFAData.EMPTY,
        UserData.EMPTY
    )

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

    fun createTable(table : String, columns : String, additional : String) {
        if (table.isBlank()) return;

        val sql = """
            CREATE TABLE IF NOT EXISTS $table (
                $columns${if (additional.isNotBlank()) ",\n$additional" else ""}
            );
        """.trimIndent()

        executeSQL(sql)
    }

    fun createTables() {
        println("Initialising database")
        initialisedTables = mutableListOf()

        for (dataClass in dataClasses) {
            initialiseTable(dataClass)
        }

        initialisedTables.clear()

        seedAdminAccount()
        println("Database initilisation completed")
    }

    fun initialiseTable(dataClass : DataClass<*>) {
        if (initialisedTables.any { it::class == dataClass::class })
            return

        println("Initialising ${dataClass.tableName}")

        DatabaseManager.createTable(
            dataClass.tableName,
            dataClass.tableCreateSQL,
            dataClass.tableAdditionalSQL
        )

        for (requirement in dataClass.requiredTables) { // WARNING: If two tables require records from one another when initialising, this will break :)
            initialiseTable(requirement)
        }

        println("Adding rows to ${dataClass.tableName}")
            
        for (row in dataClass.initialRows) {
            row.insertIntoDatabase(true)
        }

        initialisedTables.add(dataClass)
        dataClass.initTable()

        println("Finished initialising ${dataClass.tableName}")
    }

    fun insertIntoTable(
        table : String,
        values : Map<Column<*>, Any?>,
        ignore : Boolean = false
    ) : Int {

        if (values.size == 0) return -1

        val entries = values.entries.toList()
        val columns = values.keys.joinToString(", ") { it.name }
        val placeholders = List(values.size) { "?" }.joinToString(", ")

        val sql = "INSERT ${if (ignore) "OR IGNORE " else ""}INTO $table ($columns) VALUES ($placeholders)"
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
        whereArgs : WhereArgs? = null,
        orderByArgs : OrderByArgs? = null,
        limitArgs : LimitArgs? = null
    ): List<Array<Any?>> {

        var columnStr = columns.joinToString(", ") { "$table.$it" }
        if (joinArgs != null) columnStr += ", " + joinArgs.joinSelectColumns.joinToString(", ") { "${joinArgs.joinTable}.$it" }

        val sql = buildString {
            append("SELECT $columnStr FROM $table")

            if (joinArgs != null) append(" ${joinArgs.joinType} JOIN ${joinArgs.joinTable} ON $table.${joinArgs.joinTable1Column} = ${joinArgs.joinTable}.${joinArgs.joinTable2Column}")
            if (whereArgs != null) append(" WHERE (${whereArgs.whereClause})")
            if (orderByArgs != null) append(" ORDER BY ${orderByArgs.orderArgs.joinToString(", ") { "${it.orderColumn} ${if (it.ascending) "ASC" else "DESC"}" }}")
            if (limitArgs != null) append(" LIMIT ${limitArgs.limitAmount}")
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

    fun updateInDatabase(
        table: String,
        id: Int,
        values: Map<Column<*>, Any?>
    ) {
        if (values.isEmpty()) return

        val setClause = values.keys.joinToString(", ") { "${it.name} = ?" }

        val sql = "UPDATE $table SET $setClause WHERE id = ?"

        connection.prepareStatement(sql).use { stmt ->
            values.values.forEachIndexed { index, value ->
                stmt.setObject(index + 1, value)
            }
            stmt.setObject(values.size + 1, id)
            stmt.executeUpdate()
        }
    }

    fun seedAdminAccount() {
        val adminJSONFile : File = File(adminJSONFilePath)
        require(adminJSONFile.exists()) { "Admin JSON file cannot be found" }

        val adminJSONString = adminJSONFile.readText(Charsets.UTF_8).trimIndent()
        val adminJSONObject = JSONObject(adminJSONString)

        val adminEmail = adminJSONObject.getString("email")
        val adminPassword = adminJSONObject.getString("password")

        val existing = LoginData.queryDatabase(
            whereArgs = WhereArgs(
                "${LoginColumns.EMAIL.name} = ?",
                listOf(adminEmail)
            )
        )

        if (existing.isNotEmpty()) {
            println("Admin already exists")
            return
        }

        println("Seeding admin account")

        val passwordHash = BCrypt.hashpw(adminPassword, BCrypt.gensalt())

        val loginId = LoginData(
            email = adminEmail,
            passwordHash = passwordHash
        ).insertIntoDatabase()

        UserData(
            firstName = "Admin",
            lastName = "User",
            verifiedAccount = true,
            loyalityPoints = 0,
            loginId = loginId
        ).insertIntoDatabase()

        AdminData(
            loginId = loginId
        ).insertIntoDatabase()
    }

    fun debugDatabase() {
        for (dataClass in dataClasses) {
            dataClass.debugTable()
        }
    }
}
