package data

import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime
import java.time.Duration
import kotlin.math.roundToInt
import results.CreateFlightResults
import results.StaffAssignmentResults
import data.FlightColumns

const val TRANSFER_TIME : Int = 120
const val MAX_TIME : Int = 2880

object FlightColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val SCHEDULE_ID = Column<Int>("schedule_id", "INTEGER REFERENCES ${ScheduleData.EMPTY.tableName}(id)")
    val ROUTE_ID = Column<Int>("route_id", "INTEGER NOT NULL REFERENCES ${RouteData.EMPTY.tableName}(id)")
    val PLANE_ID = Column<Int>("plane_id", "INTEGER NOT NULL REFERENCES ${PlaneData.EMPTY.tableName}(id)")
    val DATE = Column<String>("date", "STRING NOT NULL")
    val TIME = Column<String>("time", "STRING NOT NULL")

    val ALL = listOf(ID, SCHEDULE_ID, ROUTE_ID, PLANE_ID, DATE, TIME)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class FlightData(

    override val id: Int = 0,
    var scheduleId : Int? = null,
    var routeId : Int = 0,
    var planeId : Int = 0,
    var date : LocalDate = LocalDate.parse("1970-01-01"),
    var time : LocalTime = LocalTime.parse("00:00")

) : DataClass<FlightData>(id) {

    override val tableName = "flights"
    override val tableColumns = FlightColumns.ALL
    override val tableAdditionalSQL = "UNIQUE (${FlightColumns.ROUTE_ID.name}, ${FlightColumns.PLANE_ID.name}, ${FlightColumns.DATE.name}, ${FlightColumns.TIME.name})"

    override val indexes : List<IndexArgs> = listOf(
        IndexArgs("inx_flights_schedule_id", FlightColumns.SCHEDULE_ID.name),
        IndexArgs("inx_flights_route_id", FlightColumns.ROUTE_ID.name),
        IndexArgs("inx_flights_plane_id", FlightColumns.PLANE_ID.name),
        IndexArgs("inx_flights_date", FlightColumns.DATE.name),
        IndexArgs("inx_flights_date_time", "${FlightColumns.DATE.name}, ${FlightColumns.TIME.name}")
    )

    override val initialRows : List<FlightData>
        get() = listOf(
        )

    override val requiredTables : List<DataClass<*>>
        get() = listOf(
            RouteData.EMPTY,
            PlaneData.EMPTY,
            DestinationData.EMPTY,
            ScheduleData.EMPTY
        )

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            FlightColumns.SCHEDULE_ID to scheduleId,
            FlightColumns.ROUTE_ID to routeId,
            FlightColumns.PLANE_ID to planeId,
            FlightColumns.DATE to date.toString(),
            FlightColumns.TIME to time.toString()
        )

    override fun mapRowToData(row : Array<Any?>) : FlightData =
        FlightData(
            id = castRowElement(row, FlightColumns.ID),
            scheduleId = castRowElement(row, FlightColumns.SCHEDULE_ID),
            routeId = castRowElement(row, FlightColumns.ROUTE_ID),
            planeId = castRowElement(row, FlightColumns.PLANE_ID),
            date = castDateRowElement(row, FlightColumns.DATE),
            time = castTimeRowElement(row, FlightColumns.TIME)
        )

    override fun debugData() {
        println("Flight data: (\"$id\", \"$scheduleId\", \"$routeId\", \"$planeId\", \"$date\", \"$time\")")
    }

    companion object {
        val EMPTY : FlightData
            get() = FlightData()

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null,
            orderByArgs : OrderByArgs? = null,
            limitArgs : LimitArgs? = null        
        ) : List<QueryResult<FlightData>> {
            return EMPTY.queryDatabase(joinArgs, whereArgs, orderByArgs, limitArgs)
        }

        fun queryDatabase (
            routeIds : List<Int>,
            joinArgs : JoinArgs? = null,
            orderByArgs : OrderByArgs? = null,
            limitArgs : LimitArgs? = null 
        ) : List<QueryResult<FlightData>> {
            val whereClause = routeIds.joinToString(" OR ") { "${FlightColumns.ROUTE_ID.name} = ?" }
            val whereArgs = routeIds.map { it }
            return EMPTY.queryDatabase(joinArgs, WhereArgs(whereClause, whereArgs), orderByArgs, limitArgs)
        }

        fun queryDatabase (
            routeIds : List<Int>,
            date : LocalDate,
            joinArgs : JoinArgs? = null,
            orderByArgs : OrderByArgs? = null,
            limitArgs : LimitArgs? = null  
        ) : List<QueryResult<FlightData>> {
            val whereClause = "(" + routeIds.joinToString(" OR ") { "${FlightColumns.ROUTE_ID.name} = ?" } + ") AND ${FlightColumns.DATE.name} = ?"
            val whereArgs = routeIds.map { it } + listOf(date.toString())
            return EMPTY.queryDatabase(joinArgs, WhereArgs(whereClause, whereArgs), orderByArgs, limitArgs)
        }

        fun queryDatabase (
            destinationArgs : DestinationArgs,
            joinArgs : JoinArgs? = null,
            orderByArgs : OrderByArgs? = null,
            limitArgs : LimitArgs? = null  
        ) : List<QueryResult<FlightData>> {
            val routes : List<QueryResult<RouteData>> = RouteData.queryDatabase(destinationArgs, joinArgs)
            val routeIds : List<Int> = routes.map { route ->
                route.dataClass.id
            }
            return queryDatabase(routeIds, joinArgs, orderByArgs, limitArgs)
        }

        fun queryDatabase (
            destinationArgs : DestinationArgs,
            date : LocalDate,
            joinArgs : JoinArgs? = null,
            orderByArgs : OrderByArgs? = null,
            limitArgs : LimitArgs? = null  
        ) : List<QueryResult<FlightData>> {
            val routes : List<QueryResult<RouteData>> = RouteData.queryDatabase(destinationArgs, joinArgs)
            val routeIds : List<Int> = routes.map { route ->
                route.dataClass.id
            }
            return queryDatabase(routeIds, date, joinArgs, orderByArgs, limitArgs)
        }

        fun queryDatabase (
            destinationArgs : DestinationArgs,
            dateTime : LocalDateTime,
            joinArgs : JoinArgs? = null,
            orderByArgs : OrderByArgs? = null,
            limitArgs : LimitArgs? = null  
        ) : List<QueryResult<FlightData>> {
            return queryDatabase(destinationArgs, dateTime.toLocalDate(), joinArgs, orderByArgs, limitArgs)
        }

        fun queryDatabase(id : Int) : List<QueryResult<FlightData>> = queryDatabase(whereArgs = WhereArgs("${FlightColumns.ID.name} = ?", listOf(id)))

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)

        fun delete(id : Int) : Int {
            return FlightData(id = id).delete()
        }

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

        fun createFlight (
            routeId : Int,
            modelId : Int,
            date : LocalDate,
            time : LocalTime,
            scheduleId : Int? = null
        ) : CreateFlightResults {
            val route : RouteData = RouteData.queryDatabase(routeId).firstOrNull()?.dataClass ?: return CreateFlightResults(returnMessage = "Could not find route")
            val model : PlaneModelData = PlaneModelData.queryDatabase(modelId).firstOrNull()?.dataClass ?: return CreateFlightResults(returnMessage = "Could not find plane model")
            val plane : PlaneData = PlaneData.getAvailablePlane(modelId, route.startDestination, date, time) ?: return CreateFlightResults(returnMessage = "Could not find an available plane")

            val flightId = FlightData(
                scheduleId = scheduleId,
                planeId = plane.id,
                routeId = routeId,
                date = date,
                time = time
            ).insertIntoDatabase()

            val arrivalDateTime : LocalDateTime = LocalDateTime.of(date, time).plusMinutes(route.duration.hour * 60L + route.duration.minute + 60L)
            plane.updateLocation(route.endDestination, arrivalDateTime.toLocalDate(), arrivalDateTime.toLocalTime())

            SeatData.generateSeatsForFlight(flightId)
            val staffResults : StaffAssignmentResults = AssignedFlightStaffData.assignStaffToFlight(flightId, model.pilots, model.attendants)

            return CreateFlightResults (
                flightId = flightId,
                staffResults = staffResults
            )
        }

        fun deleteOld() {
            val whereArgs = WhereArgs(
                whereClause = "${FlightColumns.DATE.name} < ?",
                whereArgs = listOf(LocalDate.now().minusYears(1L))
            )

            val query : List<QueryResult<FlightData>> = queryDatabase(whereArgs = whereArgs)

            query.forEach { flight ->
                flight.dataClass.delete()
            }
        }
    }
}
