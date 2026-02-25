package data

object BookingColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val BOOKER_ID = Column<Int>("booker_id", "INTEGER NOT NULL REFERENCES bookers(id)")
    val FLIGHT_ID = Column<Int>("flight_id", "INTEGER NOT NULL")
    val PASSPORT_NUMBER = Column<Int?>("passport_number", "INTEGER")
    val LASTNAME = Column<String?>("lastname", "STRING")

    val ALL = listOf(ID, BOOKER_ID, FLIGHT_ID, PASSPORT_NUMBER, LASTNAME)
}

data class BookingData(

    val id : Int = 0,
    var bookerId : Int = 0,
    var flightId : Int = 0,
    var passportNumber : Int? = null,
    var lastname : String? = null

) : DataClass<BookingData>() {

    override val tableName = "bookings"
    override val tableColumns = BookingColumns.ALL

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            BookingColumns.BOOKER_ID to bookerId,
            BookingColumns.FLIGHT_ID to flightId,
            BookingColumns.PASSPORT_NUMBER to passportNumber,
            BookingColumns.LASTNAME to lastname
        )

    override fun mapRowToData(row : Array<Any?>) : BookingData =
        BookingData(
            id = castRowElement(row, BookingColumns.ID),
            bookerId = castRowElement(row, BookingColumns.BOOKER_ID),
            flightId = castRowElement(row, BookingColumns.FLIGHT_ID),
            passportNumber = castRowElement(row, BookingColumns.PASSPORT_NUMBER),
            lastname = castRowElement(row, BookingColumns.LASTNAME),
        )

    override fun debugData() {
        println("Booking data: (\"$id\", \"$bookerId\", \"$flightId\", \"$passportNumber\", \"$lastname\")")
    }

    companion object {
        val EMPTY : BookingData
            get() = BookingData()

        fun queryDatabase (whereClause : String? = null, whereArgs : List<Any?> = emptyList()) : List<BookingData> {
            return EMPTY.queryDatabase(whereClause, whereArgs)
        }
    }
}
