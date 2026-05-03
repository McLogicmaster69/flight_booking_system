package data

data class WhereArgs(
    val whereClause : String,
    val whereArgs : List<Any?>
)