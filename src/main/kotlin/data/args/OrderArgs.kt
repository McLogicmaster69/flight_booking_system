package data

/**
 * A column used to order a query.
 */
data class OrderArgs(
    /**
     * The column being ordered by.
     */
    val orderColumn: String,

    /**
     * If the order is ascending.
     */
    val ascending: Boolean = true,
)
