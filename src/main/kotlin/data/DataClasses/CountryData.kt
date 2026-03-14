package data

object CountryColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val NAME = Column<String>("name", "VARCHAR NOT NULL UNIQUE")

    val ALL = listOf(ID, NAME)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class CountryData(

    override val id: Int = 0,
    var name : String = ""

) : DataClass<CountryData>(id) {

    override val tableName = "countries"
    override val tableColumns = CountryColumns.ALL

    override val initialRows : List<CountryData>
        get() = listOf(
            CountryData(name = "England"),
            CountryData(name = "Japan"),
            CountryData(name = "Germany")
        )

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            CountryColumns.NAME to name
        )

    override fun mapRowToData(row : Array<Any?>) : CountryData =
        CountryData(
            id = castRowElement(row, CountryColumns.ID),
            name = castRowElement(row, CountryColumns.NAME)
        )

    override fun debugData() {
        println("Country data: (\"$id\", \"$name\")")
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

        fun getCountryId (country : String) : Int {
            val query : List<QueryResult<CountryData>> = queryDatabase(whereArgs = WhereArgs("${CountryColumns.NAME.name} = ?", listOf(country)))
            if (query.isEmpty()) {
                println("Could not find country $country")
                return -1
            }

            return query.first().dataClass.id
        }
    }
}
