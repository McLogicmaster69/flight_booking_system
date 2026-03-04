package data

object RemainingSeatColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val SEAT_ID = Column<Int>("seat_id", "INTEGER NOT NULL REFERENCES seats(id)")

    val ALL = listOf(ID, SEAT_ID)
}

data class RemainingSeatData(

    override val id: Int = 0,
    var seatId : Int = 0,

) : DataClass<RemainingSeatData>(id) {

    override val tableName = "remaining_seats"
    override val tableColumns = RemainingSeatColumns.ALL

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            RemainingSeatColumns.SEAT_ID to seatId
        )

    override fun mapRowToData(row : Array<Any?>) : RemainingSeatData =
        RemainingSeatData(
            id = castRowElement(row, RemainingSeatColumns.ID),
            seatId = castRowElement(row, RemainingSeatColumns.SEAT_ID)
        )

    override fun debugData() {
        println("Remaining seat data: (\"$id\", \"$seatId\")")
    }

    companion object {
        val EMPTY : RemainingSeatData
            get() = RemainingSeatData()

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null) : List<QueryResult<RemainingSeatData>> {
            return EMPTY.queryDatabase(joinArgs, whereArgs)
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)

        fun delete(id : Int) : Int {
            return RemainingSeatData(id = id).delete()
        }
    }
}
