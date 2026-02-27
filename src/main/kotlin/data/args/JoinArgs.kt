package data

data class JoinArgs (
    val joinType : String,
    val joinTable : String,
    val joinTable1Column : String,
    val joinTable2Column : String,
    val joinSelectColumns : List<String> = emptyList(),
)