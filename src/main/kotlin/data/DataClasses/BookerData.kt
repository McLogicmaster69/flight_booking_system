package data

object BookerColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val USER_ID = Column<Int?>("user_id", "INTEGER REFERENCES users(id)")
    val GUEST_ID = Column<Int?>("guest_id", "INTEGER REFERENCES guests(id)")

    val ALL = listOf(ID, USER_ID, GUEST_ID)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class BookerData(

    override val id: Int = 0,
    var userId : Int? = 0,
    var guestId : Int? = 0

) : DataClass<BookerData>(id) {

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

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null) : List<QueryResult<BookerData>> {
            return EMPTY.queryDatabase(joinArgs, whereArgs)
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)

        fun delete(id : Int) : Int {
            return BookerData(id = id).delete()
        }
    }
}
