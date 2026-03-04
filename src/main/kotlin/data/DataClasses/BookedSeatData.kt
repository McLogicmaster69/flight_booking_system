package data

object BookedSeatColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val SEAT_ID = Column<Int>("seat_id", "INTEGER NOT NULL REFERENCES seats(id)")
    val BOOKING_ID = Column<Int>("booking_id", "INTEGER NOT NULL REFERENCES bookings(id)")

    val ALL = listOf(ID, SEAT_ID, BOOKING_ID)
}

data class BookedSeatData(

    override val id: Int = 0,
    var seatId : Int = 0,
    var bookingId : Int = 0

) : DataClass<BookedSeatData>(id) {

    override val tableName = "booked_seats"
    override val tableColumns = BookedSeatColumns.ALL

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            BookedSeatColumns.SEAT_ID to seatId,
            BookedSeatColumns.BOOKING_ID to bookingId
        )

    override fun mapRowToData(row : Array<Any?>) : BookedSeatData =
        BookedSeatData(
            id = castRowElement(row, BookedSeatColumns.ID),
            seatId = castRowElement(row, BookedSeatColumns.SEAT_ID),
            bookingId = castRowElement(row, BookedSeatColumns.BOOKING_ID)
        )

    override fun debugData() {
        println("Booked seat data: (\"$id\", \"$seatId\", \"$bookingId\")")
    }

    companion object {
        val EMPTY : BookedSeatData
            get() = BookedSeatData()

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null) : List<QueryResult<BookedSeatData>> {
            return EMPTY.queryDatabase(joinArgs, whereArgs)
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)

        fun delete(id : Int) : Int {
            return BookedSeatData(id = id).delete()
        }
    }
}
