package data

object PlaneModelColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val NAME = Column<String>("name", "STRING NOT NULL")
    val MANUFACTURER_ID = Column<Int>("manufacturer_id", "INTEGER NOT NULL REFERENCES manufacturers(id)")
    val CAPACITY = Column<Int>("capacity", "INTEGER NOT NULL")

    val ALL = listOf(ID, NAME, MANUFACTURER_ID, CAPACITY)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class PlaneModelData(

    override val id : Int = 0,
    var name : String = "",
    var manufacturerId : Int = 0,
    var capacity : Int = 0

) : DataClass<PlaneModelData>(id) {

    override val tableName = "plane_models"
    override val tableColumns = PlaneModelColumns.ALL
    override val tableAdditionalSQL = "UNIQUE (name, manufacturer_id)"

    override val initialRows: List<PlaneModelData>
        get() = listOf(
            PlaneModelData(
                name = "Boeing 737-800",
                manufacturerId = ManufacturerData.getManufacturerId("Boeing")
            ),
            PlaneModelData(
                name = "Airbus A321",
                manufacturerId = ManufacturerData.getManufacturerId("Airbus")
            )
        )

    override val requiredTables: List<DataClass<*>>
        get() = listOf(
            ManufacturerData.EMPTY
        )

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            PlaneModelColumns.NAME to name,
            PlaneModelColumns.MANUFACTURER_ID to manufacturerId,
            PlaneModelColumns.CAPACITY to capacity
        )

    override fun mapRowToData(row : Array<Any?>) : PlaneModelData =
        PlaneModelData(
            id = castRowElement(row, PlaneModelColumns.ID),
            name = castRowElement(row, PlaneModelColumns.NAME),
            manufacturerId = castRowElement(row, PlaneModelColumns.MANUFACTURER_ID),
            capacity = castRowElement(row, PlaneModelColumns.CAPACITY)
        )

    override fun debugData() {
        println("Plane model data: (\"$id\", \"$name\", \"$manufacturerId\", \"$capacity\")")
    }

    companion object {
        val EMPTY : PlaneModelData
            get() = PlaneModelData()

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null) : List<QueryResult<PlaneModelData>> {
            return EMPTY.queryDatabase(joinArgs, whereArgs)
        }

        fun getPlaneModelId(name: String): Int {
            return queryDatabase(
                whereArgs = WhereArgs("name = ?", listOf(name))
            ).firstOrNull()?.dataClass?.id ?: -1
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)

        fun delete(id : Int) : Int {
            return PlaneModelData(id = id).delete()
        }
    }
}