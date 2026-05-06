package data

import java.time.LocalTime
import java.time.LocalDate

object RouteColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val START_DESTINATION = Column<Int>("start_destination", "INTEGER NOT NULL REFERENCES ${DestinationData.EMPTY.tableName}(id)")
    val END_DESTINATION = Column<Int>("end_destination", "INTEGER NOT NULL REFERENCES ${DestinationData.EMPTY.tableName}(id)")
    val DURATION = Column<String>("duration", "STRING NOT NULL")

    val ALL = listOf(ID, START_DESTINATION, END_DESTINATION, DURATION)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class RouteData(

    override val id: Int = 0,
    var startDestination : Int = 0,
    var endDestination : Int = 0,
    var duration : LocalTime = LocalTime.parse("00:00")

) : DataClass<RouteData>(id) {

    override val tableName = "routes"
    override val tableColumns = RouteColumns.ALL
    override val tableAdditionalSQL = "UNIQUE (${RouteColumns.START_DESTINATION.name}, ${RouteColumns.END_DESTINATION.name})"

    override val indexes : List<IndexArgs> = listOf(
        IndexArgs("inx_routes_start_destination", RouteColumns.START_DESTINATION.name),
        IndexArgs("inx_routes_end_destination", RouteColumns.END_DESTINATION.name),
        IndexArgs("inx_routes_start_destination_end_destination", "${RouteColumns.START_DESTINATION.name}, ${RouteColumns.END_DESTINATION.name}")
    )

    override val initialRows : List<RouteData>
        get() = listOf(
            RouteData(
                startDestination = DestinationData.getDestinationId("Luton"),
                endDestination = DestinationData.getDestinationId("Tokyo"),
                duration = LocalTime.parse("07:00")
            ),
            RouteData(
                startDestination = DestinationData.getDestinationId("Luton"),
                endDestination = DestinationData.getDestinationId("Berlin"),
                duration = LocalTime.parse("02:00")
            ),
            RouteData(
                startDestination = DestinationData.getDestinationId("Tokyo"),
                endDestination = DestinationData.getDestinationId("Luton"),
                duration = LocalTime.parse("02:00")
            ),
            RouteData(
                startDestination = DestinationData.getDestinationId("Berlin"),
                endDestination = DestinationData.getDestinationId("Tokyo"),
                duration = LocalTime.parse("06:00")
            ),
            RouteData(
                startDestination = DestinationData.getDestinationId("Berlin"),
                endDestination = DestinationData.getDestinationId("Luton"),
                duration = LocalTime.parse("02:00")
            ),
            RouteData(
                startDestination = DestinationData.getDestinationId("Tokyo"),
                endDestination = DestinationData.getDestinationId("Berlin"),
                duration = LocalTime.parse("06:00")
            )
        )
        
    override val requiredTables : List<DataClass<*>>
        get() = listOf(
            DestinationData.EMPTY,
            PlaneData.EMPTY
        )

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            RouteColumns.START_DESTINATION to startDestination,
            RouteColumns.END_DESTINATION to endDestination,
            RouteColumns.DURATION to duration
        )

    override fun mapRowToData(row : Array<Any?>) : RouteData =
        RouteData(
            id = castRowElement(row, RouteColumns.ID),
            startDestination = castRowElement(row, RouteColumns.START_DESTINATION),
            endDestination = castRowElement(row, RouteColumns.END_DESTINATION),
            duration = castTimeRowElement(row, RouteColumns.DURATION)
        )

    override fun debugData() {
        println("Route data: (\"$id\", \"$startDestination\", \"$endDestination\", \"$duration\")")
    }

    fun getRoutePopularity(startDate : LocalDate, endDate : LocalDate) : Int {
        val joinArgs : MultipleJoinArgs = MultipleJoinArgs(
            listOf(
                JoinArgs(
                    "INNER",
                    FlightData.EMPTY.tableName,
                    RouteColumns.ID.name,
                    FlightColumns.ROUTE_ID.name,
                    FlightColumns.COLUMN_NAMES
                ),
                JoinArgs(
                    "INNER",
                    SeatData.EMPTY.tableName,
                    FlightColumns.ID.name,
                    SeatColumns.FLIGHT_ID.name,
                    SeatColumns.COLUMN_NAMES,
                    FlightData.EMPTY.tableName
                ),
                JoinArgs(
                    "INNER",
                    BookedSeatData.EMPTY.tableName,
                    SeatColumns.ID.name,
                    BookedSeatColumns.SEAT_ID.name,
                    BookedSeatColumns.COLUMN_NAMES,
                    SeatData.EMPTY.tableName
                )
            )
        )

        val whereArgs : WhereArgs = WhereArgs (
            whereClause = """
            ${RouteData.EMPTY.tableName}.${RouteColumns.ID.name} = ?
            AND ${FlightData.EMPTY.tableName}.${FlightColumns.DATE.name} >= ?
            AND ${FlightData.EMPTY.tableName}.${FlightColumns.DATE.name} < ?
            """,
            listOf(id, startDate, endDate)
        )

        return queryDatabase(
            multipleJoinArgs = joinArgs,
            whereArgs = whereArgs
        ).size
    }

    companion object {
        val EMPTY : RouteData
            get() = RouteData()

        fun queryDatabase (
            multipleJoinArgs : MultipleJoinArgs? = null,
            whereArgs : WhereArgs? = null,
            orderByArgs : OrderByArgs? = null,
            limitArgs : LimitArgs? = null,
            groupByArgs : GroupByArgs? = null   
        ) : List<QueryResult<RouteData>> {
            return EMPTY.queryDatabase(multipleJoinArgs, whereArgs, orderByArgs, limitArgs, groupByArgs)
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)

        fun delete(id : Int) : Int {
            return RouteData(id = id).delete()
        }

        fun queryDatabase (
            destinationArgs : DestinationArgs,
            multipleJoinArgs : MultipleJoinArgs? = null
        ) : List<QueryResult<RouteData>> {
            val whereClause = "${RouteColumns.START_DESTINATION.name} = ? AND ${RouteColumns.END_DESTINATION.name} = ?"
            val whereArgs = listOf(destinationArgs.startDestination, destinationArgs.endDestination)
            return EMPTY.queryDatabase(multipleJoinArgs, WhereArgs(whereClause, whereArgs))
        }

        fun queryDatabase (
            startCity : String,
            startCountry : String,
            endCity : String,
            endCountry : String,            
            multipleJoinArgs : MultipleJoinArgs? = null
        ) : List<QueryResult<RouteData>> {
            val startDestinationResults : List<QueryResult<DestinationData>> = DestinationData.queryDatabase(startCity, startCountry)
            if (startDestinationResults.isEmpty())
                return emptyList<QueryResult<RouteData>>()

            val endDestinationResults : List<QueryResult<DestinationData>> = DestinationData.queryDatabase(endCity, endCountry)
            if (endDestinationResults.isEmpty())
                return emptyList<QueryResult<RouteData>>()

            return queryDatabase(
                DestinationArgs(
                    startDestinationResults.first().dataClass.id,
                    endDestinationResults.first().dataClass.id
                ),
                multipleJoinArgs
            )
        }

        fun queryDatabase (
            start : String,
            end : String,
            multipleJoinArgs : MultipleJoinArgs? = null
        ) : List<QueryResult<RouteData>> {
            val startElements = start.split(" - ")
            if (startElements.size != 2)
                return emptyList<QueryResult<RouteData>>()

            val endElements = end.split(" - ")
            if (endElements.size != 2)
                return emptyList<QueryResult<RouteData>>()

            val query : List<QueryResult<RouteData>> = queryDatabase(
                startElements[0],
                startElements[1],
                endElements[0],
                endElements[1],
                multipleJoinArgs
            )

            if (query.isNotEmpty())
                return query

            return queryDatabase(
                startElements[1],
                startElements[0],
                endElements[1],
                endElements[0],
                multipleJoinArgs
            )
        }

        fun queryDatabase(id : Int) : List<QueryResult<RouteData>> = queryDatabase(whereArgs = WhereArgs("${RouteColumns.ID.name} = ?", listOf(id)))

        fun getPathByLayovers (
            destinationArgs : DestinationArgs,
            layovers : Int = 2
        ) : List<List<Int>> {
            var currentPaths: List<List<Int>> = listOf(listOf(destinationArgs.startDestination))

            repeat (layovers) {
                val nextPaths = mutableListOf<List<Int>>()

                for (path in currentPaths) {
                    val neighbours = queryDatabase(
                        whereArgs = WhereArgs("${RouteColumns.START_DESTINATION.name} = ?",
                        listOf(path.last()))
                    )

                    for (neighbour in neighbours) {
                        nextPaths.add(path + neighbour.dataClass.endDestination)
                    }
                }

                currentPaths = currentPaths + nextPaths
            }

            return currentPaths.filter {
                it.last() == destinationArgs.endDestination
            }.distinct()
        }

        fun getPathByLayovers (
            startCity : String,
            startCountry : String,
            endCity : String,
            endCountry : String,
            layovers : Int = 2
        ) : List<List<Int>> {
            val startDestinationResults : List<QueryResult<DestinationData>> = DestinationData.queryDatabase(startCity, startCountry)
            if (startDestinationResults.isEmpty())
                return emptyList<List<Int>>()

            val endDestinationResults : List<QueryResult<DestinationData>> = DestinationData.queryDatabase(endCity, endCountry)
            if (endDestinationResults.isEmpty())
                return emptyList<List<Int>>()

            return getPathByLayovers(
                DestinationArgs(
                    startDestinationResults.first().dataClass.id,
                    endDestinationResults.first().dataClass.id
                ),
                layovers
            )
        }

        fun getPathByLayovers (
            start : String,
            end : String,
            layovers : Int = 2
        ) : List<List<Int>>  {
            val startElements = start.split(" - ")
            if (startElements.size != 2)
                return emptyList<List<Int>>()

            val endElements = end.split(" - ")
            if (endElements.size != 2)
                return emptyList<List<Int>>()

            val query : List<List<Int>> = getPathByLayovers(
                startElements[0],
                startElements[1],
                endElements[0],
                endElements[1],
                layovers
            )

            if (query.isNotEmpty())
                return query

            return getPathByLayovers(
                startElements[1],
                startElements[0],
                endElements[1],
                endElements[0],
                layovers
            )
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

        fun getJourneyRoutes (
            startCity : String,
            startCountry : String,
            endCity : String,
            endCountry : String,
            layovers : Int = 2
        ) : List<JourneyRoute> {
            val startDestinationResults : List<QueryResult<DestinationData>> = DestinationData.queryDatabase(startCity, startCountry)
            if (startDestinationResults.isEmpty())
                return emptyList<JourneyRoute>()

            val endDestinationResults : List<QueryResult<DestinationData>> = DestinationData.queryDatabase(endCity, endCountry)
            if (endDestinationResults.isEmpty())
                return emptyList<JourneyRoute>()

            return getJourneyRoutes(
                DestinationArgs(
                    startDestinationResults.first().dataClass.id,
                    endDestinationResults.first().dataClass.id
                ),
                layovers
            )
        }

        fun getJourneyRoutes (
            start : String,
            end : String,
            layovers : Int = 2
        ) : List<JourneyRoute>  {
            val startElements = start.split(" - ")
            if (startElements.size != 2)
                return emptyList<JourneyRoute>()

            val endElements = end.split(" - ")
            if (endElements.size != 2)
                return emptyList<JourneyRoute>()

            val query : List<JourneyRoute> = getJourneyRoutes(
                startElements[0],
                startElements[1],
                endElements[0],
                endElements[1],
                layovers
            )

            if (query.isNotEmpty())
                return query

            return getJourneyRoutes(
                startElements[1],
                startElements[0],
                endElements[1],
                endElements[0],
                layovers
            )
        }

        fun getDuration(id : Int) : LocalTime {
            val query = queryDatabase(id)
            if (query.isEmpty())
                return LocalTime.parse("00:00")
            return query.first().dataClass.duration
        }

        fun getDurationMinutes(id : Int) : Long {
            val time : LocalTime = getDuration(id)
            return time.minute + time.hour * 60L
        }

        fun getRouteId (
            destinationArgs : DestinationArgs
        ) : Int {
            val query : List<QueryResult<RouteData>> = queryDatabase(destinationArgs)
            if (query.isEmpty()) {
                println("Could not find route ${destinationArgs.startDestination} to ${destinationArgs.endDestination}")
                return -1
            }

            return query.first().dataClass.id
        }

        fun getPopularRoutes (
            amount : Int,
            daysPrior : Long
        ) : List<QueryResult<RouteData>> {
            val startDate : LocalDate = LocalDate.now().minusDays(daysPrior)

            return queryDatabase().sortedByDescending { route ->
                route.dataClass.getRoutePopularity(startDate, LocalDate.now())
            }.take(amount)
        }
    }
}
