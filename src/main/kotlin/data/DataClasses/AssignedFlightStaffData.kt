package data

import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime
import results.StaffAssignmentResults

object AssignedFlightStaffColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val FLIGHT_ID = Column<Int>("flight_id", "INTEGER NOT NULL REFERENCES flights(id)")
    val STAFF_ID = Column<Int>("staff_id", "INTEGER NOT NULL REFERENCES staff(id)")

    val ALL = listOf(ID, FLIGHT_ID, STAFF_ID)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class AssignedFlightStaffData(

    override val id: Int = 0,
    var flightId : Int = 0,
    var staffId : Int = 0

) : DataClass<AssignedFlightStaffData>(id) {

    override val tableName = "assigned_flight_staff"
    override val tableColumns = AssignedFlightStaffColumns.ALL

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            AssignedFlightStaffColumns.FLIGHT_ID to flightId,
            AssignedFlightStaffColumns.STAFF_ID to staffId
        )

    override fun mapRowToData(row : Array<Any?>) : AssignedFlightStaffData =
        AssignedFlightStaffData(
            id = castRowElement(row, AssignedFlightStaffColumns.ID),
            flightId = castRowElement(row, AssignedFlightStaffColumns.FLIGHT_ID),
            staffId = castRowElement(row, AssignedFlightStaffColumns.STAFF_ID)
        )

    override fun debugData() {
        println("Assigned Flight Staff data: (\"$id\", \"$flightId\", \"$staffId\")")
    }

    companion object {
        val EMPTY : AssignedFlightStaffData
            get() = AssignedFlightStaffData()

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null
        ) : List<QueryResult<AssignedFlightStaffData>> {
            return EMPTY.queryDatabase(joinArgs, whereArgs)
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)

        fun delete(id : Int) : Int {
            return AssignedFlightStaffData(id = id).delete()
        }

        fun queryByFlightID (
            id : Int
        ) : List<QueryResult<AssignedFlightStaffData>> 
            = queryDatabase(whereArgs = WhereArgs("${AssignedFlightStaffColumns.FLIGHT_ID.name} = ?", listOf(id)))

        fun queryByStaffID (
            id : Int
        ) : List<QueryResult<AssignedFlightStaffData>> 
            = queryDatabase(whereArgs = WhereArgs("${AssignedFlightStaffColumns.STAFF_ID.name} = ?", listOf(id)))

        fun scoreStaffMember (
            staff : StaffData,
            flight : FlightData,
            route : RouteData
        ) : Int {
            return 0
        }

        fun assignStaffToFlight (
            flightId : Int,
            pilots : Int,
            attendants : Int
        ) : StaffAssignmentResults {
            val flight : FlightData = FlightData.queryDatabase(flightId).firstOrNull()?.dataClass ?: return StaffAssignmentResults(flightId, listOf(), listOf(), "Could not find flight")
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

            val sortedPilots : List<QueryResult<StaffData>> = availablePilots.mapNotNull { pilot ->
                val score = scoreStaffMember(pilot.dataClass, flight, route)
                if (score >= 0) pilot to score else null
            } .sortedBy { it.second } .map { it.first }

            val sortedAttendants : List<QueryResult<StaffData>> = availableAttendants.mapNotNull { attendant ->
                val score = scoreStaffMember(attendant.dataClass, flight, route)
                if (score >= 0) attendant to score else null
            } .sortedBy { it.second } .map { it.first }

            val selectedPilots : List<QueryResult<StaffData>> = sortedPilots.take(pilots)
            val selectedAttendants : List<QueryResult<StaffData>> = sortedAttendants.take(attendants)

            selectedPilots.forEach { pilot ->
                AssignedFlightStaffData(
                    flightId = flight.id,
                    staffId = pilot.dataClass.id
                ).insertIntoDatabase() 
            }

            selectedAttendants.forEach { attendant ->
                AssignedFlightStaffData(
                    flightId = flight.id,
                    staffId = attendant.dataClass.id
                ).insertIntoDatabase() 
            }

            return StaffAssignmentResults(
                flightId,
                selectedPilots.map{ it.dataClass.id },
                selectedAttendants.map{ it.dataClass.id },
                null
            )
        }
    }
}
