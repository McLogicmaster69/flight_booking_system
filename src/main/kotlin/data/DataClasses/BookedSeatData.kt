package data

object BookedSeatColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val SEAT_ID = Column<Int>("seat_id", "INTEGER NOT NULL REFERENCES ${SeatData.EMPTY.tableName}(id)")
    val BOOKING_ID = Column<Int>("booking_id", "INTEGER NOT NULL REFERENCES ${BookingData.EMPTY.tableName}(id)")

    val ALL = listOf(ID, SEAT_ID, BOOKING_ID)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class BookedSeatData(

    override val id: Int = 0,
    var seatId : Int = 0,
    var bookingId : Int = 0

) : DataClass<BookedSeatData>(id) {

    override val tableName = "booked_seats"
    override val tableColumns = BookedSeatColumns.ALL

    override val indexes : List<IndexArgs> = listOf(
        IndexArgs("inx_booked_seats_seat_id", BookedSeatColumns.SEAT_ID.name),
        IndexArgs("inx_booked_seats_booking_id", BookedSeatColumns.BOOKING_ID.name)
    )

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
            multipleJoinArgs : MultipleJoinArgs? = null,
            whereArgs : WhereArgs? = null,
            orderByArgs : OrderByArgs? = null,
            limitArgs : LimitArgs? = null,
            groupByArgs : GroupByArgs? = null   
        ) : List<QueryResult<BookedSeatData>> {
            return EMPTY.queryDatabase(multipleJoinArgs, whereArgs, orderByArgs, limitArgs, groupByArgs)
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
