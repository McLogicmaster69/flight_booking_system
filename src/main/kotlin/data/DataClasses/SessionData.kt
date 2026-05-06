package data

import auth.*
import java.time.LocalDate

object SessionColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val USER_ID = Column<Int>("user_id", "INTEGER NOT NULL REFERENCES ${UserData.EMPTY.tableName}(id)")
    val SESSION_TOKEN = Column<String>("session_token", "STRING NOT NULL")
    val EXPIRY_DATE = Column<String>("expiry_date", "STRING NOT NULL")

    val ALL = listOf(ID, USER_ID, SESSION_TOKEN, EXPIRY_DATE)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class SessionData(

    override val id: Int = 0,
    var userId : Int = 0,
    var sessionToken : String = "",
    var expiryDate : LocalDate = LocalDate.now().plusWeeks(1L)

) : DataClass<SessionData>(id) {

    override val tableName = "sessions"
    override val tableColumns = SessionColumns.ALL

    override val indexes : List<IndexArgs> = listOf(
        IndexArgs("inx_sessions_user_id", SessionColumns.USER_ID.name),
        IndexArgs("inx_sessions_session_token", SessionColumns.SESSION_TOKEN.name),
        IndexArgs("inx_sessions_expiry_date", SessionColumns.EXPIRY_DATE.name)
    )

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            SessionColumns.USER_ID to userId,
            SessionColumns.SESSION_TOKEN to sessionToken.toString(),
            SessionColumns.EXPIRY_DATE to expiryDate
        )

    override fun mapRowToData(row : Array<Any?>) : SessionData =
        SessionData(
            id = castRowElement(row, SessionColumns.ID),
            userId = castRowElement(row, SessionColumns.USER_ID),
            sessionToken = castRowElement(row, SessionColumns.SESSION_TOKEN),
            expiryDate = castDateRowElement(row, SessionColumns.EXPIRY_DATE)
        )

    override fun debugData() {
        println("Session data: (\"$id\", \"$userId\", \"$sessionToken\", \"$expiryDate\")")
    }

    fun toTokenSession() : SessionToken {
        return SessionToken(sessionToken)
    }

    companion object {
        val EMPTY : SessionData
            get() = SessionData()

        fun queryDatabase (
            multipleJoinArgs : MultipleJoinArgs? = null,
            whereArgs : WhereArgs? = null,
            orderByArgs : OrderByArgs? = null,
            limitArgs : LimitArgs? = null,
            groupByArgs : GroupByArgs? = null   
        ) : List<QueryResult<SessionData>> {
            return EMPTY.queryDatabase(multipleJoinArgs, whereArgs, orderByArgs, limitArgs, groupByArgs)
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)

        fun delete(
            id : Int
        ) : Int {
            return SessionData(id = id).delete()
        }

        fun createSession(
            userId : Int
        ) : SessionData {
            val token : String = EMPTY.generateSecureToken()

            val session = SessionData(
                userId = userId,
                sessionToken = token
            )

            val id : Int = session.insertIntoDatabase()
            return SessionData (
                id = id,
                userId = userId,
                sessionToken = token
            )
        }

        fun queryDatabase(
            token : String
        ) : List<QueryResult<SessionData>> {
            val joinArgs : MultipleJoinArgs = MultipleJoinArgs(
                listOf(
                    JoinArgs(
                        joinType = "INNER",
                        rightTableJoin = UserData.EMPTY.tableName,
                        leftTableJoinColumn = SessionColumns.USER_ID.name,
                        rightTableJoinColumn = UserColumns.ID.name,
                        joinSelectColumns = UserColumns.ALL.map { it.name }
                    )
                )
            )

            val whereArgs : WhereArgs = WhereArgs(
                whereClause = "${EMPTY.tableName}.${SessionColumns.SESSION_TOKEN.name} = ?",
                whereArgs = listOf(token)
            )

            return queryDatabase(
                multipleJoinArgs = joinArgs,
                whereArgs = whereArgs
            )
        }

        fun deleteOld() {
            val whereArgs = WhereArgs(
                whereClause = "${SessionColumns.EXPIRY_DATE.name} < ?",
                whereArgs = listOf(LocalDate.now())
            )

            val query : List<QueryResult<SessionData>> = queryDatabase(whereArgs = whereArgs)

            query.forEach { session ->
                session.dataClass.delete()
            }
        }
    }
}
