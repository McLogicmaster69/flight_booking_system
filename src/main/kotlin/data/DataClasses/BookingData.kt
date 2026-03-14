package data

object BookingColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val BOOKER_ID = Column<Int>("booker_id", "INTEGER NOT NULL REFERENCES bookers(id)")
    val PASSPORT_NUMBER = Column<Int?>("passport_number", "INTEGER")
    val LASTNAME = Column<String?>("lastname", "STRING")
    val BOOKING_REFERENCE = Column<String>("booking_reference", "STRING NOT NULL")

    val ALL = listOf(ID, BOOKER_ID, PASSPORT_NUMBER, LASTNAME, BOOKING_REFERENCE)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class BookingData(

    override val id : Int = 0,
    var bookerId : Int = 0,
    var passportNumber : Int? = null,
    var lastname : String? = null,
    var bookingReference : String = ""

) : DataClass<BookingData>(id) {

    override val tableName = "bookings"
    override val tableColumns = BookingColumns.ALL

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            BookingColumns.BOOKER_ID to bookerId,
            BookingColumns.PASSPORT_NUMBER to passportNumber,
            BookingColumns.LASTNAME to lastname,
            BookingColumns.BOOKING_REFERENCE to bookingReference
        )

    override fun mapRowToData(row : Array<Any?>) : BookingData =
        BookingData(
            id = castRowElement(row, BookingColumns.ID),
            bookerId = castRowElement(row, BookingColumns.BOOKER_ID),
            passportNumber = castRowElement(row, BookingColumns.PASSPORT_NUMBER),
            lastname = castRowElement(row, BookingColumns.LASTNAME),
            bookingReference = castRowElement(row, BookingColumns.BOOKING_REFERENCE)
        )

    override fun debugData() {
        println("Booking data: (\"$id\", \"$bookerId\", \"$passportNumber\", \"$lastname\", \"$bookingReference\")")
    }

    companion object {
        val EMPTY : BookingData
            get() = BookingData()

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null) : List<QueryResult<BookingData>> {
            return EMPTY.queryDatabase(joinArgs, whereArgs)
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)

        fun delete(id : Int) : Int {
            return BookingData(id = id).delete()
        }
    }
}
