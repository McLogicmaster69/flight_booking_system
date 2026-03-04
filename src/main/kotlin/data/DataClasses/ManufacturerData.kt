package data

object ManufacturerColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val NAME = Column<String?>("name", "VARCHAR")

    val ALL = listOf(ID, NAME)
}

data class ManufacturerData(

    override val id: Int = 0,
    var name : String? = null,

) : DataClass<ManufacturerData>(id) {

    override val tableName = "manufacturers"
    override val tableColumns = ManufacturerColumns.ALL

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

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)

        fun delete(id : Int) : Int {
            return ManufacturerData(id = id).delete()
        }
    }
}
