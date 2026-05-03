package data

object StaffColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val FIRSTNAME = Column<String?>("firstname", "VARCHAR")
    val LASTNAME = Column<String?>("lastname", "VARCHAR")
    val POSITION_ID = Column<Int>("position_id", "INTEGER NOT NULL REFERENCES ${StaffPositionData.EMPTY.tableName}(id)")
    val LOGIN_ID = Column<Int>("login_id", "INTEGER NOT NULL REFERENCES ${LoginData.EMPTY.tableName}(id)")
    val CURRENT_LOCATION = Column<Int>("current_location", "INTEGER NOT NULL REFERENCES ${DestinationData.EMPTY.tableName}(id)")
    val HOME_ID = Column<Int>("home_id", "INTEGER NOT NULL REFERENCES ${CountryData.EMPTY.tableName}(id)")

    val ALL = listOf(ID, FIRSTNAME, LASTNAME, POSITION_ID, LOGIN_ID, CURRENT_LOCATION, HOME_ID)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class StaffData(

    override val id: Int = 0,
    var firstName: String? = null,
    var lastName: String? = null,
    var positionId: Int = 0,
    var loginId: Int = 0,
    var currentLocation: Int = 0,
    var homeId: Int = 0

) : DataClass<StaffData>(id) {

    override val tableName = "staff"
    override val tableColumns = StaffColumns.ALL

    override val requiredTables : List<DataClass<*>>
        get() = listOf(
            StaffPositionData.EMPTY,
            LoginData.EMPTY,
            DestinationData.EMPTY,
            CountryData.EMPTY
        )
    
    override val initialRows: List<StaffData>
        get() = listOf(
        )

    override fun initTable () {
        // THIS IS FOR TEST PURPOSES AND SHOULD BE DELETED BEFORE GOING LIVE
        val destinations : List<QueryResult<DestinationData>> = DestinationData.queryDatabase()
        var currentFella : Long = 0L

        destinations.forEach { destination ->
            for (i in 1..100) {
                val loginId = LoginData(
                    email = "${currentFella.toString()}@${currentFella.toString()}",
                    passwordHash = currentFella.toString()
                ).insertIntoDatabase()
                StaffData (
                    firstName = currentFella.toString(),
                    lastName = currentFella.toString(),
                    positionId = StaffPositionData.getStaffPositionId(StaffPositions.FLIGHT_ATTENDANT),
                    loginId = loginId,
                    homeId = destination.dataClass.countryId,
                    currentLocation = destination.dataClass.id
                ).insertIntoDatabase()
                currentFella += 1
            }
            for (i in 1..20) {
                val loginId = LoginData(
                    email = "${currentFella.toString()}@${currentFella.toString()}",
                    passwordHash = currentFella.toString()
                ).insertIntoDatabase()
                StaffData (
                    firstName = currentFella.toString(),
                    lastName = currentFella.toString(),
                    positionId = StaffPositionData.getStaffPositionId(StaffPositions.PILOT),
                    loginId = loginId,
                    homeId = destination.dataClass.countryId,
                    currentLocation = destination.dataClass.id
                ).insertIntoDatabase()
                currentFella += 1
            }
        }
    }

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            StaffColumns.FIRSTNAME to firstName,
            StaffColumns.LASTNAME to lastName,
            StaffColumns.POSITION_ID to positionId,
            StaffColumns.LOGIN_ID to loginId,
            StaffColumns.CURRENT_LOCATION to currentLocation,
            StaffColumns.HOME_ID to homeId
        )

    override fun mapRowToData(row : Array<Any?>) : StaffData =
        StaffData(
            id = castRowElement(row, StaffColumns.ID),
            firstName = castRowElement(row, StaffColumns.FIRSTNAME),
            lastName = castRowElement(row, StaffColumns.LASTNAME),
            positionId = castRowElement(row, StaffColumns.POSITION_ID),
            loginId = castRowElement(row, StaffColumns.LOGIN_ID),
            currentLocation = castRowElement(row, StaffColumns.CURRENT_LOCATION),
            homeId = castRowElement(row, StaffColumns.HOME_ID)
        )

    override fun debugData() {
        println("Staff data: (\"$id\", \"$firstName\", \"$lastName\", \"$positionId\", \"$loginId\", \"$currentLocation\", \"$homeId\")")
    }

    companion object {
        val EMPTY : StaffData
            get() = StaffData()

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null,
            orderByArgs : OrderByArgs? = null,
            limitArgs : LimitArgs? = null        
        ) : List<QueryResult<StaffData>> {
            return EMPTY.queryDatabase(joinArgs, whereArgs, orderByArgs, limitArgs)
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)

        fun delete(id : Int) : Int {
            return StaffData(id = id).delete()
        }

        fun queryByLogIn(
            email : String
        ) : List<QueryResult<StaffData>> {
            val joinArgs : JoinArgs = JoinArgs(
                joinType = "INNER",
                joinTable = LoginData.EMPTY.tableName,
                joinTable1Column = StaffColumns.LOGIN_ID.name,
                joinTable2Column = LoginColumns.ID.name,
                joinSelectColumns = LoginColumns.ALL.map { it.name }
            )

            val whereArgs : WhereArgs = WhereArgs(
                whereClause = "${LoginData.EMPTY.tableName}.${LoginColumns.EMAIL.name} = ?",
                listOf(email)
            )

            return queryDatabase(
                joinArgs = joinArgs,
                whereArgs = whereArgs
            )
        }
    }
}
