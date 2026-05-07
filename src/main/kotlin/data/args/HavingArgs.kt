package data

/**
 * Defines the HAVING in a GROUP BY.
 */
data class HavingArgs(
    /**
     * The clause used in the statement. `HAVING <clause>`.
     */
    val havingClause: String,
    /**
     * Arguments used to replace any ? in the clause.
     */
    val havingArgs: List<Any?>,
)
