package data

import java.time.LocalDateTime

object FlightColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val ROUTE_ID = Column<Int>("route_id", "INTEGER NOT NULL REFERENCES routes(id)")
    val DATE_TIME = Column<String>("date_time", "STRING NOT NULL")

    val ALL = listOf(ID, ROUTE_ID, DATE_TIME)
}

data class FlightData(

    val id: Int = 0,
    var routeId : Int = 0,
    var dateTime : LocalDateTime = LocalDateTime.now()

) : DataClass<FlightData>() {

    override val tableName = "flights"
    override val tableColumns = FlightColumns.ALL

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            FlightColumns.ROUTE_ID to routeId,
            FlightColumns.DATE_TIME to dateTime.toString()
        )

    override fun mapRowToData(row : Array<Any?>) : FlightData =
        FlightData(
            id = castRowElement(row, FlightColumns.ID),
            routeId = castRowElement(row, FlightColumns.ROUTE_ID),
            dateTime = castDateRowElement(row, FlightColumns.DATE_TIME)
        )

    override fun debugData() {
        println("Flight data: (\"$id\", \"$routeId\", \"$dateTime\")")
    }

    companion object {
        val EMPTY : FlightData
            get() = FlightData()

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null) : List<QueryResult<FlightData>> {
            return EMPTY.queryDatabase(joinArgs, whereArgs)
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)
    }
}
