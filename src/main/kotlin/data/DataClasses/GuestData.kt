package data

object GuestColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val EMAIL = Column<String>("email", "VARCHAR NOT NULL")

    val ALL = listOf(ID, EMAIL)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class GuestData(

    override val id: Int = 0,
    var email: String = ""

) : DataClass<GuestData>(id) {

    override val tableName = "guests"
    override val tableColumns = GuestColumns.ALL

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            GuestColumns.EMAIL to email
        )

    override fun mapRowToData(row : Array<Any?>) : GuestData =
        GuestData(
            id = castRowElement(row, GuestColumns.ID),
            email = castRowElement(row, GuestColumns.EMAIL)
        )

    override fun debugData() {
        println("Guest data: (\"$id\", \"$email\")")
    }

    companion object {
        val EMPTY : GuestData
            get() = GuestData()

        fun queryDatabase (
            multipleJoinArgs : MultipleJoinArgs? = null,
            whereArgs : WhereArgs? = null,
            orderByArgs : OrderByArgs? = null,
            limitArgs : LimitArgs? = null,
            groupByArgs : GroupByArgs? = null   
        ) : List<QueryResult<GuestData>> {
            return EMPTY.queryDatabase(multipleJoinArgs, whereArgs, orderByArgs, limitArgs, groupByArgs)
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)

        fun delete(id : Int) : Int {
            return GuestData(id = id).delete()
        }
    }
}
