package data

object CountryColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val NAME = Column<String>("name", "VARCHAR NOT NULL")
    val TIMEZONE = Column<String>("timezone", "VARCHAR NOT NULL")

    val ALL = listOf(ID, NAME, TIMEZONE)
}

data class CountryData(

    override val id: Int = 0,
    var name : String = "",
    var timezone : String = ""

) : DataClass<CountryData>(id) {

    override val tableName = "countries"
    override val tableColumns = CountryColumns.ALL

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            CountryColumns.NAME to name,
            CountryColumns.TIMEZONE to timezone
        )

    override fun mapRowToData(row : Array<Any?>) : CountryData =
        CountryData(
            id = castRowElement(row, CountryColumns.ID),
            name = castRowElement(row, CountryColumns.NAME),
            timezone = castRowElement(row, CountryColumns.TIMEZONE)
        )

    override fun debugData() {
        println("Country data: (\"$id\", \"$name\", \"$timezone\")")
    }

    companion object {
        val EMPTY : CountryData
            get() = CountryData()

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null) : List<QueryResult<CountryData>> {
            return EMPTY.queryDatabase(joinArgs, whereArgs)
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)

        fun delete(id : Int) : Int {
            return CountryData(id = id).delete()
        }
    }
}
