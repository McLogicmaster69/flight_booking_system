package data

abstract class DataClass<T : DataClass<T>> {
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

    fun queryDatabase (whereClause : String? = null, whereArgs : List<Any?> = emptyList()) : List<T> {
        val columnNames = tableColumns.map { it.name }
        val rows = DatabaseManager.queryTable(tableName, columnNames, whereClause, whereArgs)
        return rows.map { mapRowToData(it) }
    }

    fun insertIntoDatabase() {
        val values = mapDataToColumns()
        DatabaseManager.insertIntoTable(tableName, values)
    }
}
