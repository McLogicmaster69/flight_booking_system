package data

object RouteColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val START_DESTINATION = Column<Int>("start_destination", "INTEGER NOT NULL REFERENCES destinations(id)")
    val END_DESTINATION = Column<Int>("end_destination", "INTEGER NOT NULL REFERENCES destinations(id)")
    val PLANE_ID = Column<Int>("plane_id", "INTEGER NOT NULL REFERENCES planes(id)")

    val ALL = listOf(ID, START_DESTINATION, END_DESTINATION, PLANE_ID)
}

data class RouteData(

    val id: Int = 0,
    var startDestination : Int = 0,
    var endDestination : Int = 0,
    var planeId : Int = 0

) : DataClass<RouteData>() {

    override val tableName = "routes"
    override val tableColumns = RouteColumns.ALL

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            RouteColumns.START_DESTINATION to startDestination,
            RouteColumns.END_DESTINATION to endDestination,
            RouteColumns.PLANE_ID to planeId
        )

    override fun mapRowToData(row : Array<Any?>) : RouteData =
        RouteData(
            id = castRowElement(row, RouteColumns.ID),
            startDestination = castRowElement(row, RouteColumns.START_DESTINATION),
            endDestination = castRowElement(row, RouteColumns.END_DESTINATION),
            planeId = castRowElement(row, RouteColumns.PLANE_ID)
        )

    override fun debugData() {
        println("Route data: (\"$id\", \"$startDestination\", \"$endDestination\", \"$planeId\")")
    }

    companion object {
        val EMPTY : RouteData
            get() = RouteData()

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null) : List<QueryResult<RouteData>> {
            return EMPTY.queryDatabase(joinArgs, whereArgs)
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)
    }
}
