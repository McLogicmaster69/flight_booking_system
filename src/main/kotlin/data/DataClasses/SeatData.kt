package data

object SeatColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val FLIGHT_ID = Column<Int>("flight_id", "INTEGER NOT NULL REFERENCES flights(id)")
    val CLASS_ID = Column<Int>("class_id", "INTEGER NOT NULL REFERENCES classes(id)")
    val TYPE_ID = Column<Int>("type_id", "INTEGER NOT NULL REFERENCES ticket_types(id)")
    val NUMBER = Column<Int>("number", "INTEGER NOT NULL")
    val PRICE = Column<Float>("price", "INTEGER NOT NULL")

    val ALL = listOf(ID, FLIGHT_ID, CLASS_ID, TYPE_ID, NUMBER, PRICE)
}

data class SeatData(

    override val id: Int = 0,
    var flightId : Int = 0,
    var classId : Int = 0,
    var typeId : Int = 0,
    var number : Int = 0,
    var price : Float = 0f,

) : DataClass<SeatData>(id) {

    override val tableName = "seats"
    override val tableColumns = SeatColumns.ALL

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            SeatColumns.FLIGHT_ID to flightId,
            SeatColumns.CLASS_ID to classId,
            SeatColumns.TYPE_ID to typeId,
            SeatColumns.NUMBER to number,
            SeatColumns.PRICE to price
        )

    override fun mapRowToData(row : Array<Any?>) : SeatData =
        SeatData(
            id = castRowElement(row, SeatColumns.ID),
            flightId = castRowElement(row, SeatColumns.FLIGHT_ID),
            classId = castRowElement(row, SeatColumns.CLASS_ID),
            typeId = castRowElement(row, SeatColumns.TYPE_ID),
            number = castRowElement(row, SeatColumns.NUMBER),
            price = castRowElement(row, SeatColumns.PRICE)
        )

    override fun debugData() {
        println("Seat data: (\"$id\", \"$flightId\", \"$classId\", \"$typeId\", \"$number\", \"$price\")")
    }

    companion object {
        val EMPTY : SeatData
            get() = SeatData()

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null) : List<QueryResult<SeatData>> {
            return EMPTY.queryDatabase(joinArgs, whereArgs)
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)

        fun delete(id : Int) : Int {
            return SeatData(id = id).delete()
        }
    }
}
