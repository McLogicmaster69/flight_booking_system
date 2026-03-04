package data

object PlaneModelColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val CAPACITY = Column<Int?>("passport_number", "INTEGER")
    val NAME = Column<String>("booking_reference", "STRING NOT NULL")
    val MANUFACTURER_ID = Column<Int>("booker_id", "INTEGER NOT NULL REFERENCES manufacturers(id)")

    val ALL = listOf(ID, CAPACITY, NAME, MANUFACTURER_ID)
}

data class PlaneModelData(

    val id : Int = 0,
    var capacity : Int? = null,
    var name : String = "",
    var manufacturerId : Int = 0

) : DataClass<PlaneModelData>() {

    override val tableName = "plane_models"
    override val tableColumns = PlaneModelColumns.ALL

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            PlaneModelColumns.CAPACITY to capacity,
            PlaneModelColumns.NAME to name,
            PlaneModelColumns.MANUFACTURER_ID to manufacturerId
        )

    override fun mapRowToData(row : Array<Any?>) : PlaneModelData =
        PlaneModelData(
            id = castRowElement(row, PlaneModelColumns.ID),
            capacity = castRowElement(row, PlaneModelColumns.CAPACITY),
            name = castRowElement(row, PlaneModelColumns.NAME),
            manufacturerId = castRowElement(row, PlaneModelColumns.MANUFACTURER_ID)
        )

    override fun debugData() {
        println("Plane model data: (\"$id\", \"$capacity\", \"$name\", \"$manufacturerId\")")
    }

    companion object {
        val EMPTY : PlaneModelData
            get() = PlaneModelData()

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null) : List<QueryResult<PlaneModelData>> {
            return EMPTY.queryDatabase(joinArgs, whereArgs)
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)
    }
}
