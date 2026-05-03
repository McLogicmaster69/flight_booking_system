package data

object LoginColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val EMAIL = Column<String>("email", "VARCHAR NOT NULL UNIQUE")
    val PASSWORD_HASH = Column<String>("password_hash", "VARCHAR NOT NULL")

    val ALL = listOf(ID, EMAIL, PASSWORD_HASH)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class LoginData(

    override val id: Int = 0,
    var email: String = "",
    var passwordHash: String = ""

) : DataClass<LoginData>(id) {

    override val tableName = "login_info"
    override val tableColumns = LoginColumns.ALL

    override val initialRows: List<LoginData>
        get() = listOf(
        )

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
            whereArgs : WhereArgs? = null,
            orderByArgs : OrderByArgs? = null,
            limitArgs : LimitArgs? = null
        ) : List<QueryResult<LoginData>> {
            return EMPTY.queryDatabase(joinArgs, whereArgs, orderByArgs, limitArgs)
        }

        fun getLoginData (email : String) : Int {
            val query : List<QueryResult<LoginData>> = queryDatabase(whereArgs = WhereArgs("${LoginColumns.EMAIL.name} = ?", listOf(email)))
            if (query.isEmpty()) {
                println("Could not find log in for $email")
                return -1
            }

            return query.first().dataClass.id
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)

        fun delete(id : Int) : Int {
            return LoginData(id = id).delete()
        }
    }
}