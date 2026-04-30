package data

import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime
import java.time.Duration
import kotlin.math.roundToInt
import results.CreateFlightResults
import results.StaffAssignmentResults

const val TRANSFER_TIME : Int = 120
const val MAX_TIME : Int = 2880

object FlightColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val ROUTE_ID = Column<Int>("route_id", "INTEGER NOT NULL REFERENCES routes(id)")
    val PLANE_ID = Column<Int>("plane_id", "INTEGER NOT NULL REFERENCES planes(id)")
    val DATE = Column<String>("date", "STRING NOT NULL")
    val TIME = Column<String>("time", "STRING NOT NULL")

    val ALL = listOf(ID, ROUTE_ID, PLANE_ID, DATE, TIME)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class FlightData(

    override val id: Int = 0,
    var routeId : Int = 0,
    var planeId : Int = 0,
    var date : LocalDate = LocalDate.parse("1970-01-01"),
    var time : LocalTime = LocalTime.parse("00:00")

) : DataClass<FlightData>(id) {

    override val tableName = "flights"
    override val tableColumns = FlightColumns.ALL
    override val tableAdditionalSQL = "UNIQUE (route_id, plane_id, date, time)"

    override val initialRows : List<FlightData>
        get() = listOf(
            FlightData(
                routeId = RouteData.getRouteId(
                    DestinationArgs(
                        DestinationData.getDestinationId("Luton"),
                        DestinationData.getDestinationId("Tokyo")
                    )
                ),
                planeId = PlaneData.getPlaneId("Boeing 737-800"),
                date = LocalDate.parse("2026-05-15"),
                time = LocalTime.parse("12:00")
            ),
            FlightData(
                routeId = RouteData.getRouteId(
                    DestinationArgs(
                        DestinationData.getDestinationId("Luton"),
                        DestinationData.getDestinationId("Berlin")
                    )
                ),
                planeId = PlaneData.getPlaneId("Airbus A321"),
                date = LocalDate.parse("2026-05-15"),
                time = LocalTime.parse("10:00")
            ),
            FlightData(
                routeId = RouteData.getRouteId(
                    DestinationArgs(
                        DestinationData.getDestinationId("Berlin"),
                        DestinationData.getDestinationId("Tokyo")
                    )
                ),
                planeId = PlaneData.getPlaneId("Boeing 737-800"),
                date = LocalDate.parse("2026-05-15"),
                time = LocalTime.parse("16:00")
            )
        )

    override val requiredTables : List<DataClass<*>>
        get() = listOf(
            RouteData.EMPTY,
            PlaneData.EMPTY,
            DestinationData.EMPTY
        )

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            FlightColumns.ROUTE_ID to routeId,
            FlightColumns.PLANE_ID to planeId,
            FlightColumns.DATE to date.toString(),
            FlightColumns.TIME to time.toString()
        )

    override fun mapRowToData(row : Array<Any?>) : FlightData =
        FlightData(
            id = castRowElement(row, FlightColumns.ID),
            routeId = castRowElement(row, FlightColumns.ROUTE_ID),
            planeId = castRowElement(row, FlightColumns.PLANE_ID),
            date = castDateRowElement(row, FlightColumns.DATE),
            time = castTimeRowElement(row, FlightColumns.TIME)
        )

    override fun debugData() {
        println("Flight data: (\"$id\", \"$routeId\", \"$planeId\" \"$date\", \"$time\")")
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

        fun queryDatabase (
            routeIds : List<Int>,
            joinArgs : JoinArgs? = null
        ) : List<QueryResult<FlightData>> {
            val whereClause = routeIds.joinToString(" OR ") { "${FlightColumns.ROUTE_ID.name} = ?" }
            val whereArgs = routeIds.map { it as Any? }
            return EMPTY.queryDatabase(joinArgs, WhereArgs(whereClause, whereArgs))
        }

        fun queryDatabase (
            routeIds : List<Int>,
            date : LocalDate,
            joinArgs : JoinArgs? = null
        ) : List<QueryResult<FlightData>> {
            val whereClause = "(" + routeIds.joinToString(" OR ") { "${FlightColumns.ROUTE_ID.name} = ?" } + ") AND ${FlightColumns.DATE.name} = ?"
            val whereArgs = routeIds.map { it as Any? } + listOf(date.toString())
            return EMPTY.queryDatabase(joinArgs, WhereArgs(whereClause, whereArgs))
        }

        fun queryDatabase (
            destinationArgs : DestinationArgs,
            joinArgs : JoinArgs? = null
        ) : List<QueryResult<FlightData>> {
            val routes : List<QueryResult<RouteData>> = RouteData.queryDatabase(destinationArgs, joinArgs)
            val routeIds : List<Int> = routes.map { route ->
                route.dataClass.id
            }
            return queryDatabase(routeIds, joinArgs)
        }

        fun queryDatabase (
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

        fun queryDatabase (
            destinationArgs : DestinationArgs,
            dateTime : LocalDateTime,
            joinArgs : JoinArgs? = null
        ) : List<QueryResult<FlightData>> = queryDatabase(destinationArgs, dateTime.toLocalDate(), joinArgs)

        fun queryDatabase(id : Int) : List<QueryResult<FlightData>> = queryDatabase(whereArgs = WhereArgs("${FlightColumns.ID.name} = ?", listOf(id)))

        private fun offsetTimeByZone (
            dateTime : LocalDateTime,
            destinationId : Int
        ) : LocalDateTime {
            val query : List<QueryResult<DestinationData>> = DestinationData.queryDatabase(destinationId)
            if (query.isEmpty())
                return dateTime

            return dateTime.plusMinutes((query.first().dataClass.getTimezoneOffset() * 60f).roundToInt().toLong())
        }

        private fun addTime (
            time : LocalTime,
            routeId : Int
        ) : LocalTime {
            val duration : LocalTime = RouteData.getDuration(routeId)
            return time.plusHours(duration.hour.toLong()).plusMinutes(duration.minute.toLong())
        }

        private fun addTime (
            time : LocalTime,
            startId : Int,
            endId : Int
        ) : LocalTime {
            val query = RouteData.queryDatabase(DestinationArgs(startId, endId))
            if (query.isEmpty())
                return time
            return addTime(time, query.first().dataClass.id)
        }

        private fun addTime (
            minutes : Long,
            routeId : Int
        ) : Long {
            val duration : LocalTime = RouteData.getDuration(routeId)
            return minutes + duration.hour.toLong() * 60L + duration.minute.toLong()
        }

        private fun addTime (
            minutes : Long,
            startId : Int,
            endId : Int
        ) : Long {
            val query = RouteData.queryDatabase(DestinationArgs(startId, endId))
            if (query.isEmpty())
                return minutes
            return addTime(minutes, query.first().dataClass.id)
        }

        private fun getJourneyPath (
            route : JourneyRoute,
            dateTime : LocalDateTime,
            i : Int,
            totalMinutes : Long
        ) : List<JourneyFlightTimePath> {

            val locationId = route.destinationIds[i]
            val query : List<QueryResult<FlightData>> = queryDatabase(
                DestinationArgs(
                    locationId,
                    route.destinationIds[i + 1]
                ),
                dateTime
            ) + queryDatabase(
                DestinationArgs(
                    locationId,
                    route.destinationIds[i + 1]
                ),
                dateTime.plusDays(1)
            ) + queryDatabase(
                DestinationArgs(
                    locationId,
                    route.destinationIds[i + 1]
                ),
                dateTime.minusDays(1)
            ).filter { result ->
                offsetTimeByZone(LocalDateTime.of(result.dataClass.date, result.dataClass.time), locationId).isAfter(dateTime)
            }

            if (query.isEmpty())
                return emptyList<JourneyFlightTimePath>()

            if (i == route.destinationIds.size - 2) {
                return query.map { result ->
                    JourneyFlightTimePath(
                        route.destinationIds,
                        route.locationNames,
                        listOf(result.dataClass.id),
                        listOf(LocalDateTime.of(result.dataClass.date, result.dataClass.time)),
                        listOf(DestinationData.getTimezoneCode(locationId)),
                        addTime(totalMinutes, locationId, route.destinationIds[i + 1])
                    )
                }
            }

            val output : MutableList<JourneyFlightTimePath> = mutableListOf()
            
            for (flight in query) {
                val minutesBetweenFlights : Long = Duration.between(
                    dateTime,
                    offsetTimeByZone(
                        LocalDateTime.of(
                            flight.dataClass.date,
                            flight.dataClass.time
                        ),
                        locationId
                    )
                ).toMinutes()

                val minutesInFlightAndTransfer : Long = TRANSFER_TIME + RouteData.getDurationMinutes(locationId)
                val paths : List<JourneyFlightTimePath> = getJourneyPath(
                    route,
                    offsetTimeByZone(
                        LocalDateTime.of(
                            flight.dataClass.date,
                            flight.dataClass.time
                        ),
                        locationId
                    ).plusMinutes(minutesInFlightAndTransfer),
                    i + 1,
                    totalMinutes + minutesBetweenFlights + minutesInFlightAndTransfer
                )

                if (paths.isEmpty())
                    continue

                for (path in paths) {
                    output.add(
                        JourneyFlightTimePath(
                            path.destinationIds,
                            path.locationNames,
                            listOf<Int>(flight.dataClass.id) + path.flightIds,
                            listOf(LocalDateTime.of(flight.dataClass.date, flight.dataClass.time)) + path.localDateTimes,
                            listOf(DestinationData.getTimezoneCode(locationId)) + path.timezones,
                            path.totalMinutes
                        )
                    )
                }
            }

            return output.toList()
        }

        fun getJourneyFlight (
            routes : List<JourneyRoute>,
            date : LocalDate
        ) : List<JourneyFlightTimePath> {
            val output : MutableList<JourneyFlightTimePath> = mutableListOf()
            for (route in routes) {
                val paths : List<JourneyFlightTimePath> = getJourneyPath(
                    route,
                    LocalDateTime.of(date, LocalTime.parse("00:00")),
                    0,
                    0L
                )

                for (path in paths) {
                    output.add(path)
                }
            }

            return output.toList()
        }

        fun getJourneyFlight (
            start : String,
            end : String,
            date : LocalDate,
            layovers : Int = 2
        ) : List<JourneyFlightTimePath> {
            val routes : List<JourneyRoute> = RouteData.getJourneyRoutes(start, end, layovers + 1)

            if (routes.isEmpty())
                return emptyList<JourneyFlightTimePath>()
                
            val flights = getJourneyFlight(routes, date)
            if (flights.isEmpty())
                return emptyList<JourneyFlightTimePath>()

            return flights.sortedBy { it.totalMinutes }
        }

        fun getAvailablePlane (
            modelId : Int,
            locationId : Int,
            date : LocalDate,
            time : LocalTime
        ) : PlaneData? {
            val availablePlanes : List<QueryResult<PlaneData>> = PlaneData.queryDatabase(
                whereArgs = WhereArgs(
                    whereClause = "${PlaneColumns.MODEL_ID} = ? AND ${PlaneColumns.CURRENT_LOCATION} = ?",
                    whereArgs = listOf(modelId, locationId)
                )
            )

            val planeAvailableAtTime : List<QueryResult<PlaneData>> = availablePlanes.filter { 
                LocalDateTime.of(
                    it.dataClass.currentLocationDate, 
                    it.dataClass.currentLocationTime
                ).isBefore(LocalDateTime.of(date, time))
            }

            return planeAvailableAtTime.firstOrNull()?.dataClass
        }

        fun assignStaffToFlight (
            flightId : Int,
            pilots : Int,
            attendants : Int
        ) : StaffAssignmentResults {
            val flight : FlightData = queryDatabase(flightId).firstOrNull()?.dataClass ?: return StaffAssignmentResults(flightId, listOf(), listOf(), "Could not find flight")
            val route : RouteData = RouteData.queryDatabase(flight.routeId).firstOrNull()?.dataClass ?: return StaffAssignmentResults(flightId, listOf(), listOf(), "Could not find route")
            val pilotRole : StaffPositionData = StaffPositionData.queryDatabase(StaffPositions.PILOT).firstOrNull()?.dataClass ?: return StaffAssignmentResults(flightId, listOf(), listOf(), "Could not find pilot role")
            val copilotRole : StaffPositionData = StaffPositionData.queryDatabase(StaffPositions.COPILOT).firstOrNull()?.dataClass ?: return StaffAssignmentResults(flightId, listOf(), listOf(), "Could not find copilot role")
            val attendantRole : StaffPositionData = StaffPositionData.queryDatabase(StaffPositions.FLIGHT_ATTENDANT).firstOrNull()?.dataClass ?: return StaffAssignmentResults(flightId, listOf(), listOf(), "Could not find attendant role")

            val startTime : LocalDateTime = LocalDateTime.of(
                flight.date,
                flight.time
            ).minusHours(1)
            val endTime : LocalDateTime = startTime.plusMinutes(route.duration.hour * 60L + route.duration.minute + 120L)

            val availableStaff : List<QueryResult<StaffData>> = StaffData.queryDatabase(
                whereArgs = WhereArgs(
                    whereClause = """
                        SELECT 1
                        FROM ${AssignedFlightStaffData.EMPTY.tableName}
                        INNER JOIN ${FlightData.EMPTY.tableName}
                        ON ${FlightData.EMPTY.tableName}.${FlightColumns.ID.name} = ${AssignedFlightStaffData.EMPTY.tableName}.${AssignedFlightStaffColumns.FLIGHT_ID.name}
                        WHERE ${AssignedFlightStaffData.EMPTY.tableName}.${AssignedFlightStaffColumns.STAFF_ID} = ${StaffData.EMPTY.tableName}.${StaffColumns.ID.name}
                        AND ((
                                ${FlightData.EMPTY.tableName}.${FlightColumns.DATE.name} > ?
                            OR (
                                ${FlightData.EMPTY.tableName}.${FlightColumns.DATE.name} = ? AND ${FlightData.EMPTY.tableName}.${FlightColumns.TIME.name} >= ?
                            )) AND (
                                ${FlightData.EMPTY.tableName}.${FlightColumns.DATE.name} < ?
                            OR (
                                ${FlightData.EMPTY.tableName}.${FlightColumns.DATE.name} = ? AND ${FlightData.EMPTY.tableName}.${FlightColumns.TIME.name} <= ?
                            ))
                        )
                    """,
                    whereArgs = listOf(
                        startTime.toLocalDate(),
                        startTime.toLocalDate(),
                        startTime.toLocalTime(),
                        endTime.toLocalDate(),
                        endTime.toLocalDate(),
                        endTime.toLocalTime()
                    ),
                    notExists = true
                )
            )

            if (availableStaff.isEmpty()) return StaffAssignmentResults(flightId, listOf(), listOf(), "No available staff")

            val availablePilots : List<QueryResult<StaffData>> = availableStaff.filter { it.dataClass.positionId == pilotRole.id || it.dataClass.positionId == copilotRole.id }
            val availableAttendants : List<QueryResult<StaffData>> = availableStaff.filter { it.dataClass.positionId == attendantRole.id }
            val sortedPilots : List<ScoredStaffData> = availablePilots.map { ScoredStaffData(it.dataClass, 0) }
            val sortedAttendants : List<ScoredStaffData> = availableAttendants.map { ScoredStaffData(it.dataClass, 0) }

            return StaffAssignmentResults(
                flightId,
                sortedPilots.take(pilots).map{ it.staffData.id },
                sortedAttendants.take(attendants).map{ it.staffData.id },
                null
            )
        }

        fun createFlight (
            routeId : Int,
            modelId : Int,
            date : LocalDate,
            time : LocalTime
        ) : CreateFlightResults {
            val route : RouteData = RouteData.queryDatabase(routeId).firstOrNull()?.dataClass ?: return CreateFlightResults(returnMessage = "Could not find route")
            val model : PlaneModelData = PlaneModelData.queryDatabase(modelId).firstOrNull()?.dataClass ?: return CreateFlightResults(returnMessage = "Could not find plane model")
            val plane : PlaneData = getAvailablePlane(modelId, route.startDestination, date, time) ?: return CreateFlightResults(returnMessage = "Could not find an available plane")

            val flightId = FlightData(
                planeId = plane.id,
                routeId = routeId,
                date = date,
                time = time
            ).insertIntoDatabase()

            SeatData.generateSeatsForFlight(flightId)
            val staffResults : StaffAssignmentResults = assignStaffToFlight(flightId, model.pilots, model.attendants)

            return CreateFlightResults (
                flightId = flightId,
                staffResults = staffResults
            )
        }
    }
}
