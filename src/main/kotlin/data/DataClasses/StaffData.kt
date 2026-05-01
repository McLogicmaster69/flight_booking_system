package data

object StaffColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val FIRSTNAME = Column<String?>("firstname", "VARCHAR")
    val LASTNAME = Column<String?>("lastname", "VARCHAR")
    val POSITION_ID = Column<Int>("position_id", "INTEGER NOT NULL REFERENCES staff_positions(id)")
    val LOGIN_ID = Column<Int>("login_id", "INTEGER NOT NULL REFERENCES login_info(id)")
    val HOME_ID = Column<Int>("home_id", "INTEGER NOT NULL REFERENCES countries(id)")

    val ALL = listOf(ID, FIRSTNAME, LASTNAME, POSITION_ID, LOGIN_ID, HOME_ID)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class StaffData(

    override val id: Int = 0,
    var firstName: String? = null,
    var lastName: String? = null,
    var positionId: Int = 0,
    var loginId: Int = 0,
    var homeId: Int = 0

) : DataClass<StaffData>(id) {

    override val tableName = "staff"
    override val tableColumns = StaffColumns.ALL

    override val requiredTables : List<DataClass<*>>
        get() = listOf(
            StaffPositionData.EMPTY,
            LoginData.EMPTY
        )
    
    override val initialRows: List<StaffData>
        get() = listOf(
            StaffData(
                firstName = "A",
                lastName = "1",
                positionId = StaffPositionData.getStaffPositionId(StaffPositions.PILOT),
                loginId = LoginData.getLoginData("a@1"),
                homeId = CountryData.queryDatabase().first().dataClass.id
            ),
            StaffData(
                firstName = "B",
                lastName = "2",
                positionId = StaffPositionData.getStaffPositionId(StaffPositions.PILOT),
                loginId = LoginData.getLoginData("b@2"),
                homeId = CountryData.queryDatabase().first().dataClass.id
            ),
            StaffData(
                firstName = "C",
                lastName = "3",
                positionId = StaffPositionData.getStaffPositionId(StaffPositions.FLIGHT_ATTENDANT),
                loginId = LoginData.getLoginData("c@3"),
                homeId = CountryData.queryDatabase().first().dataClass.id
            ),
            StaffData(
                firstName = "D",
                lastName = "4",
                positionId = StaffPositionData.getStaffPositionId(StaffPositions.FLIGHT_ATTENDANT),
                loginId = LoginData.getLoginData("d@4"),
                homeId = CountryData.queryDatabase().first().dataClass.id
            ),
            StaffData(
                firstName = "E",
                lastName = "5",
                positionId = StaffPositionData.getStaffPositionId(StaffPositions.FLIGHT_ATTENDANT),
                loginId = LoginData.getLoginData("e@5"),
                homeId = CountryData.queryDatabase().first().dataClass.id
            ),
            StaffData(
                firstName = "F",
                lastName = "6",
                positionId = StaffPositionData.getStaffPositionId(StaffPositions.FLIGHT_ATTENDANT),
                loginId = LoginData.getLoginData("f@6"),
                homeId = CountryData.queryDatabase().first().dataClass.id
            )
        )

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            StaffColumns.FIRSTNAME to firstName,
            StaffColumns.LASTNAME to lastName,
            StaffColumns.POSITION_ID to positionId,
            StaffColumns.LOGIN_ID to loginId,
            StaffColumns.HOME_ID to homeId
        )

    override fun mapRowToData(row : Array<Any?>) : StaffData =
        StaffData(
            id = castRowElement(row, StaffColumns.ID),
            firstName = castRowElement(row, StaffColumns.FIRSTNAME),
            lastName = castRowElement(row, StaffColumns.LASTNAME),
            positionId = castRowElement(row, StaffColumns.POSITION_ID),
            loginId = castRowElement(row, StaffColumns.LOGIN_ID),
            homeId = castRowElement(row, StaffColumns.HOME_ID)
        )

    override fun debugData() {
        println("Staff data: (\"$id\", \"$firstName\", \"$lastName\", \"$positionId\", \"$loginId\", \"$homeId\")")
    }

    companion object {
        val EMPTY : StaffData
            get() = StaffData()

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null
        ) : List<QueryResult<StaffData>> {
            return EMPTY.queryDatabase(joinArgs, whereArgs)
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
