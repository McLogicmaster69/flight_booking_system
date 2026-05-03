package data

import auth.*

object AdminSessionColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val ADMIN_ID = Column<Int>("admin_id", "INTEGER NOT NULL REFERENCES ${AdminData.EMPTY.tableName}(id)")
    val SESSION_TOKEN = Column<String>("session_token", "STRING NOT NULL")

    val ALL = listOf(ID, ADMIN_ID, SESSION_TOKEN)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class AdminSessionData(

    override val id: Int = 0,
    var adminId : Int = 0,
    var sessionToken : String = ""

) : DataClass<AdminSessionData>(id) {

    override val tableName = "admin_sessions"
    override val tableColumns = AdminSessionColumns.ALL

    override val indexes : List<IndexArgs> = listOf(
        IndexArgs("inx_admin_sessions_admin_id", AdminSessionColumns.ADMIN_ID.name),
        IndexArgs("inx_admin_session_token", AdminSessionColumns.SESSION_TOKEN.name)
    )

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            AdminSessionColumns.ADMIN_ID to adminId,
            AdminSessionColumns.SESSION_TOKEN to sessionToken.toString()
        )

    override fun mapRowToData(row : Array<Any?>) : AdminSessionData =
        AdminSessionData(
            id = castRowElement(row, AdminSessionColumns.ID),
            adminId = castRowElement(row, AdminSessionColumns.ADMIN_ID),
            sessionToken = castRowElement(row, AdminSessionColumns.SESSION_TOKEN)
        )

    override fun debugData() {
        println("Admin session data: (\"$id\", \"$adminId\", \"$sessionToken\")")
    }

    fun toTokenSession() : AdminSessionToken {
        return AdminSessionToken(sessionToken)
    }

    companion object {
        val EMPTY : AdminSessionData
            get() = AdminSessionData()

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null,
            orderByArgs : OrderByArgs? = null,
            limitArgs : LimitArgs? = null
        ) : List<QueryResult<AdminSessionData>> {
            return EMPTY.queryDatabase(joinArgs, whereArgs, orderByArgs, limitArgs)
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)

        fun delete(
            id : Int
        ) : Int {
            return AdminSessionData(id = id).delete()
        }

        fun createSession(
            adminId : Int
        ) : AdminSessionData {
            val token : String = EMPTY.generateSecureToken()

            val session = AdminSessionData(
                adminId = adminId,
                sessionToken = token
            )

            val id : Int = session.insertIntoDatabase()
            return AdminSessionData (
                id = id,
                adminId = adminId,
                sessionToken = token
            )
        }

        fun queryDatabase(
            token : String
        ) : List<QueryResult<AdminSessionData>> {
            val joinArgs : JoinArgs = JoinArgs(
                joinType = "INNER",
                joinTable = UserData.EMPTY.tableName,
                joinTable1Column = AdminSessionColumns.ADMIN_ID.name,
                joinTable2Column = UserColumns.ID.name,
                joinSelectColumns = UserColumns.ALL.map { it.name }
            )

            val whereArgs : WhereArgs = WhereArgs(
                whereClause = "${EMPTY.tableName}.${AdminSessionColumns.SESSION_TOKEN.name} = ?",
                whereArgs = listOf(token)
            )

            return queryDatabase(
                joinArgs = joinArgs,
                whereArgs = whereArgs
            )
        }
    }
}
