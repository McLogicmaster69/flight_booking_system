package data

/**
 * Store a value retrieved from a column in a row in a table.
 */
data class ColumnValue(
    /**
     * The table the value came from.
     */
    val table: String,

    /**
     * The column the value came from.
     */
    val column: String,

    /**
     * The value.
     */
    val columnVal: Any?,
)
