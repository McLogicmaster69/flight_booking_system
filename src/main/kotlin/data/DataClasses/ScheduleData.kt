package data

import java.time.LocalDate
import java.time.LocalTime
import java.time.Duration
import results.CreateFlightResults

const val DAYS_IN_ADVANCE = 14

object ScheduleColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val ROUTE_ID = Column<Int>("route_id", "INTEGER NOT NULL REFERENCES ${RouteData.EMPTY.tableName}(id)")
    val MODEL_ID = Column<Int>("model_id", "INTEGER NOT NULL REFERENCES ${PlaneModelData.EMPTY.tableName}(id)")
    val ACTIVE = Column<Boolean>("active", "BOOL NOT NULL")
    val START_DATE = Column<String>("date", "STRING NOT NULL")
    val DAY_GAP = Column<Int>("day_gap", "INTEGER NOT NULL")
    val TIME = Column<String>("time", "STRING NOT NULL")

    val ALL = listOf(ID, ROUTE_ID, MODEL_ID, ACTIVE, START_DATE, DAY_GAP, TIME)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class ScheduleData(
    override val id: Int = 0,
    var routeId: Int = 0,
    var modelId: Int = 0,
    var active: Boolean = true,
    var startDate: LocalDate = LocalDate.parse("1970-01-01"),
    var dayGap: Int = 1,
    var time: LocalTime = LocalTime.parse("00:00"),
) : DataClass<ScheduleData>(id) {
    override val tableName = "schedules"
    override val tableColumns = ScheduleColumns.ALL
    override val tableAdditionalSQL = """UNIQUE (
        ${ScheduleColumns.ROUTE_ID.name},
        ${ScheduleColumns.MODEL_ID.name},
        ${ScheduleColumns.START_DATE.name},
        ${ScheduleColumns.DAY_GAP.name},
        ${ScheduleColumns.TIME.name}
    )"""

    override val indexes: List<IndexArgs> =
        listOf(
            IndexArgs("inx_schedules_route_id", ScheduleColumns.ROUTE_ID.name),
            IndexArgs("inx_schedules_plane_id", ScheduleColumns.MODEL_ID.name),
            IndexArgs("inx_schedules_start_date", ScheduleColumns.START_DATE.name),
        )

    override val initialRows: List<ScheduleData>
        get() =
            listOf(
                ScheduleData(
                    routeId =
                        RouteData.getRouteId(
                            DestinationArgs(
                                DestinationData.getDestinationId("Luton"),
                                DestinationData.getDestinationId("Tokyo"),
                            ),
                        ),
                    modelId = PlaneModelData.getPlaneModelId("Boeing 737-800"),
                    active = true,
                    startDate = LocalDate.parse("2026-05-15"),
                    dayGap = 5,
                    time = LocalTime.parse("12:00"),
                ),
                ScheduleData(
                    routeId =
                        RouteData.getRouteId(
                            DestinationArgs(
                                DestinationData.getDestinationId("Tokyo"),
                                DestinationData.getDestinationId("Luton"),
                            ),
                        ),
                    modelId = PlaneModelData.getPlaneModelId("Boeing 737-800"),
                    active = true,
                    startDate = LocalDate.parse("2026-05-16"),
                    dayGap = 4,
                    time = LocalTime.parse("12:00"),
                ),
                ScheduleData(
                    routeId =
                        RouteData.getRouteId(
                            DestinationArgs(
                                DestinationData.getDestinationId("Luton"),
                                DestinationData.getDestinationId("Berlin"),
                            ),
                        ),
                    modelId = PlaneModelData.getPlaneModelId("Airbus A321"),
                    active = true,
                    startDate = LocalDate.parse("2026-05-15"),
                    dayGap = 2,
                    time = LocalTime.parse("10:00"),
                ),
                ScheduleData(
                    routeId =
                        RouteData.getRouteId(
                            DestinationArgs(
                                DestinationData.getDestinationId("Berlin"),
                                DestinationData.getDestinationId("Tokyo"),
                            ),
                        ),
                    modelId = PlaneModelData.getPlaneModelId("Boeing 737-800"),
                    active = true,
                    startDate = LocalDate.parse("2026-05-15"),
                    dayGap = 6,
                    time = LocalTime.parse("16:00"),
                ),
                ScheduleData(
                    routeId =
                        RouteData.getRouteId(
                            DestinationArgs(
                                DestinationData.getDestinationId("Berlin"),
                                DestinationData.getDestinationId("Luton"),
                            ),
                        ),
                    modelId = PlaneModelData.getPlaneModelId("Airbus A321"),
                    active = true,
                    startDate = LocalDate.parse("2026-05-13"),
                    dayGap = 2,
                    time = LocalTime.parse("16:00"),
                ),
                ScheduleData(
                    routeId =
                        RouteData.getRouteId(
                            DestinationArgs(
                                DestinationData.getDestinationId("Tokyo"),
                                DestinationData.getDestinationId("Berlin"),
                            ),
                        ),
                    modelId = PlaneModelData.getPlaneModelId("Boeing 737-800"),
                    active = true,
                    startDate = LocalDate.parse("2026-05-09"),
                    dayGap = 6,
                    time = LocalTime.parse("16:00"),
                ),
            )

    override val requiredTables: List<DataClass<*>>
        get() =
            listOf(
                RouteData.EMPTY,
                PlaneModelData.EMPTY,
            )

    override fun mapDataToColumns(): Map<Column<*>, Any?> =
        mapOf(
            ScheduleColumns.ROUTE_ID to routeId,
            ScheduleColumns.MODEL_ID to modelId,
            ScheduleColumns.ACTIVE to active,
            ScheduleColumns.START_DATE to startDate.toString(),
            ScheduleColumns.DAY_GAP to dayGap,
            ScheduleColumns.TIME to time.toString(),
        )

    override fun mapRowToData(row: Array<Any?>): ScheduleData =
        ScheduleData(
            id = castRowElement(row, ScheduleColumns.ID),
            routeId = castRowElement(row, ScheduleColumns.ROUTE_ID),
            modelId = castRowElement(row, ScheduleColumns.MODEL_ID),
            active = anyToBool(castRowElement(row, ScheduleColumns.ACTIVE))!!,
            startDate = castDateRowElement(row, ScheduleColumns.START_DATE),
            dayGap = castRowElement(row, ScheduleColumns.DAY_GAP),
            time = castTimeRowElement(row, ScheduleColumns.TIME),
        )

    override fun debugData() {
        println(
            "Schedule data: (\"$id\", \"$routeId\", \"$modelId\", \"$active\", \"$startDate\", \"$dayGap\", \"$time\")",
        )
    }

    fun updateFlights(): Boolean {
        println("Update flights for schedule id $id")
        if (dayGap < 1) return false

        val nextActualFlightDate: LocalDate =
            if (startDate.isBefore(LocalDate.now())) {
                LocalDate.now().plusDays(
                    dayGap - (
                        Duration
                            .between(
                                startDate.atStartOfDay(),
                                LocalDate.now().atStartOfDay(),
                            ).toDays() % dayGap
                    ),
                )
            } else {
                startDate
            }

        var dayShift: Long = 0L
        var changeMade: Boolean = false

        while (
            Duration
                .between(
                    LocalDate.now().atStartOfDay(),
                    nextActualFlightDate.plusDays(dayShift).atStartOfDay(),
                ).toDays() <= DAYS_IN_ADVANCE
        ) {
            val flightDate = nextActualFlightDate.plusDays(dayShift)
            println("Creating flight on $flightDate")

            val flights: List<QueryResult<FlightData>> =
                FlightData.queryDatabase(
                    whereArgs =
                        WhereArgs(
                            """
                            ${FlightColumns.SCHEDULE_ID.name} = ?
                            AND ${FlightColumns.DATE.name} = ?
                            """,
                            listOf(id, flightDate),
                        ),
                )

            if (flights.size > 0) {
                println("Flight already exists, skipping")
            } else {
                val results: CreateFlightResults =
                    FlightData.createFlight(
                        routeId,
                        modelId,
                        flightDate,
                        time,
                        id,
                    )

                if (results.returnMessage != null) {
                    println("Flight creation results are: ${results.returnMessage}")
                } else if (results.staffResults != null) {
                    println("Flight staff assignment results are: ${results.staffResults.returnMessage}")
                    changeMade = true
                } else {
                    println("Flight was created successfully")
                    changeMade = true
                }
            }

            println("")
            dayShift += dayGap
        }

        return changeMade
    }

    companion object {
        val EMPTY: ScheduleData
            get() = ScheduleData()

        fun queryDatabase(
            multipleJoinArgs: MultipleJoinArgs? = null,
            whereArgs: WhereArgs? = null,
            orderByArgs: OrderByArgs? = null,
            limitArgs: LimitArgs? = null,
            groupByArgs: GroupByArgs? = null,
        ): List<QueryResult<ScheduleData>> =
            EMPTY.queryDatabase(multipleJoinArgs, whereArgs, orderByArgs, limitArgs, groupByArgs)

        fun queryDatabase(id: Int): List<QueryResult<ScheduleData>> =
            queryDatabase(whereArgs = WhereArgs("${ScheduleColumns.ID.name} = ?", listOf(id)))

        fun updateTable(
            values: Map<Column<*>, Any?>,
            whereArgs: WhereArgs,
        ): Int = EMPTY.updateTable(values, whereArgs)

        fun delete(id: Int): Int = ScheduleData(id = id).delete()

        fun updateAllFlights() {
            println("Creating flights from schedules")

            val schedules: List<QueryResult<ScheduleData>> =
                queryDatabase(
                    whereArgs =
                        WhereArgs(
                            "${ScheduleColumns.ACTIVE.name} = ?",
                            listOf(true),
                        ),
                )

            println("${schedules.size} active schedules found")
            println("")

            var changeMade = false
            var loops = 0

            do {
                changeMade = false
                schedules.forEach { schedule ->
                    val changed = schedule.dataClass.updateFlights()
                    if (changed) changeMade = true
                }
                loops += 1
            } while (changeMade == true && loops < 5)
        }
    }
}
