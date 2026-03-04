package data

object LoginColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val EMAIL = Column<String>("email", "VARCHAR NOT NULL UNIQUE")
    val PASSWORD_HASH = Column<String>("password_hash", "VARCHAR NOT NULL")

    val ALL = listOf(ID, EMAIL, PASSWORD_HASH)
}

data class LoginData(

    val id: Int = 0,
    var email: String = "",
    var passwordHash: String = ""

) : DataClass<LoginData>() {

    override val tableName = "login_info"
    override val tableColumns = LoginColumns.ALL

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            LoginColumns.EMAIL to email,
            LoginColumns.PASSWORD_HASH to passwordHash
        )

    override fun mapRowToData(row : Array<Any?>) : LoginData =
        LoginData(
            id = castRowElement(row, LoginColumns.ID),
            email = castRowElement(row, LoginColumns.EMAIL),
            passwordHash = castRowElement(row, LoginColumns.PASSWORD_HASH)
        )

    override fun debugData() {
        println("Login data: (\"$id\", \"$email\", \"$passwordHash\")")
    }

    companion object {
        val EMPTY : LoginData
            get() = LoginData()

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null) : List<QueryResult<LoginData>> {
            return EMPTY.queryDatabase(joinArgs, whereArgs)
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)
    }
}