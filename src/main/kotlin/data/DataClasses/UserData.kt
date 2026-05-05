package data

object UserColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val FIRSTNAME = Column<String?>("firstname", "VARCHAR")
    val LASTNAME = Column<String?>("lastname", "VARCHAR")
    val VERIFIED = Column<Boolean?>("verified_account", "BOOL")
    val LOYALTY_POINTS = Column<Int>("loyalty_points", "INT NOT NULL")
    val LOGIN_ID = Column<Int>("login_id", "INTEGER NOT NULL REFERENCES ${LoginData.EMPTY.tableName}(id)")

    val ALL = listOf(ID, FIRSTNAME, LASTNAME, VERIFIED, LOYALTY_POINTS, LOGIN_ID)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class UserData(

    override val id: Int = 0,
    var firstName: String? = null,
    var lastName: String? = null,
    var verifiedAccount: Boolean? = null,
    var loyaltyPoints: Int = 0,
    var loginId: Int = 0

) : DataClass<UserData>(id) {

    override val tableName = "users"
    override val tableColumns = UserColumns.ALL

    override val indexes : List<IndexArgs> = listOf(
        IndexArgs("inx_users_login_id", UserColumns.LOGIN_ID.name)
    )

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            UserColumns.FIRSTNAME to firstName,
            UserColumns.LASTNAME to lastName,
            UserColumns.VERIFIED to verifiedAccount,
            UserColumns.LOYALTY_POINTS to loyaltyPoints,
            UserColumns.LOGIN_ID to loginId
        )

    override fun mapRowToData(row : Array<Any?>) : UserData =
        UserData(
            id = castRowElement(row, UserColumns.ID),
            firstName = castRowElement(row, UserColumns.FIRSTNAME),
            lastName = castRowElement(row, UserColumns.LASTNAME),
            verifiedAccount = anyToBool(castRowElement(row, UserColumns.VERIFIED)),
            loyaltyPoints = castRowElement(row, UserColumns.LOYALTY_POINTS),
            loginId = castRowElement(row, UserColumns.LOGIN_ID)
        )

    override fun debugData() {
        println("User data: (\"$id\", \"$firstName\", \"$lastName\", \"$verifiedAccount\", \"$loyaltyPoints\", \"$loginId\")")
    }

    fun awardPoints(points : Int) : Int {
        loyaltyPoints += points
        return update()
    }

    fun usePoints(points : Int) : Int {
        loyaltyPoints -= points
        return update()
    }

    companion object {
        val EMPTY : UserData
            get() = UserData()

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null,
            orderByArgs : OrderByArgs? = null,
            limitArgs : LimitArgs? = null        
        ) : List<QueryResult<UserData>> {
            return EMPTY.queryDatabase(joinArgs, whereArgs, orderByArgs, limitArgs)
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)

        fun delete(
            id : Int
        ) : Int = UserData(id = id).delete()

        fun awardPoints(id : Int,
            points : Int
        ) : List<Int> {
            return queryDatabase(
                whereArgs = WhereArgs("${UserColumns.ID.name} = ?", listOf(id))
            ).map { result ->
                result.dataClass.awardPoints(points)
            }
        }

        fun usePoints(
            id : Int,
            points : Int
        ) : List<Int> {
            return queryDatabase(
                whereArgs = WhereArgs("${UserColumns.ID.name} = ?", listOf(id))
            ).map { result ->
                result.dataClass.usePoints(points)
            }
        }

        fun queryByLogIn(
            email : String
        ) : List<QueryResult<UserData>> {
            val joinArgs : JoinArgs = JoinArgs(
                joinType = "INNER",
                joinTable = LoginData.EMPTY.tableName,
                joinTable1Column = UserColumns.LOGIN_ID.name,
                joinTable2Column = LoginColumns.ID.name,
                joinSelectColumns = LoginColumns.COLUMN_NAMES
            )

            val whereArgs : WhereArgs = WhereArgs(
                whereClause = "${LoginData.EMPTY.tableName}.${LoginColumns.EMAIL.name} = ?",
                whereArgs = listOf(email)
            )

            return queryDatabase(
                joinArgs = joinArgs,
                whereArgs = whereArgs
            )
        }

        fun queryByToken(
            token: String
        ): List<QueryResult<UserData>> {

            val joinArgs = JoinArgs(
                joinType = "INNER",
                joinTable = SessionData.EMPTY.tableName,
                joinTable1Column = UserColumns.ID.name,
                joinTable2Column = SessionColumns.USER_ID.name,
                joinSelectColumns = SessionColumns.COLUMN_NAMES
            )

            val whereArgs = WhereArgs(
                whereClause = "${SessionData.EMPTY.tableName}.${SessionColumns.SESSION_TOKEN.name} = ?",
                whereArgs = listOf(token)
            )

            return queryDatabase(
                joinArgs = joinArgs,
                whereArgs = whereArgs
            )
        }
    }
}
