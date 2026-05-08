package data

import java.time.LocalTime
import java.time.LocalDate

/**
 * Column definitions for the routes table.
 */
object RouteColumns {
    /** Primary key for the routes table. */
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")

    /** Starting destination ID for the route. */
    val START_DESTINATION =
        Column<Int>("start_destination", "INTEGER NOT NULL REFERENCES ${DestinationData.EMPTY.tableName}(id)")

    /** Ending destination ID for the route. */
    val END_DESTINATION =
        Column<Int>("end_destination", "INTEGER NOT NULL REFERENCES ${DestinationData.EMPTY.tableName}(id)")

    /** Duration of the route as a time value. */
    val DURATION = Column<String>("duration", "STRING NOT NULL")

    /** All route table columns. */
    val ALL = listOf(ID, START_DESTINATION, END_DESTINATION, DURATION)

    /** Column names only, used for projections and joins. */
    val COLUMN_NAMES = ALL.map { it.name }
}

/**
 * Represents a flight route between two destinations.
 *
 * @property id Unique database identifier
 * @property startDestination Starting destination ID
 * @property endDestination Ending destination ID
 * @property duration Flight duration
 */
data class RouteData(
    override val id: Int = 0,
    var startDestination: Int = 0,
    var endDestination: Int = 0,
    var duration: LocalTime = LocalTime.parse("00:00"),
) : DataClass<RouteData>(id) {
    /** Database table name. */
    override val tableName = "routes"

    /** Columns belonging to the routes table. */
    override val tableColumns = RouteColumns.ALL

    /**
     * Prevents duplicate routes between the same two destinations.
     */
    override val tableAdditionalSQL = """
        UNIQUE (
            ${RouteColumns.START_DESTINATION.name},
            ${RouteColumns.END_DESTINATION.name}
        )"""

    /**
     * Indexes used to optimize route lookups.
     */
    override val indexes: List<IndexArgs> =
        listOf(
            IndexArgs("inx_routes_start_destination", RouteColumns.START_DESTINATION.name),
            IndexArgs("inx_routes_end_destination", RouteColumns.END_DESTINATION.name),
            IndexArgs(
                "inx_routes_start_destination_end_destination",
                "${RouteColumns.START_DESTINATION.name}, ${RouteColumns.END_DESTINATION.name}",
            ),
        )

    /**
     * Initial seed routes inserted into the database.
     */
    override val initialRows: List<RouteData>
        get() =
            listOf(
                RouteData(
                    startDestination = DestinationData.getDestinationId("Luton"),
                    endDestination = DestinationData.getDestinationId("Tokyo"),
                    duration = LocalTime.parse("07:00"),
                ),
                RouteData(
                    startDestination = DestinationData.getDestinationId("Luton"),
                    endDestination = DestinationData.getDestinationId("Berlin"),
                    duration = LocalTime.parse("02:00"),
                ),
                RouteData(
                    startDestination = DestinationData.getDestinationId("Tokyo"),
                    endDestination = DestinationData.getDestinationId("Luton"),
                    duration = LocalTime.parse("02:00"),
                ),
                RouteData(
                    startDestination = DestinationData.getDestinationId("Berlin"),
                    endDestination = DestinationData.getDestinationId("Tokyo"),
                    duration = LocalTime.parse("06:00"),
                ),
                RouteData(
                    startDestination = DestinationData.getDestinationId("Berlin"),
                    endDestination = DestinationData.getDestinationId("Luton"),
                    duration = LocalTime.parse("02:00"),
                ),
                RouteData(
                    startDestination = DestinationData.getDestinationId("Tokyo"),
                    endDestination = DestinationData.getDestinationId("Berlin"),
                    duration = LocalTime.parse("06:00"),
                ),
            )

    /**
     * Tables that must exist before routes are created.
     */
    override val requiredTables: List<DataClass<*>>
        get() =
            listOf(
                DestinationData.EMPTY,
                PlaneData.EMPTY,
            )

    /**
     * Maps this route into database column values.
     */
    override fun mapDataToColumns(): Map<Column<*>, Any?> =
        mapOf(
            RouteColumns.START_DESTINATION to startDestination,
            RouteColumns.END_DESTINATION to endDestination,
            RouteColumns.DURATION to duration,
        )

    /**
     * Converts a database row into a `RouteData` instance.
     */
    override fun mapRowToData(row: Array<Any?>): RouteData =
        RouteData(
            id = castRowElement(row, RouteColumns.ID),
            startDestination = castRowElement(row, RouteColumns.START_DESTINATION),
            endDestination = castRowElement(row, RouteColumns.END_DESTINATION),
            duration = castTimeRowElement(row, RouteColumns.DURATION),
        )

    /**
     * Prints a human-readable representation for debugging.
     */
    override fun debugData() {
        println("Route data: (\"$id\", \"$startDestination\", \"$endDestination\", \"$duration\")")
    }

    /**
     * Calculates how popular this route is within a date range,
     * based on booked seats.
     *
     * @param startDate Start of the date range (inclusive)
     * @param endDate End of the date range (exclusive)
     * @return Number of booked seats on this route
     */
    fun getRoutePopularity(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Int {
        val joinArgs: MultipleJoinArgs =
            MultipleJoinArgs(
                listOf(
                    JoinArgs(
                        "INNER",
                        FlightData.EMPTY.tableName,
                        RouteColumns.ID.name,
                        FlightColumns.ROUTE_ID.name,
                        FlightColumns.COLUMN_NAMES,
                    ),
                    JoinArgs(
                        "INNER",
                        SeatData.EMPTY.tableName,
                        FlightColumns.ID.name,
                        SeatColumns.FLIGHT_ID.name,
                        SeatColumns.COLUMN_NAMES,
                        FlightData.EMPTY.tableName,
                    ),
                    JoinArgs(
                        "INNER",
                        BookedSeatData.EMPTY.tableName,
                        SeatColumns.ID.name,
                        BookedSeatColumns.SEAT_ID.name,
                        BookedSeatColumns.COLUMN_NAMES,
                        SeatData.EMPTY.tableName,
                    ),
                ),
            )

        val whereArgs: WhereArgs =
            WhereArgs(
                whereClause = """
            ${RouteData.EMPTY.tableName}.${RouteColumns.ID.name} = ?
            AND ${FlightData.EMPTY.tableName}.${FlightColumns.DATE.name} >= ?
            AND ${FlightData.EMPTY.tableName}.${FlightColumns.DATE.name} < ?
            """,
                listOf(id, startDate, endDate),
            )

        return queryDatabase(
            multipleJoinArgs = joinArgs,
            whereArgs = whereArgs,
        ).size
    }

    companion object {
        /** Empty instance used for static database access. */
        val EMPTY: RouteData
            get() = RouteData()

        /**
         * Generic route query wrapper.
         */
        fun queryDatabase(
            multipleJoinArgs: MultipleJoinArgs? = null,
            whereArgs: WhereArgs? = null,
            orderByArgs: OrderByArgs? = null,
            limitArgs: LimitArgs? = null,
            groupByArgs: GroupByArgs? = null,
        ): List<QueryResult<RouteData>> =
            EMPTY.queryDatabase(multipleJoinArgs, whereArgs, orderByArgs, limitArgs, groupByArgs)

        /**
         * Updates routes matching the given WHERE clause.
         */
        fun updateTable(
            values: Map<Column<*>, Any?>,
            whereArgs: WhereArgs,
        ): Int = EMPTY.updateTable(values, whereArgs)

        /**
         * Deletes a route by ID.
         */
        fun delete(id: Int): Int = RouteData(id = id).delete()

        /**
         * Queries a route using destination IDs.
         */
        fun queryDatabase(
            destinationArgs: DestinationArgs,
            multipleJoinArgs: MultipleJoinArgs? = null,
        ): List<QueryResult<RouteData>> {
            val whereClause = "${RouteColumns.START_DESTINATION.name} = ? AND ${RouteColumns.END_DESTINATION.name} = ?"
            val whereArgs = listOf(destinationArgs.startDestination, destinationArgs.endDestination)
            return EMPTY.queryDatabase(multipleJoinArgs, WhereArgs(whereClause, whereArgs))
        }

        /**
         * Queries a route using city and country names.
         */
        fun queryDatabase(
            startCity: String,
            startCountry: String,
            endCity: String,
            endCountry: String,
            multipleJoinArgs: MultipleJoinArgs? = null,
        ): List<QueryResult<RouteData>> {
            val startDestinationResults =
                DestinationData.queryDatabase(startCity, startCountry)
            if (startDestinationResults.isEmpty()) {
                return emptyList()
            }

            val endDestinationResults =
                DestinationData.queryDatabase(endCity, endCountry)
            if (endDestinationResults.isEmpty()) {
                return emptyList()
            }

            return queryDatabase(
                DestinationArgs(
                    startDestinationResults.first().dataClass.id,
                    endDestinationResults.first().dataClass.id,
                ),
                multipleJoinArgs,
            )
        }

        /**
         * Queries a route using "Country - City" formatted strings.
         */
        fun queryDatabase(
            start: String,
            end: String,
            multipleJoinArgs: MultipleJoinArgs? = null,
        ): List<QueryResult<RouteData>> {
            val startElements = start.split(" - ")
            if (startElements.size != 2) {
                return emptyList()
            }

            val endElements = end.split(" - ")
            if (endElements.size != 2) {
                return emptyList()
            }

            val query =
                queryDatabase(
                    startElements[0],
                    startElements[1],
                    endElements[0],
                    endElements[1],
                    multipleJoinArgs,
                )

            if (query.isNotEmpty()) {
                return query
            }

            return queryDatabase(
                startElements[1],
                startElements[0],
                endElements[1],
                endElements[0],
                multipleJoinArgs,
            )
        }

        /**
         * Queries a route by its ID.
         */
        fun queryDatabase(id: Int): List<QueryResult<RouteData>> =
            queryDatabase(whereArgs = WhereArgs("${RouteColumns.ID.name} = ?", listOf(id)))

        /**
         * Computes all destination paths up to a given number of layovers.
         */
        fun getPathByLayovers(
            destinationArgs: DestinationArgs,
            layovers: Int = 2,
        ): List<List<Int>> {
            var currentPaths: List<List<Int>> = listOf(listOf(destinationArgs.startDestination))

            repeat(layovers) {
                val nextPaths = mutableListOf<List<Int>>()

                for (path in currentPaths) {
                    val neighbours =
                        queryDatabase(
                            whereArgs =
                                WhereArgs(
                                    "${RouteColumns.START_DESTINATION.name} = ?",
                                    listOf(path.last()),
                                ),
                        )

                    for (neighbour in neighbours) {
                        nextPaths.add(path + neighbour.dataClass.endDestination)
                    }
                }

                currentPaths = currentPaths + nextPaths
            }

            return currentPaths
                .filter { it.last() == destinationArgs.endDestination }
                .distinct()
        }

        /**
         * Computes paths using city and country names.
         */
        fun getPathByLayovers(
            startCity: String,
            startCountry: String,
            endCity: String,
            endCountry: String,
            layovers: Int = 2,
        ): List<List<Int>> {
            val startDestinationResults =
                DestinationData.queryDatabase(startCity, startCountry)
            if (startDestinationResults.isEmpty()) {
                return emptyList()
            }

            val endDestinationResults =
                DestinationData.queryDatabase(endCity, endCountry)
            if (endDestinationResults.isEmpty()) {
                return emptyList()
            }

            return getPathByLayovers(
                DestinationArgs(
                    startDestinationResults.first().dataClass.id,
                    endDestinationResults.first().dataClass.id,
                ),
                layovers,
            )
        }

        /**
         * Computes paths using "Country - City" formatted strings.
         */
        fun getPathByLayovers(
            start: String,
            end: String,
            layovers: Int = 2,
        ): List<List<Int>> {
            val startElements = start.split(" - ")
            if (startElements.size != 2) {
                return emptyList()
            }

            val endElements = end.split(" - ")
            if (endElements.size != 2) {
                return emptyList()
            }

            val query =
                getPathByLayovers(
                    startElements[0],
                    startElements[1],
                    endElements[0],
                    endElements[1],
                    layovers,
                )

            if (query.isNotEmpty()) {
                return query
            }

            return getPathByLayovers(
                startElements[1],
                startElements[0],
                endElements[1],
                endElements[0],
                layovers,
            )
        }

        /**
         * Builds `JourneyRoute` objects from computed destination paths.
         */
        fun getJourneyRoutes(
            destinationArgs: DestinationArgs,
            layovers: Int = 2,
        ): List<JourneyRoute> {
            val path = getPathByLayovers(destinationArgs, layovers)
            return path.map { routeIds ->
                JourneyRoute(
                    routeIds,
                    routeIds.map { destinationId ->
                        val destination =
                            DestinationData
                                .queryDatabase(
                                    whereArgs = WhereArgs("${DestinationColumns.ID.name} = ?", listOf(destinationId)),
                                ).firstOrNull()
                                ?.dataClass ?: return@map "UNKNOWN"

                        val country =
                            CountryData
                                .queryDatabase(
                                    whereArgs =
                                        WhereArgs(
                                            "${CountryColumns.ID.name} = ?",
                                            listOf(destination.countryId),
                                        ),
                                ).firstOrNull()
                                ?.dataClass ?: return@map "UNKNOWN"

                        "${country.name} - ${destination.cityName}"
                    },
                )
            }
        }

        /**
         * Builds journey routes using city and country names.
         */
        fun getJourneyRoutes(
            startCity: String,
            startCountry: String,
            endCity: String,
            endCountry: String,
            layovers: Int = 2,
        ): List<JourneyRoute> {
            val startDestinationResults =
                DestinationData.queryDatabase(startCity, startCountry)
            if (startDestinationResults.isEmpty()) {
                return emptyList()
            }

            val endDestinationResults =
                DestinationData.queryDatabase(endCity, endCountry)
            if (endDestinationResults.isEmpty()) {
                return emptyList()
            }

            return getJourneyRoutes(
                DestinationArgs(
                    startDestinationResults.first().dataClass.id,
                    endDestinationResults.first().dataClass.id,
                ),
                layovers,
            )
        }

        /**
         * Builds journey routes using "Country - City" formatted strings.
         */
        fun getJourneyRoutes(
            start: String,
            end: String,
            layovers: Int = 2,
        ): List<JourneyRoute> {
            val startElements = start.split(" - ")
            if (startElements.size != 2) {
                return emptyList()
            }

            val endElements = end.split(" - ")
            if (endElements.size != 2) {
                return emptyList()
            }

            val query =
                getJourneyRoutes(
                    startElements[0],
                    startElements[1],
                    endElements[0],
                    endElements[1],
                    layovers,
                )

            if (query.isNotEmpty()) {
                return query
            }

            return getJourneyRoutes(
                startElements[1],
                startElements[0],
                endElements[1],
                endElements[0],
                layovers,
            )
        }

        /**
         * Returns the duration of a route by ID.
         */
        fun getDuration(id: Int): LocalTime {
            val query = queryDatabase(id)
            if (query.isEmpty()) {
                return LocalTime.parse("00:00")
            }
            return query.first().dataClass.duration
        }

        /**
         * Returns the duration of a route in minutes.
         */
        fun getDurationMinutes(id: Int): Long {
            val time = getDuration(id)
            return time.minute + time.hour * 60L
        }

        /**
         * Retrieves the route ID for the given destination pair.
         */
        fun getRouteId(destinationArgs: DestinationArgs): Int {
            val query = queryDatabase(destinationArgs)
            if (query.isEmpty()) {
                println("Could not find route ${destinationArgs.startDestination} to ${destinationArgs.endDestination}")
                return -1
            }
            return query.first().dataClass.id
        }

        /**
         * Returns the most popular routes over a recent time period.
         */
        fun getPopularRoutes(
            amount: Int,
            daysPrior: Long,
        ): List<QueryResult<RouteData>> {
            val startDate = LocalDate.now().minusDays(daysPrior)

            return queryDatabase()
                .sortedByDescending { route ->
                    route.dataClass.getRoutePopularity(startDate, LocalDate.now().plusDays(7))
                }.take(amount)
        }
    }
}
