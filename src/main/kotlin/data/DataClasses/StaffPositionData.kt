package data

object StaffPositionColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val NAME = Column<String?>("name", "VARCHAR")

    val ALL = listOf(ID, NAME)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class StaffPositionData(

    override val id: Int = 0,
    var name : String? = null,

) : DataClass<StaffPositionData>(id) {

    override val tableName = "staff_positions"
    override val tableColumns = StaffPositionColumns.ALL

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            StaffPositionColumns.NAME to name
        )

    override fun mapRowToData(row : Array<Any?>) : StaffPositionData =
        StaffPositionData(
            id = castRowElement(row, StaffPositionColumns.ID),
            name = castRowElement(row, StaffPositionColumns.NAME)
        )

    override fun debugData() {
        println("Staff Position data: (\"$id\", \"$name\")")
    }

    companion object {
        val EMPTY : StaffPositionData
            get() = StaffPositionData()

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null) : List<QueryResult<StaffPositionData>> {
            return EMPTY.queryDatabase(joinArgs, whereArgs)
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)

        fun delete(id : Int) : Int {
            return StaffPositionData(id = id).delete()
        }
    }
}
