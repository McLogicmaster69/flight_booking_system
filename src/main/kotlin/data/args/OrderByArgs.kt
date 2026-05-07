package data

/**
 * Defines how a query is ordered.
 */
data class OrderByArgs(
    /**
     * A list of order args for each column.
     */
    val orderArgs: List<OrderArgs>,
)
