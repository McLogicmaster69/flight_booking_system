package data

import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import org.mindrot.jbcrypt.BCrypt
import org.json.JSONObject

/**
 * Converts a database integer value to a Boolean.
 *
 * @param i Value to convert.
 * @return `true` if value is non-zero, `false` if zero, or `null` if not an Int.
 */
fun anyToBool(i: Any?): Boolean? = (i as? Int)?.let { it != 0 }

/**
 * Singleton responsible for managing all database operations.
 */
object DatabaseManager {
    /**
     * Path to the SQLite database file.
     */
    private val dbFilePath: String = "data/database.db"

    /**
     * Path to the admin seed JSON file.
     */
    private val adminJSONFilePath: String = "data/admin.json"

    /**
     * Active database connection.
     */
    private var connection: Connection? = null

    /**
     * Tracks tables already initialized to prevent duplicates.
     */
    private var initialisedTables: MutableList<DataClass<*>> = mutableListOf()

    /**
     * Signals if the database has been initialised
     */
    var dbInitialised: Boolean = false
        private set

    /**
     * List of all data classes managed by the database.
     */
    val dataClasses: List<DataClass<*>> =
        listOf(
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
            ScheduleData.EMPTY,
            SeatData.EMPTY,
            SessionData.EMPTY,
            StaffData.EMPTY,
            StaffPositionData.EMPTY,
            StaffSessionData.EMPTY,
            TicketTypeData.EMPTY,
            TimezoneData.EMPTY,
            TwoFAData.EMPTY,
            UserData.EMPTY,
        )

    /**
     * Initializes the database connection on startup.
     */
    fun initialise(path: String? = null) {
        connection = connect(path ?: dbFilePath)
    }

    /**
     * Creates and returns a connection to the SQLite database.
     *
     * @return Active SQL connection.
     */
    fun connect(path: String): Connection {
        val dbPath = File(path)
        println("SQLite DB absolute path: ${dbPath.absolutePath}")
        dbPath.parentFile?.mkdirs()

        val url = "jdbc:sqlite:$path"
        val conn = DriverManager.getConnection(url)

        conn.createStatement().use {
            it.execute("PRAGMA foreign_keys = ON;")
        }

        return conn
    }

    /**
     * Closes the connection
     */
    fun disconnect() {
        connection!!.close()
    }

    /**
     * Executes a raw SQL statement.
     *
     * @param sql SQL string to execute.
     */
    fun executeSQL(sql: String) {
        connection!!.createStatement().use { stmt ->
            stmt.execute(sql)
        }
    }

    /**
     * Creates a table if it does not already exist.
     *
     * @param table Table name.
     * @param columns Column definitions.
     * @param additional Additional SQL (constraints, foreign keys, etc).
     */
    fun createTable(
        table: String,
        columns: String,
        additional: String,
    ) {
        if (table.isBlank()) return

        val sql =
            """
            CREATE TABLE IF NOT EXISTS $table (
                $columns${if (additional.isNotBlank()) ",\n$additional" else ""}
            );
            """.trimIndent()

        executeSQL(sql)
    }

    /**
     * Creates indexes for a table.
     *
     * @param table Table name.
     * @param indexes Index definitions.
     */
    fun createIndexes(
        table: String,
        indexes: List<IndexArgs>,
    ) {
        if (table.isBlank()) return

        for (index in indexes) {
            executeSQL(
                "CREATE INDEX IF NOT EXISTS ${index.indexName} ON $table(${index.columnName})",
            )
        }
    }

    /**
     * Initializes all tables in dependency order.
     */
    fun createTables() {
        println("Initialising database")
        initialisedTables = mutableListOf()

        for (dataClass in dataClasses) {
            initialiseTable(dataClass)
        }

        initialisedTables.clear()
        seedAdminAccount()

        dbInitialised = true
        println("Database initilisation completed")
    }

    /**
     * Initializes a single table and its dependencies.
     *
     * @param dataClass Data class describing the table schema.
     */
    fun initialiseTable(dataClass: DataClass<*>) {
        if (initialisedTables.any { it::class == dataClass::class }) return

        println("Initialising ${dataClass.tableName}")

        createTable(
            dataClass.tableName,
            dataClass.tableCreateSQL,
            dataClass.tableAdditionalSQL,
        )

        createIndexes(
            dataClass.tableName,
            dataClass.indexes,
        )

        // WARNING: Circular dependencies during initialization will break :)
        for (requirement in dataClass.requiredTables) {
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

    /**
     * Inserts a row into a table.
     *
     * @param table Table name.
     * @param values Column-value map.
     * @param ignore Whether to ignore conflicts.
     * @return Generated row ID or -1.
     */
    fun insertIntoTable(
        table: String,
        values: Map<Column<*>, Any?>,
        ignore: Boolean = false,
    ): Int {
        if (values.isEmpty()) return -1

        val entries = values.entries.toList()
        val columns = values.keys.joinToString(", ") { it.name }
        val placeholders = List(values.size) { "?" }.joinToString(", ")

        val sql =
            "INSERT ${if (ignore) "OR IGNORE " else ""}INTO $table ($columns) VALUES ($placeholders)"

        var id = -1

        connection!!.prepareStatement(sql).use { stmt ->
            entries.forEachIndexed { index, entry ->
                stmt.setObject(index + 1, entry.value)
            }

            stmt.executeUpdate()

            stmt.generatedKeys.use { keys ->
                if (keys.next()) id = keys.getInt(1)
            }
        }

        return id
    }

    /**
     * Updates rows in a table matching the given WHERE clause.
     *
     * @param table Table name.
     * @param values Values to update.
     * @param whereArgs WHERE clause arguments.
     * @return Number of affected rows.
     */
    fun updateTable(
        table: String,
        values: Map<Column<*>, Any?>,
        whereArgs: WhereArgs,
    ): Int {
        require(values.isNotEmpty()) { "No values where given to update" }

        val entries = values.entries.toList()
        val setClause = entries.joinToString(", ") { "${it.key.name} = ?" }
        val sql = "UPDATE $table SET $setClause WHERE ${whereArgs.whereClause}"

        connection!!.prepareStatement(sql).use { stmt ->
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

    /**
     * Deletes rows from a table using a WHERE clause.
     *
     * @param table Table name.
     * @param whereArgs WHERE clause arguments.
     * @return Number of affected rows.
     */
    fun deleteFromTable(
        table: String,
        whereArgs: WhereArgs,
    ): Int {
        require(whereArgs.whereClause.isNotBlank()) {
            "DELETE requires a WHERE clause"
        }

        val sql = "DELETE FROM $table WHERE ${whereArgs.whereClause}"

        connection!!.prepareStatement(sql).use { stmt ->
            whereArgs.whereArgs.forEachIndexed { i, value ->
                stmt.setObject(i + 1, value)
            }

            return stmt.executeUpdate()
        }
    }

    /**
     * Executes a SELECT query with optional joins and clauses.
     *
     * @return List of raw result rows.
     */
    fun queryTable(
        table: String,
        columns: List<String>,
        multipleJoinArgs: MultipleJoinArgs? = null,
        whereArgs: WhereArgs? = null,
        orderByArgs: OrderByArgs? = null,
        limitArgs: LimitArgs? = null,
        groupByArgs: GroupByArgs? = null,
    ): List<Array<Any?>> {
        var columnStr = columns.joinToString(", ") { "$table.$it" }

        if (multipleJoinArgs != null) {
            multipleJoinArgs.joinArgs.forEach { joinArgs ->
                columnStr += ", " +
                    joinArgs.joinSelectColumns.joinToString(", ") {
                        "${joinArgs.rightTableJoin}.$it"
                    }
            }
        }

        val sql =
            buildString {
                append("SELECT $columnStr FROM $table")

                if (multipleJoinArgs != null) {
                    multipleJoinArgs.joinArgs.forEach { joinArgs ->
                        append(
                            " ${joinArgs.joinType} JOIN ${joinArgs.rightTableJoin} ON " +
                                "${if (joinArgs.leftTableJoin == null) table else joinArgs.leftTableJoin}." +
                                "${joinArgs.leftTableJoinColumn} = " +
                                "${joinArgs.rightTableJoin}.${joinArgs.rightTableJoinColumn}",
                        )
                    }
                }

                if (whereArgs != null) append(" WHERE (${whereArgs.whereClause})")

                if (groupByArgs != null) {
                    append(" GROUP BY ${groupByArgs.groupClause}")
                    if (groupByArgs.havingArgs != null) {
                        append(" HAVING ${groupByArgs.havingArgs.havingClause}")
                    }
                }

                if (orderByArgs != null) {
                    append(
                        " ORDER BY ${
                            orderByArgs.orderArgs.joinToString(", ") {
                                "${it.orderColumn} ${if (it.ascending) "ASC" else "DESC"}"
                            }
                        }",
                    )
                }

                if (limitArgs != null) append(" LIMIT ${limitArgs.limitAmount}")
            }

        var totalSize = columns.size
        if (multipleJoinArgs != null) {
            multipleJoinArgs.joinArgs.forEach {
                totalSize += it.joinSelectColumns.size
            }
        }

        connection!!.prepareStatement(sql).use { stmt ->
            var objectIndex = 1

            if (whereArgs != null) {
                whereArgs.whereArgs.forEach {
                    stmt.setObject(objectIndex++, it)
                }
            }

            if (groupByArgs?.havingArgs != null) {
                groupByArgs.havingArgs.havingArgs.forEach {
                    stmt.setObject(objectIndex++, it)
                }
            }

            stmt.executeQuery().use { rs ->
                val results = mutableListOf<Array<Any?>>()

                while (rs.next()) {
                    results.add(
                        Array(totalSize) { index ->
                            rs.getObject(index + 1)
                        },
                    )
                }

                return results
            }
        }
    }

    /**
     * Updates a row by ID.
     *
     * @param table Table name.
     * @param id Row ID.
     * @param values Values to update.
     */
    fun updateInDatabase(
        table: String,
        id: Int,
        values: Map<Column<*>, Any?>,
    ) {
        if (values.isEmpty()) return

        val setClause = values.keys.joinToString(", ") { "${it.name} = ?" }
        val sql = "UPDATE $table SET $setClause WHERE id = ?"

        connection!!.prepareStatement(sql).use { stmt ->
            values.values.forEachIndexed { index, value ->
                stmt.setObject(index + 1, value)
            }

            stmt.setObject(values.size + 1, id)
            stmt.executeUpdate()
        }
    }

    /**
     * Seeds the admin account using values from the admin JSON file.
     */
    fun seedAdminAccount() {
        val adminJSONFile = File(adminJSONFilePath)
        if (adminJSONFile.exists() == false) {
            println("WARNING: COULD NOT FIND ADMIN JSON FILE")
            return
        }

        val adminJSONObject =
            JSONObject(adminJSONFile.readText(Charsets.UTF_8).trimIndent())

        val adminEmail = adminJSONObject.getString("email")
        val adminPassword = adminJSONObject.getString("password")

        val existing =
            LoginData.queryDatabase(
                whereArgs =
                    WhereArgs(
                        "${LoginColumns.EMAIL.name} = ?",
                        listOf(adminEmail),
                    ),
            )

        if (existing.isNotEmpty()) {
            println("Admin already exists")
            return
        }

        println("Seeding admin account")

        val passwordHash = BCrypt.hashpw(adminPassword, BCrypt.gensalt())

        val loginId =
            LoginData(
                email = adminEmail,
                passwordHash = passwordHash,
            ).insertIntoDatabase()

        UserData(
            firstName = "Admin",
            lastName = "User",
            verifiedAccount = true,
            loyaltyPoints = 0,
            loginId = loginId,
        ).insertIntoDatabase()

        AdminData(loginId = loginId).insertIntoDatabase()
    }

    /**
     * Prints the contents of all tables.
     */
    fun debugDatabase() {
        for (dataClass in dataClasses) {
            dataClass.debugTable()
        }
    }
}
