package data

/**
 * Used to create an index for a table.
 */
data class IndexArgs(
    /**
     * Name of the index.
     */
    val indexName: String,
    /**
     * The name of the column being indexed. Use brackets an index for multiple columns.
     *
     * For example `(firstname, lastname)`.
     */
    val columnName: String,
)
