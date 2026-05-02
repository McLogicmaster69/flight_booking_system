package data

import java.time.LocalDate
import java.time.LocalTime
import java.security.SecureRandom
import java.util.Base64

abstract class DataClass<T : DataClass<T>> ( open val id : Int = 0 ) {
    abstract val tableName: String
    abstract val tableColumns : List<Column<*>>
    
    open val tableAdditionalSQL : String = ""
    open val initialRows : List<T> = emptyList<T>()
    open val requiredTables : List<DataClass<*>> = emptyList<DataClass<*>>()

    private val random = SecureRandom()

    val tableCreateSQL : String
        get() = tableColumns.joinToString(", ") { "${it.name} ${it.sqlType}" }

    protected abstract fun mapDataToColumns() : Map<Column<*>, Any?>

    protected abstract fun mapRowToData(row : Array<Any?>) : T

    abstract fun debugData()

    protected fun <K> castRowElement(row : Array<Any?>, column : Column<K>) : K {
        val index = tableColumns.indexOf(column)
        require(index >= 0) { "Column ${column.name} not found" }
        @Suppress("UNCHECKED_CAST")
        return row[index] as K
    }

    protected fun castDateRowElement(row : Array<Any?>, column : Column<*>) : LocalDate {
        val index = tableColumns.indexOf(column)
        require(index >= 0) { "Column ${column.name} not found" }
        @Suppress("UNCHECKED_CAST")

        return (row[index] as? String)
            ?.let { LocalDate.parse(it) }
            ?: LocalDate.parse("1970-01-01")
    }

    protected fun castTimeRowElement(row : Array<Any?>, column : Column<*>) : LocalTime {
        val index = tableColumns.indexOf(column)
        require(index >= 0) { "Column ${column.name} not found" }
        @Suppress("UNCHECKED_CAST")

        return (row[index] as? String)
            ?.let { LocalTime.parse(it) }
            ?: LocalTime.parse("00:00")
    }

    open fun initTable() {
    }

    fun mapRawRows(
        tables : List<String>,
        columns : List<String>,
        row : Array<Any?>
    ) : List<ColumnValue> =
        row.indices.map { i ->
            ColumnValue(
                tables[i],
                columns[i],
                row[i]
            )
        }

    fun update() : Int = DatabaseManager.updateTable(tableName, mapDataToColumns(), WhereArgs("id = ?", listOf(id)))

    fun updateTable(
        values : Map<Column<*>, Any?>,
        whereArgs : WhereArgs
    ) : Int = DatabaseManager.updateTable(tableName, values, whereArgs)

    fun delete() : Int = DatabaseManager.deleteFromTable(tableName, WhereArgs("id = ?", listOf(id)))

    fun queryDatabase (
        joinArgs : JoinArgs? = null,
        whereArgs : WhereArgs? = null
    ) : List<QueryResult<T>> {

        val columnNames = tableColumns.map { it.name }
        val rows = DatabaseManager.queryTable(
            tableName,
            columnNames,
            joinArgs,
            whereArgs)

        val columns : MutableList<String> = columnNames.toMutableList()
        val tables : MutableList<String> = MutableList(columns.size) { tableName }

        if (joinArgs != null) {
            columns.addAll(joinArgs.joinSelectColumns)
            tables.addAll(List(joinArgs.joinSelectColumns.size) { joinArgs.joinTable })
        }

        return rows.map { QueryResult<T>(mapRowToData(it), mapRawRows(tables.toList(), columns.toList(), it)) }
    }

    fun debugTable () {
        println("Printing ${tableName}")
        println(tableColumns.joinToString(", ") { it.name })
        queryDatabase().forEach { it.dataClass.debugData() }
    }

    fun insertIntoDatabase(ignore : Boolean = false) : Int {
        val values = mapDataToColumns()
        return DatabaseManager.insertIntoTable(tableName, values, ignore)
    }

    fun generateToken(length : Int = 16) : String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length).map { allowedChars.random() }.joinToString("")
    }

    fun generateSecureToken() : String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
