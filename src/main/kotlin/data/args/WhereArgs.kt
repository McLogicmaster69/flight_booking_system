package data

/**
 * Defines the `WHERE` argument in a query.
 */
data class WhereArgs(
    /**
     * The clause used in the statement. `WHERE <clause>`.
     */
    val whereClause: String,

    /**
     * 
     */
    val whereArgs: List<Any?>,
)
