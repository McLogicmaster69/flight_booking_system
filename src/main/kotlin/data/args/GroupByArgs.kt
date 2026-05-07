package data

data class GroupByArgs(
    val groupClause: String,
    val havingArgs: HavingArgs? = null,
)
