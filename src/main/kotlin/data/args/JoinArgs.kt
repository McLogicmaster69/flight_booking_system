package data

data class JoinArgs (
    val joinType : String,
    val rightTableJoin : String,
    val leftTableJoinColumn : String,
    val rightTableJoinColumn : String,
    val joinSelectColumns : List<String> = emptyList(),
)