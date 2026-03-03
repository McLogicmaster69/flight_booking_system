package data

object DestinationColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val COUNTRY_ID = Column<Int>("country_id", "INTEGER NOT NULL REFERENCES countries(id)")
    val CITY_NAME = Column<String>("city_name", "STRING NOT NULL")

    val ALL = listOf(ID, COUNTRY_ID, CITY_NAME)
}

data class DestinationData(

    val id: Int = 0,
    var countryId : Int = 0,
    var cityName : String = ""

) : DataClass<DestinationData>() {

    override val tableName = "destinations"
    override val tableColumns = DestinationColumns.ALL

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            DestinationColumns.COUNTRY_ID to countryId,
            DestinationColumns.CITY_NAME to cityName.toString()
        )

    override fun mapRowToData(row : Array<Any?>) : DestinationData =
        DestinationData(
            id = castRowElement(row, DestinationColumns.ID),
            countryId = castRowElement(row, DestinationColumns.COUNTRY_ID),
            cityName = castRowElement(row, DestinationColumns.CITY_NAME)
        )

    override fun debugData() {
        println("Destination data: (\"$id\", \"$countryId\", \"$cityName\")")
    }

    companion object {
        val EMPTY : DestinationData
            get() = DestinationData()

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null) : List<QueryResult<DestinationData>> {
            return EMPTY.queryDatabase(joinArgs, whereArgs)
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)
    }
}
