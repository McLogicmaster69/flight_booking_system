package data

data class QueryResult <T : DataClass<T>> (
    val dataClass : T,
    val rawValues : List<ColumnValue>
) {
    fun getColumn (table : String, column : String) : ColumnValue? {
        for (v in rawValues) {
            if (v.table == table && v.column == column) {
                return v
            }
        }

        return null
    }
}
