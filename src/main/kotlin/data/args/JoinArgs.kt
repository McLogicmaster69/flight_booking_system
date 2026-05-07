package data

/**
 * Defines how a query joins tables.
 */
data class JoinArgs(
    /**
     * The type of join.
     *
     * For example `INNER`.
     */
    val joinType: String,
    /**
     * The right table used in the join.
     */
    val rightTableJoin: String,
    /**
     * The column being used in the left table.
     */
    val leftTableJoinColumn: String,
    /**
     * The column being used in the right table.
     */
    val rightTableJoinColumn: String,
    /**
     * The columns being selected in the query from the right table.
     */
    val joinSelectColumns: List<String> = emptyList(),
    /**
     * OPTIONAL: The left table used in the join.
     *
     * If left null, the table being queried will be used.
     */
    val leftTableJoin: String? = null,
)
