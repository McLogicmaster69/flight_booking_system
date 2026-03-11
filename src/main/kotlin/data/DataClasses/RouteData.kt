package data

import java.time.LocalTime

object RouteColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val START_DESTINATION = Column<Int>("start_destination", "INTEGER NOT NULL REFERENCES destinations(id)")
    val END_DESTINATION = Column<Int>("end_destination", "INTEGER NOT NULL REFERENCES destinations(id)")
    val PLANE_ID = Column<Int>("plane_id", "INTEGER NOT NULL REFERENCES planes(id)")
    val DURATION = Column<String>("duration", "STRING NOT NULL")

    val ALL = listOf(ID, START_DESTINATION, END_DESTINATION, PLANE_ID, DURATION)
}

data class RouteData(

    override val id: Int = 0,
    var startDestination : Int = 0,
    var endDestination : Int = 0,
    var planeId : Int = 0,
    var duration : LocalTime = LocalTime.parse("00:00")

) : DataClass<RouteData>(id) {

    override val tableName = "routes"
    override val tableColumns = RouteColumns.ALL

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            RouteColumns.START_DESTINATION to startDestination,
            RouteColumns.END_DESTINATION to endDestination,
            RouteColumns.PLANE_ID to planeId,
            RouteColumns.DURATION to duration
        )

    override fun mapRowToData(row : Array<Any?>) : RouteData =
        RouteData(
            id = castRowElement(row, RouteColumns.ID),
            startDestination = castRowElement(row, RouteColumns.START_DESTINATION),
            endDestination = castRowElement(row, RouteColumns.END_DESTINATION),
            planeId = castRowElement(row, RouteColumns.PLANE_ID),
            duration = castTimeRowElement(row, RouteColumns.DURATION)
        )

    override fun debugData() {
        println("Route data: (\"$id\", \"$startDestination\", \"$endDestination\", \"$planeId\", \"$duration\")")
    }

    companion object {
        val EMPTY : RouteData
            get() = RouteData()

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null
        ) : List<QueryResult<RouteData>> {
            return EMPTY.queryDatabase(joinArgs, whereArgs)
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)

        fun delete(id : Int) : Int {
            return RouteData(id = id).delete()
        }

        fun queryDatabase(
            destinationArgs : DestinationArgs,
            joinArgs : JoinArgs? = null
        ) : List<QueryResult<RouteData>> {
            val whereClause = "${RouteColumns.START_DESTINATION.name} = ? AND ${RouteColumns.END_DESTINATION.name} = ?"
            val whereArgs = listOf(destinationArgs.startDestination, destinationArgs.endDestination)
            return EMPTY.queryDatabase(joinArgs, WhereArgs(whereClause, whereArgs))
        }

        fun getPathByLayovers(
            destinationArgs : DestinationArgs,
            layovers : Int = 2
        ) : List<List<Int>> {
            val reachable : MutableList<List<Int>> = mutableListOf(listOf(destinationArgs.startDestination))
            for (i in 1..layovers) {
                for (r in reachable) {
                    val neighbours = queryDatabase(whereArgs = WhereArgs("${RouteColumns.START_DESTINATION.name} = ?", listOf(r.last())))
                    for (neighbour in neighbours) {
                        reachable.add(r.toList() + listOf(neighbour.dataClass.endDestination))
                    }
                }
            }

            val output : MutableList<List<Int>> = mutableListOf()
            reachable.forEach { r ->
                if (r.last() == destinationArgs.endDestination) {
                    output.add(r)
                }
            }

            return output.toList()
        }

        fun getJourneyRoutes (
            destinationArgs : DestinationArgs,
            layovers : Int = 2
        ) : List<JourneyRoute> {
            val path : List<List<Int>> = getPathByLayovers(destinationArgs, layovers)
            return path.map { routeIds ->
                JourneyRoute(
                    routeIds,
                    routeIds.map { destinationId ->
                        val destination = DestinationData.queryDatabase(
                            whereArgs = WhereArgs("${DestinationColumns.ID.name} = ?", listOf(destinationId))
                        ).firstOrNull()?.dataClass ?: return@map "UNKNOWN"

                        val country = CountryData.queryDatabase(
                            whereArgs = WhereArgs("${CountryColumns.ID.name} = ?", listOf(destination.countryId))
                        ).firstOrNull()?.dataClass ?: return@map "UNKNOWN"

                        "${country.name} - ${destination.cityName}"
                    }
                )
            }
        }
    }
}
