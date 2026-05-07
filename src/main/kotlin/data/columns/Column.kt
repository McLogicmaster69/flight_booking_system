package data

/**
 * Defines a column within a table.
 */
data class Column<T>(
    /**
     * The name of the column.
     */
    val name: String,

    /**
     * Information used to create the column, such as type and any additional properties.
     */
    val sqlType: String,
) {
    override fun toString(): String = name
}
