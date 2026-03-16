package data

object DestinationColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val COUNTRY_ID = Column<Int>("country_id", "INTEGER NOT NULL REFERENCES countries(id)")
    val CITY_NAME = Column<String>("city_name", "STRING NOT NULL")
    val TIMEZONE_ID = Column<Int>("timezone_id", "INTEGER NOT NULL REFERENCES timezones(id)")

    val ALL = listOf(ID, COUNTRY_ID, CITY_NAME, TIMEZONE_ID)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class DestinationData(

    override val id: Int = 0,
    var countryId : Int = 0,
    var cityName : String = "",
    var timezoneId : Int = 0

) : DataClass<DestinationData>(id) {

    override val tableName = "destinations"
    override val tableColumns = DestinationColumns.ALL
    override val tableAdditionalSQL = "UNIQUE (country_id, city_name)"

    override val initialRows : List<DestinationData>
        get() = listOf(
            DestinationData(countryId = CountryData.getCountryId("England"), cityName = "Luton", timezoneId = TimezoneData.getTimezoneId("GMT")),
            DestinationData(countryId = CountryData.getCountryId("Japan"), cityName = "Tokyo", timezoneId = TimezoneData.getTimezoneId("JST")),
            DestinationData(countryId = CountryData.getCountryId("Germany"), cityName = "Berlin", timezoneId = TimezoneData.getTimezoneId("CET"))
        )

    override val requiredTables : List<DataClass<*>>
        get() = listOf(
            CountryData.EMPTY,
            TimezoneData.EMPTY
        )

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            DestinationColumns.COUNTRY_ID to countryId,
            DestinationColumns.CITY_NAME to cityName.toString(),
            DestinationColumns.TIMEZONE_ID to timezoneId
        )

    override fun mapRowToData(row : Array<Any?>) : DestinationData =
        DestinationData(
            id = castRowElement(row, DestinationColumns.ID),
            countryId = castRowElement(row, DestinationColumns.COUNTRY_ID),
            cityName = castRowElement(row, DestinationColumns.CITY_NAME),
            timezoneId = castRowElement(row, DestinationColumns.TIMEZONE_ID)
        )

    override fun debugData() {
        println("Destination data: (\"$id\", \"$countryId\", \"$cityName\", \"$timezoneId\")")
    }

    fun getTimezoneOffset() : Float {
        val query : List<QueryResult<TimezoneData>> = TimezoneData.queryDatabase(timezoneId)
        if (query.isEmpty())
            return 0f

        return query.first().dataClass.timeOffset
    }

    companion object {
        val EMPTY : DestinationData
            get() = DestinationData()

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null
        ) : List<QueryResult<DestinationData>> = EMPTY.queryDatabase(joinArgs, whereArgs)

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)

        fun delete(id : Int) : Int {
            return DestinationData(id = id).delete()
        }

        fun queryDatabase(
            city : String,
            country : String
        ) : List<QueryResult<DestinationData>> {
            val joinArgs : JoinArgs = JoinArgs(
                joinType = "INNER",
                joinTable = CountryData.EMPTY.tableName,
                joinTable1Column = DestinationColumns.COUNTRY_ID.name,
                joinTable2Column = CountryColumns.ID.name,
                joinSelectColumns = CountryColumns.COLUMN_NAMES
            )

            val whereArgs : WhereArgs = WhereArgs(
                whereClause = "${EMPTY.tableName}.${DestinationColumns.CITY_NAME.name} = ? AND ${CountryData.EMPTY.tableName}.${CountryColumns.NAME.name} = ?",
                whereArgs = listOf(city, country)
            )

            return queryDatabase(joinArgs, whereArgs)
        }

        fun queryDatabase(id : Int) : List<QueryResult<DestinationData>> = queryDatabase(whereArgs = WhereArgs("${DestinationColumns.ID.name} = ?", listOf(id)))

        fun getTimezoneCode(id : Int) : String {
            val query = queryDatabase(id)
            if (query.isEmpty())
                return ""

            val timezones = TimezoneData.queryDatabase(query.first().dataClass.timezoneId)
            if (timezones.isEmpty())
                return ""

            if (timezones.first().dataClass.timeOffset >= 0)
                return "${timezones.first().dataClass.name} (UTC+${timezones.first().dataClass.timeOffset})"
            else
                return "${timezones.first().dataClass.name} (UTC${timezones.first().dataClass.timeOffset})"
        }

        fun getDestinationNames() : List<String> {
            val joinArgs : JoinArgs = JoinArgs(
                joinType = "INNER",
                joinTable = CountryData.EMPTY.tableName,
                joinTable1Column = DestinationColumns.COUNTRY_ID.name,
                joinTable2Column = CountryColumns.ID.name,
                joinSelectColumns = CountryColumns.COLUMN_NAMES
            )

            return queryDatabase(joinArgs).map { result ->
                "${result.dataClass.cityName} - ${result.getColumn(CountryData.EMPTY.tableName, CountryColumns.NAME.name)?.columnVal ?: ""}"
            }
        }

        fun getDestinationId (destination : String) : Int {
            val query : List<QueryResult<DestinationData>> = queryDatabase(whereArgs = WhereArgs("${DestinationColumns.CITY_NAME.name} = ?", listOf(destination)))
            if (query.isEmpty()) {
                println("Could not find destination $destination")
                return -1
            }

            return query.first().dataClass.id
        }
    }
}
