package data

import java.time.LocalDate
import java.time.LocalTime
import java.security.SecureRandom
import java.util.Base64

/**
 * Base abstract class used for mapping Kotlin objects to database tables.
 *
 * @param T The subclass.
 * @property id Unique identifier for a row.
 */
abstract class DataClass<T : DataClass<T>>(
    open val id: Int = 0,
) {
    /**
     * The name of the table being created and accessed.
     */
    abstract val tableName: String

    /**
     * The columns that exist within the table.
     */
    abstract val tableColumns: List<Column<*>>

    /**
     * Used to specify additional SQL that will be used when creating the table.
     */
    open val tableAdditionalSQL: String = ""

    /**
     * Used to specify the initial rows in the table.
     */
    open val initialRows: List<T> = emptyList<T>()

    /**
     * Used to specify which tables are required to be created before the initial rows are added.
     */
    open val requiredTables: List<DataClass<*>> = emptyList<DataClass<*>>()

    /**
     * Used to specify any indexes that exist within a table. Used to speed up search queries.
     */
    open val indexes: List<IndexArgs> = emptyList()

    private val random = SecureRandom()

    /**
     * SQL string used to create the table columns.
     */
    val tableCreateSQL: String
        get() = tableColumns.joinToString(", ") { "${it.name} ${it.sqlType}" }

    /**
     * Maps data stored in variables in the data class to columns stored in the table.
     *
     * @return Map of columns to values.
     */
    protected abstract fun mapDataToColumns(): Map<Column<*>, Any?>

    /**
     * Maps data retrieved from columns in a table to a variable.
     *
     * @param row Raw database row.
     * @return Parsed object instance.
     */
    protected abstract fun mapRowToData(row: Array<Any?>): T

    /**
     * Should be used to display to the console the state of the class.
     */
    abstract fun debugData()

    /**
     * Casts an element from a row using a column.
     *
     * @param row Database row.
     * @param column Target column.
     * @return Casted value.
     * @throws IllegalArgumentException If the column is not found.
     */
    protected fun <K> castRowElement(
        row: Array<Any?>,
        column: Column<K>,
    ): K {
        val index = tableColumns.indexOf(column)
        require(index >= 0) { "Column ${column.name} not found" }

        @Suppress("UNCHECKED_CAST")
        return row[index] as K
    }

    /**
     * Casts a date from a row using a column.
     *
     * Returns `1970-01-01` if parsing fails.
     *
     * @param row Database row.
     * @param column Target column.
     * @return Parsed [LocalDate].
     */
    protected fun castDateRowElement(
        row: Array<Any?>,
        column: Column<*>,
    ): LocalDate {
        val index = tableColumns.indexOf(column)
        require(index >= 0) { "Column ${column.name} not found" }

        @Suppress("UNCHECKED_CAST")

        return (row[index] as? String)
            ?.let { LocalDate.parse(it) }
            ?: LocalDate.parse("1970-01-01")
    }

    /**
     * Casts a time from a row using a column.
     *
     * Returns `00:00` if parsing fails.
     *
     * @param row Database row.
     * @param column Target column.
     * @return Parsed [LocalTime].
     */
    protected fun castTimeRowElement(
        row: Array<Any?>,
        column: Column<*>,
    ): LocalTime {
        val index = tableColumns.indexOf(column)
        require(index >= 0) { "Column ${column.name} not found" }

        @Suppress("UNCHECKED_CAST")

        return (row[index] as? String)
            ?.let { LocalTime.parse(it) }
            ?: LocalTime.parse("00:00")
    }

    /**
     * Initializes the table.
     *
     * Can be overridden by subclasses for custom setup logic.
     */
    open fun initTable() {
    }

    /**
     * Maps raw database rows into `ColumnValue` objects.
     *
     * @param tables Table names.
     * @param columns Column names.
     * @param row Raw database row.
     * @return List of mapped column values.
     */
    fun mapRawRows(
        tables: List<String>,
        columns: List<String>,
        row: Array<Any?>,
    ): List<ColumnValue> =
        row.indices.map { i ->
            ColumnValue(
                tables[i],
                columns[i],
                row[i],
            )
        }

    /**
     * Updates the current row in the database.
     *
     * @return Return code of the update.
     */
    fun update(): Int =
        DatabaseManager.updateTable(
            tableName,
            mapDataToColumns(),
            WhereArgs("id = ?", listOf(id)),
        )

    /**
     * Updates rows in the table matching the given condition.
     *
     * @param values Values to update.
     * @param whereArgs WHERE clause arguments.
     * @return Return code of the update.
     */
    fun updateTable(
        values: Map<Column<*>, Any?>,
        whereArgs: WhereArgs,
    ): Int = DatabaseManager.updateTable(tableName, values, whereArgs)

    /**
     * Deletes the current row from the database.
     *
     * @return Return code of the update.
     */
    fun delete(): Int =
        DatabaseManager.deleteFromTable(
            tableName,
            WhereArgs("id = ?", listOf(id)),
        )

    /**
     * Queries the database.
     *
     * @param multipleJoinArgs Optional JOIN arguments.
     * @param whereArgs Optional WHERE clause.
     * @param orderByArgs Optional ORDER BY clause.
     * @param limitArgs Optional LIMIT clause.
     * @param groupByArgs Optional GROUP BY clause.
     * @return List of query results.
     */
    fun queryDatabase(
        multipleJoinArgs: MultipleJoinArgs? = null,
        whereArgs: WhereArgs? = null,
        orderByArgs: OrderByArgs? = null,
        limitArgs: LimitArgs? = null,
        groupByArgs: GroupByArgs? = null,
    ): List<QueryResult<T>> {
        val columnNames = tableColumns.map { it.name }

        val rows =
            DatabaseManager.queryTable(
                tableName,
                columnNames,
                multipleJoinArgs,
                whereArgs,
                orderByArgs,
                limitArgs,
                groupByArgs,
            )

        val columns: MutableList<String> = columnNames.toMutableList()
        val tables: MutableList<String> = MutableList(columns.size) { tableName }

        if (multipleJoinArgs != null) {
            multipleJoinArgs.joinArgs.forEach { joinArgs ->
                columns.addAll(joinArgs.joinSelectColumns)
                tables.addAll(
                    List(joinArgs.joinSelectColumns.size) {
                        joinArgs.rightTableJoin
                    },
                )
            }
        }

        return rows.map {
            QueryResult<T>(
                mapRowToData(it),
                mapRawRows(tables.toList(), columns.toList(), it),
            )
        }
    }

    /**
     * Prints the contents of the table.
     */
    fun debugTable() {
        println("Printing $tableName")
        println(tableColumns.joinToString(", ") { it.name })

        queryDatabase().forEach {
            it.dataClass.debugData()
        }
    }

    /**
     * Inserts the current row into the table.
     *
     * @param ignore Whether conflicts should be ignored.
     * @return Inserted row ID.
     */
    fun insertIntoDatabase(ignore: Boolean = false): Int {
        val values = mapDataToColumns()
        return DatabaseManager.insertIntoTable(tableName, values, ignore)
    }

    /**
     * Generates a random alphanumeric token.
     *
     * @param length Length of the token.
     * @return Generated token.
     */
    fun generateToken(length: Int = 16): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')

        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    /**
     * Generates a cryptographically secure token.
     *
     * @return Secure Base64 URL-safe token.
     */
    fun generateSecureToken(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)

        return Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString(bytes)
    }
}
