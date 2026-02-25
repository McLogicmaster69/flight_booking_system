package data

object BookerColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY")
    val USER_ID = Column<Int?>("user_id", "INTEGER")
    val GUEST_ID = Column<Int?>("guest_id", "INTEGER")

    val ALL = listOf(ID, USER_ID, GUEST_ID)
}

data class BookerData(

    val id: Int = 0,
    var userId : Int? = 0,
    var guestId : Int? = 0

) : DataClass<BookerData>() {

    override val tableName = "bookers"
    override val tableColumns = BookerColumns.ALL

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            BookerColumns.USER_ID to userId,
            BookerColumns.GUEST_ID to guestId
        )

    override fun mapRowToData(row : Array<Any?>) : BookerData =
        BookerData(
            id = castRowElement(row, BookerColumns.ID),
            userId = castRowElement(row, BookerColumns.USER_ID),
            guestId = castRowElement(row, BookerColumns.GUEST_ID)
        )

    override fun debugData() {
        println("Booker data: (\"$id\", \"$userId\", \"$guestId\")")
    }

    companion object {
        val EMPTY : BookerData
            get() = BookerData()

        fun queryDatabase (whereClause : String? = null, whereArgs : List<Any?> = emptyList()) : List<BookerData> {
            return EMPTY.queryDatabase(whereClause, whereArgs)
        }
    }
}
