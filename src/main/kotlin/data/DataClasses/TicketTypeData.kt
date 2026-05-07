package data

object TicketTypeColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val NAME = Column<String>("name", "VARCHAR NOT NULL UNIQUE")

    val ALL = listOf(ID, NAME)
    val COLUMN_NAMES = ALL.map { it.name }
}

object TicketTypes {
    val ADULT = "Adult"
    val CHILD = "Child"
}

data class TicketTypeData(
    override val id: Int = 0,
    var name: String = "",
) : DataClass<TicketTypeData>(id) {
    override val tableName = "ticket_types"
    override val tableColumns = TicketTypeColumns.ALL

    override val initialRows: List<TicketTypeData>
        get() =
            listOf(
                TicketTypeData(name = TicketTypes.ADULT),
                TicketTypeData(name = TicketTypes.CHILD),
            )

    override fun mapDataToColumns(): Map<Column<*>, Any?> =
        mapOf(
            TicketTypeColumns.NAME to name,
        )

    override fun mapRowToData(row: Array<Any?>): TicketTypeData =
        TicketTypeData(
            id = castRowElement(row, TicketTypeColumns.ID),
            name = castRowElement(row, TicketTypeColumns.NAME),
        )

    override fun debugData() {
        println("Ticket type data: (\"$id\", \"$name\")")
    }

    companion object {
        val EMPTY: TicketTypeData
            get() = TicketTypeData()

        fun queryDatabase(
            multipleJoinArgs: MultipleJoinArgs? = null,
            whereArgs: WhereArgs? = null,
            orderByArgs: OrderByArgs? = null,
            limitArgs: LimitArgs? = null,
            groupByArgs: GroupByArgs? = null,
        ): List<QueryResult<TicketTypeData>> =
            EMPTY.queryDatabase(multipleJoinArgs, whereArgs, orderByArgs, limitArgs, groupByArgs)

        fun updateTable(
            values: Map<Column<*>, Any?>,
            whereArgs: WhereArgs,
        ): Int = EMPTY.updateTable(values, whereArgs)

        fun delete(id: Int): Int = TicketTypeData(id = id).delete()
    }
}
