package data

object ManufacturerColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val NAME = Column<String?>("name", "VARCHAR")

    val ALL = listOf(ID, NAME)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class ManufacturerData(

    override val id: Int = 0,
    var name : String? = null,

) : DataClass<ManufacturerData>(id) {

    override val tableName = "manufacturers"
    override val tableColumns = ManufacturerColumns.ALL

    override val initialRows: List<ManufacturerData>
        get() = listOf(
            ManufacturerData(name = "Boeing"),
            ManufacturerData(name = "Airbus")
        )

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            ManufacturerColumns.NAME to name
        )

    override fun mapRowToData(row : Array<Any?>) : ManufacturerData =
        ManufacturerData(
            id = castRowElement(row, ManufacturerColumns.ID),
            name = castRowElement(row, ManufacturerColumns.NAME)
        )

    override fun debugData() {
        println("Manufacturers data: (\"$id\", \"$name\")")
    }

    companion object {
        val EMPTY : ManufacturerData
            get() = ManufacturerData()

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null) : List<QueryResult<ManufacturerData>> {
            return EMPTY.queryDatabase(joinArgs, whereArgs)
        }

        fun getManufacturerId(name: String): Int {
            return queryDatabase(
                whereArgs = WhereArgs("name = ?", listOf(name))
            ).firstOrNull()?.dataClass?.id ?: -1
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)

        fun delete(id : Int) : Int {
            return ManufacturerData(id = id).delete()
        }

        fun getManufacturerId (manufacturer : String) : Int {
            val query : List<QueryResult<ManufacturerData>> = queryDatabase(whereArgs = WhereArgs("${ManufacturerColumns.NAME.name} = ?", listOf(manufacturer)))
            if (query.isEmpty()) {
                println("Could not find manufacturer $manufacturer")
                return -1
            }

            return query.first().dataClass.id
        }
    }
}