package data

object FlightSearchFlightColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val FLIGHT_SEARCH_ID =
        Column<Int>("flight_search_id", "INTEGER NOT NULL REFERENCES ${FlightSearchData.EMPTY.tableName}(id)")
    val FLIGHT_ID = Column<Int>("flight_id", "INTEGER NOT NULL REFERENCES ${FlightData.EMPTY.tableName}(id)")
    val POSITION = Column<Int>("position", "INTEGER NOT NULL")

    val ALL = listOf(ID, FLIGHT_SEARCH_ID, FLIGHT_ID, POSITION)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class FlightSearchFlightData(
    override val id: Int = 0,
    var flightSearchId: Int = 0,
    var flightId: Int = 0,
    var position: Int = 0,
) : DataClass<FlightSearchFlightData>(id) {
    override val tableName = "flight_search_flights"
    override val tableColumns = FlightSearchFlightColumns.ALL

    override val indexes: List<IndexArgs> =
        listOf(
            IndexArgs("inx_flight_searches_flights_flight_search_id", FlightSearchFlightColumns.FLIGHT_SEARCH_ID.name),
            IndexArgs("inx_flight_searches_flights_flight_id", FlightSearchFlightColumns.FLIGHT_ID.name),
        )

    override fun mapDataToColumns(): Map<Column<*>, Any?> =
        mapOf(
            FlightSearchFlightColumns.FLIGHT_SEARCH_ID to flightSearchId,
            FlightSearchFlightColumns.FLIGHT_ID to flightId,
            FlightSearchFlightColumns.POSITION to position,
        )

    override fun mapRowToData(row: Array<Any?>): FlightSearchFlightData =
        FlightSearchFlightData(
            id = castRowElement(row, FlightSearchFlightColumns.ID),
            flightSearchId = castRowElement(row, FlightSearchFlightColumns.FLIGHT_SEARCH_ID),
            flightId = castRowElement(row, FlightSearchFlightColumns.FLIGHT_ID),
            position = castRowElement(row, FlightSearchFlightColumns.POSITION),
        )

    override fun debugData() {
        println("Flight Search Flight data: (\"$id\", \"$flightSearchId\", \"$flightId\", \"$position\")")
    }

    companion object {
        val EMPTY: FlightSearchFlightData
            get() = FlightSearchFlightData()

        fun queryDatabase(
            multipleJoinArgs: MultipleJoinArgs? = null,
            whereArgs: WhereArgs? = null,
            orderByArgs: OrderByArgs? = null,
            limitArgs: LimitArgs? = null,
            groupByArgs: GroupByArgs? = null,
        ): List<QueryResult<FlightSearchFlightData>> =
            EMPTY.queryDatabase(multipleJoinArgs, whereArgs, orderByArgs, limitArgs, groupByArgs)

        fun updateTable(
            values: Map<Column<*>, Any?>,
            whereArgs: WhereArgs,
        ): Int = EMPTY.updateTable(values, whereArgs)

        fun delete(id: Int): Int = FlightSearchFlightData(id = id).delete()

        fun deleteByFlightSearch(id: Int) {
            val query: List<QueryResult<FlightSearchFlightData>> = queryByFlightSearch(id)

            query.forEach {
                it.dataClass.delete()
            }
        }

        fun queryByFlightSearch(id: Int): List<QueryResult<FlightSearchFlightData>> =
            queryDatabase(
                whereArgs = WhereArgs("${FlightSearchFlightColumns.FLIGHT_SEARCH_ID.name} = ?", listOf(id)),
            ).sortedBy {
                it.dataClass.position
            }
    }
}
