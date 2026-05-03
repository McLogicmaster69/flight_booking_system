package data

object SeatColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val FLIGHT_ID = Column<Int>("flight_id", "INTEGER NOT NULL REFERENCES ${FlightData.EMPTY.tableName}(id)")
    val CLASS_ID = Column<Int>("class_id", "INTEGER NOT NULL REFERENCES ${ClassData.EMPTY.tableName}(id)")
    val TYPE_ID = Column<Int>("type_id", "INTEGER NOT NULL REFERENCES ${TicketTypeData.EMPTY.tableName}(id)")
    val NUMBER = Column<Int>("number", "INTEGER NOT NULL")
    val PRICE = Column<Float>("price", "INTEGER NOT NULL")

    val ALL = listOf(ID, FLIGHT_ID, CLASS_ID, TYPE_ID, NUMBER, PRICE)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class SeatData(

    override val id: Int = 0,
    var flightId : Int = 0,
    var classId : Int = 0,
    var typeId : Int = 0,
    var number : Int = 0,
    var price : Float = 0f,

) : DataClass<SeatData>(id) {

    override val tableName = "seats"
    override val tableColumns = SeatColumns.ALL

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            SeatColumns.FLIGHT_ID to flightId,
            SeatColumns.CLASS_ID to classId,
            SeatColumns.TYPE_ID to typeId,
            SeatColumns.NUMBER to number,
            SeatColumns.PRICE to price
        )

    override fun mapRowToData(row : Array<Any?>) : SeatData =
        SeatData(
            id = castRowElement(row, SeatColumns.ID),
            flightId = castRowElement(row, SeatColumns.FLIGHT_ID),
            classId = castRowElement(row, SeatColumns.CLASS_ID),
            typeId = castRowElement(row, SeatColumns.TYPE_ID),
            number = castRowElement(row, SeatColumns.NUMBER),
            price = castRowElement(row, SeatColumns.PRICE)
        )

    override fun debugData() {
        println("Seat data: (\"$id\", \"$flightId\", \"$classId\", \"$typeId\", \"$number\", \"$price\")")
    }

    companion object {
        val EMPTY : SeatData
            get() = SeatData()

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null,
            orderByArgs : OrderByArgs? = null,
            limitArgs : LimitArgs? = null
        ) : List<QueryResult<SeatData>> {
            return EMPTY.queryDatabase(joinArgs, whereArgs, orderByArgs, limitArgs)
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)

        fun delete(id : Int) : Int {
            return SeatData(id = id).delete()
        }

        fun generateSeatsForFlight(flightId: Int) {
            val existingSeats = queryDatabase(
                whereArgs = WhereArgs("${SeatColumns.FLIGHT_ID.name} = ?", listOf(flightId))
            )

            if (existingSeats.isNotEmpty()) return

            val flight = FlightData.queryDatabase(
                whereArgs = WhereArgs("id = ?", listOf(flightId))
            ).firstOrNull()?.dataClass ?: return

            val plane = PlaneData.queryDatabase(
                whereArgs = WhereArgs("id = ?", listOf(flight.planeId))
            ).firstOrNull()?.dataClass ?: return

            val model = PlaneModelData.queryDatabase(
                whereArgs = WhereArgs("id = ?", listOf(plane.modelId))
            ).firstOrNull()?.dataClass ?: return

            val capacity = model.capacity

            val firstClassId = ClassData.queryDatabase(
                whereArgs = WhereArgs("${ClassColumns.NAME.name} = ?", listOf(Classes.FIRST_CLASS))
            ).firstOrNull()?.dataClass?.id ?: return

            val businessClassId = ClassData.queryDatabase(
                whereArgs = WhereArgs("${ClassColumns.NAME.name} = ?", listOf(Classes.BUSINESS))
            ).firstOrNull()?.dataClass?.id ?: return

            val economyClassId = ClassData.queryDatabase(
                whereArgs = WhereArgs("${ClassColumns.NAME.name} = ?", listOf(Classes.ECONOMY))
            ).firstOrNull()?.dataClass?.id ?: return

            val adultTypeId = TicketTypeData.queryDatabase(
                whereArgs = WhereArgs("${TicketTypeColumns.NAME.name} = ?", listOf(TicketTypes.ADULT))
            ).firstOrNull()?.dataClass?.id ?: return

            for (seatNumber in 1..capacity) {
                val classId = when {
                    seatNumber <= capacity * 0.05 -> firstClassId
                    seatNumber <= capacity * 0.20 -> businessClassId
                    else -> economyClassId
                }

                SeatData(
                    flightId = flightId,
                    classId = classId,
                    typeId = adultTypeId,
                    number = seatNumber,
                    price = 0f
                ).insertIntoDatabase()
            }
        }

        fun getAvailableSeats(flightId: Int, classId: Int): List<SeatData> {
            val seats = queryDatabase(
                whereArgs = WhereArgs(
                    "${SeatColumns.FLIGHT_ID.name} = ? AND ${SeatColumns.CLASS_ID.name} = ?",
                    listOf(flightId, classId)
                )
            ).map { it.dataClass }

            return seats.filter { seat ->
                BookedSeatData.queryDatabase(
                    whereArgs = WhereArgs("${BookedSeatColumns.SEAT_ID.name} = ?", listOf(seat.id))
                ).isEmpty()
            }
        }

        fun getRandomAvailableSeat(flightId: Int, classId: Int): SeatData? {
            return getAvailableSeats(flightId, classId).randomOrNull()
        }

        fun isSeatAvailable(seatId: Int): Boolean {
            return BookedSeatData.queryDatabase(
                whereArgs = WhereArgs("${BookedSeatColumns.SEAT_ID.name} = ?", listOf(seatId))
            ).isEmpty()
        }
    }
}
