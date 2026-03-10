package data

object StaffColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val FIRSTNAME = Column<String?>("firstname", "VARCHAR")
    val LASTNAME = Column<String?>("lastname", "VARCHAR")
    val POSITION_ID = Column<Int>("position_id", "INTEGER NOT NULL REFERENCES staff_positions(id)")
    val LOGIN_ID = Column<Int>("login_id", "INTEGER NOT NULL REFERENCES login_info(id)")

    val ALL = listOf(ID, FIRSTNAME, LASTNAME, POSITION_ID, LOGIN_ID)
}

data class StaffData(

    override val id: Int = 0,
    var firstName: String? = null,
    var lastName: String? = null,
    var positionId: Int = 0,
    var loginId: Int = 0

) : DataClass<StaffData>(id) {

    override val tableName = "staff"
    override val tableColumns = StaffColumns.ALL

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            StaffColumns.FIRSTNAME to firstName,
            StaffColumns.LASTNAME to lastName,
            StaffColumns.POSITION_ID to positionId,
            StaffColumns.LOGIN_ID to loginId
        )

    override fun mapRowToData(row : Array<Any?>) : StaffData =
        StaffData(
            id = castRowElement(row, StaffColumns.ID),
            firstName = castRowElement(row, StaffColumns.FIRSTNAME),
            lastName = castRowElement(row, StaffColumns.LASTNAME),
            positionId = castRowElement(row, StaffColumns.POSITION_ID),
            loginId = castRowElement(row, StaffColumns.LOGIN_ID)
        )

    override fun debugData() {
        println("Staff data: (\"$id\", \"$firstName\", \"$lastName\", \"$positionId\", \"$loginId\")")
    }

    companion object {
        val EMPTY : StaffData
            get() = StaffData()

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null) : List<QueryResult<StaffData>> {
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
