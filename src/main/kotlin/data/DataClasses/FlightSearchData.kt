package data

import java.sql.Timestamp
import java.time.LocalDate
import java.time.Instant

object FlightSearchColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY")
    val TOKEN = Column<String>("token", "VARCHAR NOT NULL UNIQUE")
    val START_DESTINATION = Column<Int>("start_destination", "INTEGER NOT NULL REFERENCES ${DestinationData.EMPTY.tableName}(id)")
    val END_DESTINATION = Column<Int>("end_destination", "INTEGER NOT NULL REFERENCES ${DestinationData.EMPTY.tableName}(id)")
    val DATE = Column<String>("date", "STRING NOT NULL")
    val CREATED_AT = Column<Timestamp>("created_at", "TIMESTAMP NOT NULL")

    val ALL = listOf(ID, TOKEN, START_DESTINATION, END_DESTINATION, DATE, CREATED_AT)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class FlightSearchData(

    override val id : Int = 0,
    var token : String = "",
    var startDestination : Int = 0,
    var endDestination : Int = 0,
    var date : LocalDate = LocalDate.parse("1970-01-01"),
    var createdAt : Timestamp = Timestamp(System.currentTimeMillis())

) : DataClass<FlightSearchData>(id) {

    override val tableName = "flight_searches"
    override val tableColumns = FlightSearchColumns.ALL

    override val indexes : List<IndexArgs> = listOf(
        IndexArgs("inx_flight_searches_start_destination", FlightSearchColumns.START_DESTINATION.name),
        IndexArgs("inx_flight_searches_end_destination", FlightSearchColumns.END_DESTINATION.name),
        IndexArgs("inx_flight_searches_created_at", FlightSearchColumns.CREATED_AT.name)
    )

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            FlightSearchColumns.TOKEN to token,
            FlightSearchColumns.START_DESTINATION to startDestination,
            FlightSearchColumns.END_DESTINATION to endDestination,
            FlightSearchColumns.DATE to date,
            FlightSearchColumns.CREATED_AT to createdAt
        )

    override fun mapRowToData(row : Array<Any?>) : FlightSearchData {
        val rawCreatedAt = row[tableColumns.indexOf(FlightSearchColumns.CREATED_AT)]

        val createdAtValue = when (rawCreatedAt) {
            is Timestamp -> rawCreatedAt
            is Long -> Timestamp(rawCreatedAt)
            else -> throw IllegalStateException("Unexpected created at type: ${rawCreatedAt?.javaClass}")
        }

        return FlightSearchData(
            id = castRowElement(row, FlightSearchColumns.ID),
            token = castRowElement(row, FlightSearchColumns.TOKEN),
            startDestination = castRowElement(row, FlightSearchColumns.START_DESTINATION),
            endDestination = castRowElement(row, FlightSearchColumns.END_DESTINATION),
            date = castDateRowElement(row, FlightSearchColumns.DATE),
            createdAt = createdAtValue
        )
    }

    override fun debugData() {
        println("Flight Search data: (\"$id\", \"$token\", \"$startDestination\", \"$endDestination\", \"$date\", \"$createdAt\")")
    }

    companion object {
        val EMPTY : FlightSearchData
            get() = FlightSearchData()

        fun queryDatabase (
            multipleJoinArgs : MultipleJoinArgs? = null,
            whereArgs : WhereArgs? = null,
            orderByArgs : OrderByArgs? = null,
            limitArgs : LimitArgs? = null,
            groupByArgs : GroupByArgs? = null   
        ) : List<QueryResult<FlightSearchData>> {
            return EMPTY.queryDatabase(multipleJoinArgs, whereArgs, orderByArgs, limitArgs, groupByArgs)
        }

        fun queryDatabase (
            token : String,
            multipleJoinArgs : MultipleJoinArgs? = null
        ) : List<QueryResult<FlightSearchData>> {
            val whereArgs : WhereArgs = WhereArgs(
                whereClause = "${FlightSearchColumns.TOKEN.name} = ?",
                whereArgs = listOf(token)
            )

            return queryDatabase(multipleJoinArgs, whereArgs)
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)

        fun delete(id : Int) : Int {
            return FlightSearchData(id = id).delete()
        }

        fun deleteOld() {
            val whereArgs = WhereArgs(
                whereClause = "${FlightSearchColumns.CREATED_AT.name} > datetime('now', '-1 month')",
                whereArgs = emptyList()
            )

            val query : List<QueryResult<FlightSearchData>> = queryDatabase(whereArgs = whereArgs)

            query.forEach {
                FlightSearchFlightData.deleteByFlightSearch(it.dataClass.id)
                it.dataClass.delete()
            }
        }

        fun hasToken(token : String) : Boolean {
            val query : List<QueryResult<FlightSearchData>> = queryDatabase(token)
            return query.size > 0
        }

        fun createSearch (
            startDestination : Int,
            endDestination : Int,
            date : LocalDate,
            createdAt : Timestamp
        ) : FlightSearchData {
            var token : String
            do {
                token = EMPTY.generateToken()
            } while (hasToken(token))

            val flightSearch : FlightSearchData = FlightSearchData(
                token = token,
                startDestination = startDestination,
                endDestination = endDestination,
                date = date,
                createdAt = createdAt
            )

            val id : Int = flightSearch.insertIntoDatabase()
            return FlightSearchData(
                id = id,
                token = token,
                startDestination = startDestination,
                endDestination = endDestination,
                date = date,
                createdAt = createdAt
            )
        }

        fun queryByToken (
            token : String
        ) : FlightSearchInfo? {
            val query : List<QueryResult<FlightSearchData>> = queryDatabase(token)
            if (query.isEmpty())
                return null

            val search : FlightSearchData = query.first().dataClass
            return FlightSearchInfo (
                search = search,
                flights = FlightSearchFlightData.queryByFlightSearch(search.id).map { it.dataClass }
            )
        }

        fun checkDataIsPath (
            path : JourneyFlightTimePath,
            data : FlightSearchData
        ) : Boolean {
            val query : List<QueryResult<FlightSearchFlightData>> = FlightSearchFlightData.queryByFlightSearch(data.id)

            if (query.size != path.flightIds.size)
                return false

            query.forEachIndexed { index, item ->
                if (item.dataClass.flightId != path.flightIds[index])
                    return false
            }

            return true
        }

        fun queryFlightPath (
            path : JourneyFlightTimePath
        ) : FlightSearchData? {
            val whereArgs : WhereArgs = WhereArgs (
                whereClause = "${FlightSearchColumns.START_DESTINATION.name} = ? AND ${FlightSearchColumns.END_DESTINATION.name} = ? AND ${FlightSearchColumns.DATE.name} = ?",
                whereArgs = listOf(
                    path.destinationIds.first(),
                    path.destinationIds.last(),
                    path.localDateTimes.first().toLocalDate().toString()
                )
            )

            val query : List<QueryResult<FlightSearchData>> = queryDatabase(
                whereArgs = whereArgs
            )

            query.forEach {
                if (checkDataIsPath(path, it.dataClass))
                    return it.dataClass
            }

            return null
        }

        fun queryOrAddFlightPath (
            path : JourneyFlightTimePath
        ) : FlightSearchData {
            val possibleSearch : FlightSearchData? = queryFlightPath(path)
            if (possibleSearch != null) {
                possibleSearch.createdAt = Timestamp.from(Instant.now())
                possibleSearch.update()
                return possibleSearch
            }

            val search : FlightSearchData = createSearch(
                path.destinationIds.first(),
                path.destinationIds.last(),
                path.localDateTimes.first().toLocalDate(),
                Timestamp.from(Instant.now())
            )

            path.flightIds.forEachIndexed { index, item ->
                FlightSearchFlightData(
                    flightSearchId = search.id,
                    flightId = item,
                    position = index
                ).insertIntoDatabase()
            }

            return search
        }
    }
}