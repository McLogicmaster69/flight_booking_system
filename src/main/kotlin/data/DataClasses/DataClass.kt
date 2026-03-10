package data

import java.time.LocalDateTime

abstract class DataClass<T : DataClass<T>> ( open val id : Int = 0 ) {
    abstract val tableName: String
    abstract val tableColumns : List<Column<*>>

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

    protected fun castDateRowElement(row : Array<Any?>, column : Column<*>) : LocalDateTime {
        val index = tableColumns.indexOf(column)
        require(index >= 0) { "Column ${column.name} not found" }
        @Suppress("UNCHECKED_CAST")
        return LocalDateTime.parse(row[index] as String)
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

    fun insertIntoDatabase() : Int {
        val values = mapDataToColumns()
        return DatabaseManager.insertIntoTable(tableName, values)
    }
}
