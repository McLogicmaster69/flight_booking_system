package data

object PlaneModelColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val NAME = Column<String>("name", "STRING NOT NULL")
    val MANUFACTURER_ID = Column<Int>("manufacturer_id", "INTEGER NOT NULL REFERENCES ${ManufacturerData.EMPTY.tableName}(id)")
    val CAPACITY = Column<Int>("capacity", "INTEGER NOT NULL")
    val PILOTS = Column<Int>("pilots", "INTEGER NOT NULL")
    val ATTENDANTS = Column<Int>("attendants", "INTEGER NOT NULL")

    val ALL = listOf(ID, NAME, MANUFACTURER_ID, CAPACITY, PILOTS, ATTENDANTS)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class PlaneModelData(

    override val id : Int = 0,
    var name : String = "",
    var manufacturerId : Int = 0,
    var capacity : Int = 0,
    var pilots : Int = 0,
    var attendants : Int = 0

) : DataClass<PlaneModelData>(id) {

    override val tableName = "plane_models"
    override val tableColumns = PlaneModelColumns.ALL
    override val tableAdditionalSQL = "UNIQUE (${PlaneModelColumns.NAME.name}, ${PlaneModelColumns.MANUFACTURER_ID.name})"

    override val indexes : List<IndexArgs> = listOf(
        IndexArgs("inx_plane_models_manufacturer_id", PlaneModelColumns.MANUFACTURER_ID.name)
    )

    override val initialRows: List<PlaneModelData>
        get() = listOf(
            PlaneModelData(
                name = "Boeing 737-800",
                manufacturerId = ManufacturerData.getManufacturerId("Boeing"),
                capacity = 189,
                pilots = 2,
                attendants = 4
            ),
            PlaneModelData(
                name = "Airbus A321",
                manufacturerId = ManufacturerData.getManufacturerId("Airbus"),
                capacity = 220,
                pilots = 2,
                attendants = 4
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
            PlaneModelColumns.CAPACITY to capacity,
            PlaneModelColumns.PILOTS to pilots,
            PlaneModelColumns.ATTENDANTS to attendants
        )

    override fun mapRowToData(row : Array<Any?>) : PlaneModelData =
        PlaneModelData(
            id = castRowElement(row, PlaneModelColumns.ID),
            name = castRowElement(row, PlaneModelColumns.NAME),
            manufacturerId = castRowElement(row, PlaneModelColumns.MANUFACTURER_ID),
            capacity = castRowElement(row, PlaneModelColumns.CAPACITY),
            pilots = castRowElement(row, PlaneModelColumns.PILOTS),
            attendants = castRowElement(row, PlaneModelColumns.ATTENDANTS)
        )

    override fun debugData() {
        println("Plane model data: (\"$id\", \"$name\", \"$manufacturerId\", \"$capacity\", \"$pilots\", \"$attendants\")")
    }

    companion object {
        val EMPTY : PlaneModelData
            get() = PlaneModelData()

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null,
            orderByArgs : OrderByArgs? = null,
            limitArgs : LimitArgs? = null        
        ) : List<QueryResult<PlaneModelData>> {
            return EMPTY.queryDatabase(joinArgs, whereArgs, orderByArgs, limitArgs)
        }

        fun queryDatabase(id : Int) : List<QueryResult<PlaneModelData>> = queryDatabase(whereArgs = WhereArgs("${PlaneModelColumns.ID.name} = ?", listOf(id)))

        fun getPlaneModelId(name: String): Int {
            return queryDatabase(
                whereArgs = WhereArgs("${PlaneModelColumns.NAME.name} = ?", listOf(name))
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