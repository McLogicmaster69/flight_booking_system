package data

/**
 * Defines how a query is grouped.
 */
data class GroupByArgs(
    /**
     * The clause used in the statement. `GROUP BY <clause>`.
     */
    val groupClause: String,
    /**
     * Optional HAVING args.
     */
    val havingArgs: HavingArgs? = null,
)
