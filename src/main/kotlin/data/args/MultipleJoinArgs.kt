package data

/**
 * Defines multiple `JOIN` arguments in a query.
 */
data class MultipleJoinArgs(
    /**
     * A list of `JOIN` arguments used in the query.
     */
    val joinArgs: List<JoinArgs>,
)
