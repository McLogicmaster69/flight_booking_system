package data

object TicketTypeColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val NAME = Column<String>("name", "VARCHAR NOT NULL UNIQUE")

    val ALL = listOf(ID, NAME)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class TicketTypeData(

    override val id: Int = 0,
    var name : String = "",

) : DataClass<TicketTypeData>(id) {

    override val tableName = "ticket_types"
    override val tableColumns = TicketTypeColumns.ALL

    override val initialRows: List<TicketTypeData>
        get() = listOf(
            TicketTypeData(name = "Adult"),
            TicketTypeData(name = "Child")
        )

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            TicketTypeColumns.NAME to name
        )

    override fun mapRowToData(row : Array<Any?>) : TicketTypeData =
        TicketTypeData(
            id = castRowElement(row, TicketTypeColumns.ID),
            name = castRowElement(row, TicketTypeColumns.NAME)
        )

    override fun debugData() {
        println("Ticket type data: (\"$id\", \"$name\")")
    }

    companion object {
        val EMPTY : TicketTypeData
            get() = TicketTypeData()

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null) : List<QueryResult<TicketTypeData>> {
            return EMPTY.queryDatabase(joinArgs, whereArgs)
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)

        fun delete(id : Int) : Int {
            return TicketTypeData(id = id).delete()
        }
    }
}
