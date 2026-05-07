package data

/**
 * Defines the `LIMIT` in a query.
 */
data class LimitArgs(
    /**
     * The number of rows returned.
     */
    val limitAmount: Int,
)
