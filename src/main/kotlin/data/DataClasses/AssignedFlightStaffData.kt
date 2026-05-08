package data

import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime
import java.time.Duration
import results.StaffAssignmentResults

/**
 * Minimum required rest time (in hours) between two flights
 * for a staff member to be considered available.
 */
const val HOURS_BETWEEN_FLIGHT: Int = 3

/**
 * Database column definitions for the assigned_flight_staff table.
 *
 * This table represents the assignment of staff members to flights,
 * including a secure search token for lookup or validation.
 */
object AssignedFlightStaffColumns {
    /** Primary key */
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")

    /** Foreign key referencing the flight */
    val FLIGHT_ID = Column<Int>("flight_id", "INTEGER NOT NULL REFERENCES ${FlightData.EMPTY.tableName}(id)")

    /** Foreign key referencing the staff member */
    val STAFF_ID = Column<Int>("staff_id", "INTEGER NOT NULL REFERENCES ${StaffData.EMPTY.tableName}(id)")

    /** Secure token used for searching or validation */
    val SEARCH_TOKEN = Column<String>("search_token", "STRING NOT NULL")

    /** All columns in this table */
    val ALL = listOf(ID, FLIGHT_ID, STAFF_ID, SEARCH_TOKEN)

    /** Column names only, in declaration order */
    val COLUMN_NAMES = ALL.map { it.name }
}

/**
 * Represents an assignment of a staff member to a flight.
 *
 * @property id Primary key
 * @property flightId ID of the flight
 * @property staffId ID of the staff member
 * @property searchToken Secure token associated with the assignment
 */
data class AssignedFlightStaffData(
    override val id: Int = 0,
    var flightId: Int = 0,
    var staffId: Int = 0,
    var searchToken: String = "",
) : DataClass<AssignedFlightStaffData>(id) {
    /** Name of the database table */
    override val tableName = "assigned_flight_staff"

    /** Columns used by the table */
    override val tableColumns = AssignedFlightStaffColumns.ALL

    /**
     * Indexes to improve lookup performance by flight or staff member.
     */
    override val indexes: List<IndexArgs> =
        listOf(
            IndexArgs("inx_assigned_flight_staff_flight_id", AssignedFlightStaffColumns.FLIGHT_ID.name),
            IndexArgs("inx_assigned_flight_staff_staff_id", AssignedFlightStaffColumns.STAFF_ID.name),
        )

    /**
     * Maps this data class into database column values.
     */
    override fun mapDataToColumns(): Map<Column<*>, Any?> =
        mapOf(
            AssignedFlightStaffColumns.FLIGHT_ID to flightId,
            AssignedFlightStaffColumns.STAFF_ID to staffId,
            AssignedFlightStaffColumns.SEARCH_TOKEN to searchToken,
        )

    /**
     * Converts a database row into an AssignedFlightStaffData instance.
     */
    override fun mapRowToData(row: Array<Any?>): AssignedFlightStaffData =
        AssignedFlightStaffData(
            id = castRowElement(row, AssignedFlightStaffColumns.ID),
            flightId = castRowElement(row, AssignedFlightStaffColumns.FLIGHT_ID),
            staffId = castRowElement(row, AssignedFlightStaffColumns.STAFF_ID),
            searchToken = castRowElement(row, AssignedFlightStaffColumns.SEARCH_TOKEN),
        )

    /**
     * Prints the contents of this object for debugging purposes.
     */
    override fun debugData() {
        println("Assigned Flight Staff data: (\"$id\", \"$flightId\", \"$staffId\", \"$searchToken\")")
    }

    companion object {
        /**
         * Empty instance used for static-style database operations.
         */
        val EMPTY: AssignedFlightStaffData
            get() = AssignedFlightStaffData()

        /**
         * Queries the database with optional joins, filters, ordering, grouping, and limits.
         */
        fun queryDatabase(
            multipleJoinArgs: MultipleJoinArgs? = null,
            whereArgs: WhereArgs? = null,
            orderByArgs: OrderByArgs? = null,
            limitArgs: LimitArgs? = null,
            groupByArgs: GroupByArgs? = null,
        ): List<QueryResult<AssignedFlightStaffData>> =
            EMPTY.queryDatabase(multipleJoinArgs, whereArgs, orderByArgs, limitArgs, groupByArgs)

        /**
         * Queries assignments by search token.
         */
        fun queryDatabase(token: String): List<QueryResult<AssignedFlightStaffData>> =
            queryDatabase(whereArgs = WhereArgs("${AssignedFlightStaffColumns.SEARCH_TOKEN} = ?", listOf(token)))

        /**
         * Updates records in the table matching the given conditions.
         */
        fun updateTable(
            values: Map<Column<*>, Any?>,
            whereArgs: WhereArgs,
        ): Int = EMPTY.updateTable(values, whereArgs)

        /**
         * Deletes an assignment by ID.
         */
        fun delete(id: Int): Int = AssignedFlightStaffData(id = id).delete()

        /**
         * Retrieves all staff assignments for a given flight.
         */
        fun queryByFlightID(id: Int): List<QueryResult<AssignedFlightStaffData>> =
            queryDatabase(whereArgs = WhereArgs("${AssignedFlightStaffColumns.FLIGHT_ID.name} = ?", listOf(id)))

        /**
         * Retrieves all flight assignments for a given staff member.
         */
        fun queryByStaffID(id: Int): List<QueryResult<AssignedFlightStaffData>> =
            queryDatabase(whereArgs = WhereArgs("${AssignedFlightStaffColumns.STAFF_ID.name} = ?", listOf(id)))

        /**
         * Retrieves all upcoming flights assigned to a specific staff member.
         */
        fun getFlightsAssignedToStaff(id: Int): List<QueryResult<FlightData>> {
            val joinArgs: MultipleJoinArgs =
                MultipleJoinArgs(
                    listOf(
                        JoinArgs(
                            "INNER",
                            AssignedFlightStaffData.EMPTY.tableName,
                            FlightColumns.ID.name,
                            AssignedFlightStaffColumns.FLIGHT_ID.name,
                            AssignedFlightStaffColumns.COLUMN_NAMES,
                            FlightData.EMPTY.tableName,
                        ),
                    ),
                )

            val whereArgs: WhereArgs =
                WhereArgs(
                    whereClause = """
                    ${AssignedFlightStaffData.EMPTY.tableName}.${AssignedFlightStaffColumns.STAFF_ID.name} = ?
                    AND ${FlightData.EMPTY.tableName}.${FlightColumns.DATE.name} >= ?
                """,
                    listOf(id, LocalDate.now().minusDays(1L)),
                )

            return FlightData.queryDatabase(
                multipleJoinArgs = joinArgs,
                whereArgs = whereArgs,
            )
        }

        /**
         * Calculates a suitability score for assigning a staff member to a flight.
         *
         * The score is based on:
         * - Home country match
         * - Time since last assignment
         * - Number of recent assignments
         */
        fun scoreStaffMember(
            staff: StaffData,
            flight: FlightData,
            route: RouteData,
        ): Int {
            var points: Int = 20

            val endCountry =
                DestinationData
                    .queryDatabase(route.endDestination)
                    .firstOrNull()
                    ?.dataClass
                    ?.id ?: -2

            val pastAssignments: List<QueryResult<AssignedFlightStaffData>> =
                queryDatabase(
                    multipleJoinArgs =
                        MultipleJoinArgs(
                            listOf(
                                JoinArgs(
                                    joinType = "INNER",
                                    rightTableJoin = FlightData.EMPTY.tableName,
                                    leftTableJoinColumn = AssignedFlightStaffColumns.FLIGHT_ID.name,
                                    rightTableJoinColumn = FlightColumns.ID.name,
                                    joinSelectColumns = FlightColumns.COLUMN_NAMES,
                                ),
                            ),
                        ),
                    whereArgs =
                        WhereArgs(
                            whereClause = """
                        ${AssignedFlightStaffData.EMPTY.tableName}.${AssignedFlightStaffColumns.STAFF_ID.name} = ?
                        AND ${FlightData.EMPTY.tableName}.${FlightColumns.DATE.name} >= ?
                    """,
                            whereArgs =
                                listOf(
                                    staff.id,
                                    flight.date.minusDays(14L),
                                ),
                        ),
                    orderByArgs =
                        OrderByArgs(
                            orderArgs =
                                listOf(
                                    OrderArgs("${FlightData.EMPTY.tableName}.${FlightColumns.DATE.name}", false),
                                    OrderArgs("${FlightData.EMPTY.tableName}.${FlightColumns.TIME.name}", false),
                                ),
                        ),
                )

            if (staff.homeId == endCountry) points += 30

            if (pastAssignments.isEmpty()) {
                points += 25
            } else {
                points += Duration
                    .between(
                        LocalDateTime.of(flight.date, flight.time),
                        LocalDateTime.of(
                            LocalDate.parse(
                                pastAssignments
                                    .first()
                                    .getColumn(FlightData.EMPTY.tableName, FlightColumns.DATE.name)!!
                                    .columnVal as String,
                            ),
                            LocalTime.parse(
                                pastAssignments
                                    .first()
                                    .getColumn(FlightData.EMPTY.tableName, FlightColumns.TIME.name)!!
                                    .columnVal as String,
                            ),
                        ),
                    ).toHours()
                    .toInt() / 2

                points -= pastAssignments.size
            }

            return points
        }

        /**
         * Automatically assigns pilots and flight attendants to a flight
         * based on availability and scoring rules.
         *
         * @return A StaffAssignmentResults object describing the outcome.
         */
        fun assignStaffToFlight(
            flightId: Int,
            pilots: Int,
            attendants: Int,
        ): StaffAssignmentResults {
            val flight: FlightData =
                FlightData.queryDatabase(flightId).firstOrNull()?.dataClass
                    ?: return StaffAssignmentResults(flightId, listOf(), listOf(), "Could not find flight")
            val route: RouteData =
                RouteData.queryDatabase(flight.routeId).firstOrNull()?.dataClass
                    ?: return StaffAssignmentResults(flightId, listOf(), listOf(), "Could not find route")
            val pilotRole: StaffPositionData =
                StaffPositionData.queryDatabase(StaffPositions.PILOT).firstOrNull()?.dataClass
                    ?: return StaffAssignmentResults(flightId, listOf(), listOf(), "Could not find pilot role")
            val copilotRole: StaffPositionData =
                StaffPositionData.queryDatabase(StaffPositions.COPILOT).firstOrNull()?.dataClass
                    ?: return StaffAssignmentResults(flightId, listOf(), listOf(), "Could not find copilot role")
            val attendantRole: StaffPositionData =
                StaffPositionData.queryDatabase(StaffPositions.FLIGHT_ATTENDANT).firstOrNull()?.dataClass
                    ?: return StaffAssignmentResults(flightId, listOf(), listOf(), "Could not find attendant role")

            val endTime: LocalDateTime =
                LocalDateTime
                    .of(
                        flight.date,
                        flight.time,
                    ).plusMinutes(route.duration.hour * 60L + route.duration.minute + HOURS_BETWEEN_FLIGHT * 60L)

            val availableStaff: List<QueryResult<StaffData>> =
                StaffData.queryDatabase(
                    whereArgs =
                        WhereArgs(
                            whereClause = """
                        ${StaffData.EMPTY.tableName}.${StaffColumns.CURRENT_LOCATION} = ?
                        AND NOT EXISTS (
                            SELECT 1
                            FROM ${AssignedFlightStaffData.EMPTY.tableName}
                            INNER JOIN ${FlightData.EMPTY.tableName}
                            ON ${FlightData.EMPTY.tableName}.${FlightColumns.ID.name} = ${AssignedFlightStaffData.EMPTY.tableName}.${AssignedFlightStaffColumns.FLIGHT_ID.name}
                            WHERE ${AssignedFlightStaffData.EMPTY.tableName}.${AssignedFlightStaffColumns.STAFF_ID} = ${StaffData.EMPTY.tableName}.${StaffColumns.ID.name}
                            AND (
                                ${FlightData.EMPTY.tableName}.${FlightColumns.DATE.name} > ?
                                OR (
                                    ${FlightData.EMPTY.tableName}.${FlightColumns.DATE.name} = ? AND ${FlightData.EMPTY.tableName}.${FlightColumns.TIME.name} >= ?
                                )
                            )
                        )
                    """,
                            whereArgs =
                                listOf(
                                    route.startDestination,
                                    endTime.toLocalDate(),
                                    endTime.toLocalDate(),
                                    endTime.toLocalTime(),
                                ),
                        ),
                )

            if (availableStaff.isEmpty()) {
                return StaffAssignmentResults(
                    flightId,
                    listOf(),
                    listOf(),
                    "No available staff",
                )
            }

            val availablePilots: List<QueryResult<StaffData>> =
                availableStaff.filter {
                    it.dataClass.positionId ==
                        pilotRole.id ||
                        it.dataClass.positionId == copilotRole.id
                }
            val availableAttendants: List<QueryResult<StaffData>> =
                availableStaff.filter {
                    it.dataClass.positionId ==
                        attendantRole.id
                }

            val sortedPilots: List<QueryResult<StaffData>> =
                availablePilots
                    .mapNotNull { pilot ->
                        val score = scoreStaffMember(pilot.dataClass, flight, route)
                        if (score >= 0) pilot to score else null
                    }.sortedByDescending { it.second }
                    .map { it.first }

            val sortedAttendants: List<QueryResult<StaffData>> =
                availableAttendants
                    .mapNotNull { attendant ->
                        val score = scoreStaffMember(attendant.dataClass, flight, route)
                        if (score >= 0) attendant to score else null
                    }.sortedByDescending { it.second }
                    .map { it.first }

            val selectedPilots: List<QueryResult<StaffData>> = sortedPilots.take(pilots)
            val selectedAttendants: List<QueryResult<StaffData>> = sortedAttendants.take(attendants)

            selectedPilots.forEach { pilot ->
                AssignedFlightStaffData(
                    flightId = flight.id,
                    staffId = pilot.dataClass.id,
                    searchToken = EMPTY.generateSecureToken(),
                ).insertIntoDatabase()
                pilot.dataClass.updateLocation(route.endDestination)
            }

            selectedAttendants.forEach { attendant ->
                AssignedFlightStaffData(
                    flightId = flight.id,
                    staffId = attendant.dataClass.id,
                    searchToken = EMPTY.generateSecureToken(),
                ).insertIntoDatabase()
                attendant.dataClass.updateLocation(route.endDestination)
            }

            return StaffAssignmentResults(
                flightId,
                selectedPilots.map { it.dataClass.id },
                selectedAttendants.map { it.dataClass.id },
                null,
            )
        }
    }
}
