package data

data class Column<T>(
    val name: String,
    val sqlType : String
) {
    override fun toString() : String = name
}
