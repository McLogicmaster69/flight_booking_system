package data

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
            whereArgs : WhereArgs? = null) : List<QueryResult<AssignedFlightStaffData>> {
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
    }
}
