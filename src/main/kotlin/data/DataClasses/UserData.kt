package data

object UserColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val FIRSTNAME = Column<String?>("firstname", "VARCHAR")
    val LASTNAME = Column<String?>("lastname", "VARCHAR")
    val VERIFIED = Column<Boolean?>("verified_account", "BOOL")
    val LOGIN_ID = Column<Int>("login_id", "INTEGER NOT NULL REFERENCES login_info(id)")

    val ALL = listOf(ID, FIRSTNAME, LASTNAME, VERIFIED, LOGIN_ID)
}

data class UserData(

    val id: Int = 0,
    var firstName: String? = null,
    var lastName: String? = null,
    var verifiedAccount: Boolean? = null,
    var loginId: Int = 0

) : DataClass<UserData>() {

    override val tableName = "users"
    override val tableColumns = UserColumns.ALL

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            UserColumns.FIRSTNAME to firstName,
            UserColumns.LASTNAME to lastName,
            UserColumns.VERIFIED to verifiedAccount,
            UserColumns.LOGIN_ID to loginId
        )

    override fun mapRowToData(row : Array<Any?>) : UserData =
        UserData(
            id = castRowElement(row, UserColumns.ID),
            firstName = castRowElement(row, UserColumns.FIRSTNAME),
            lastName = castRowElement(row, UserColumns.LASTNAME),
            verifiedAccount = anyToBool(castRowElement(row, UserColumns.VERIFIED)),
            loginId = castRowElement(row, UserColumns.LOGIN_ID)
        )

    override fun debugData() {
        println("User data: (\"$id\", \"$firstName\", \"$lastName\", \"$verifiedAccount\", \"$loginId\")")
    }

    fun update() {
        DatabaseManager.updateInDatabase(
            tableName,
            id,
            mapDataToColumns()
        )
    }

    companion object {
        val EMPTY : UserData
            get() = UserData()

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null) : List<QueryResult<UserData>> {
            return EMPTY.queryDatabase(joinArgs, whereArgs)
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)
    }
}
