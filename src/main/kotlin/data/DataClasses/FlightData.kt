package data

import java.time.LocalDate
import java.time.LocalTime

object FlightColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val ROUTE_ID = Column<Int>("route_id", "INTEGER NOT NULL REFERENCES routes(id)")
    val DATE = Column<String>("date", "STRING NOT NULL")
    val TIME = Column<String>("time", "STRING NOT NULL")

    val ALL = listOf(ID, ROUTE_ID, DATE, TIME)
}

data class FlightData(

    override val id: Int = 0,
    var routeId : Int = 0,
    var date : LocalDate = LocalDate.parse("1970-01-01"),
    var time : LocalTime = LocalTime.parse("00:00")

) : DataClass<FlightData>(id) {

    override val tableName = "flights"
    override val tableColumns = FlightColumns.ALL

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            FlightColumns.ROUTE_ID to routeId,
            FlightColumns.DATE to date.toString(),
            FlightColumns.TIME to time.toString()
        )

    override fun mapRowToData(row : Array<Any?>) : FlightData =
        FlightData(
            id = castRowElement(row, FlightColumns.ID),
            routeId = castRowElement(row, FlightColumns.ROUTE_ID),
            date = castDateRowElement(row, FlightColumns.DATE),
            time = castTimeRowElement(row, FlightColumns.TIME)
        )

    override fun debugData() {
        println("Flight data: (\"$id\", \"$routeId\", \"$date\", \"$time\")")
    }

    companion object {
        val EMPTY : FlightData
            get() = FlightData()

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null
        ) : List<QueryResult<FlightData>> {
            return EMPTY.queryDatabase(joinArgs, whereArgs)
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)

        fun delete(id : Int) : Int {
            return FlightData(id = id).delete()
        }

        fun queryDatabase(
            routeIds : List<Int>,
            joinArgs : JoinArgs? = null
        ) : List<QueryResult<FlightData>> {
            val whereClause = routeIds.joinToString(" OR ") { "${FlightColumns.ROUTE_ID.name} = ?" }
            val whereArgs = routeIds.map { it as Any? }
            return EMPTY.queryDatabase(joinArgs, WhereArgs(whereClause, whereArgs))
        }

        fun queryDatabase(
            routeIds : List<Int>,
            date : LocalDate,
            joinArgs : JoinArgs? = null
        ) : List<QueryResult<FlightData>> {
            val whereClause = "(" + routeIds.joinToString(" OR ") { "${FlightColumns.ROUTE_ID.name} = ?" } + ") AND ${FlightColumns.DATE.name} = ?"
            val whereArgs = routeIds.map { it as Any? } + listOf(date.toString())
            return EMPTY.queryDatabase(joinArgs, WhereArgs(whereClause, whereArgs))
        }

        fun queryDatabase(
            destinationArgs : DestinationArgs,
            joinArgs : JoinArgs? = null
        ) : List<QueryResult<FlightData>> {
            val routes : List<QueryResult<RouteData>> = RouteData.queryDatabase(destinationArgs, joinArgs)
            val routeIds : List<Int> = routes.map { route ->
                route.dataClass.id
            }
            return queryDatabase(routeIds, joinArgs)
        }

        fun queryDatabase(
            destinationArgs : DestinationArgs,
            date : LocalDate,
            joinArgs : JoinArgs? = null
        ) : List<QueryResult<FlightData>> {
            val routes : List<QueryResult<RouteData>> = RouteData.queryDatabase(destinationArgs, joinArgs)
            val routeIds : List<Int> = routes.map { route ->
                route.dataClass.id
            }
            return queryDatabase(routeIds, date, joinArgs)
        }
    }
}
