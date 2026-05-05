package data

import auth.*
import java.time.LocalDate

object StaffSessionColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val STAFF_ID = Column<Int>("staff_id", "INTEGER NOT NULL REFERENCES ${StaffData.EMPTY.tableName}(id)")
    val SESSION_TOKEN = Column<String>("session_token", "STRING NOT NULL")
    val EXPIRY_DATE = Column<String>("expiry_date", "STRING NOT NULL")

    val ALL = listOf(ID, STAFF_ID, SESSION_TOKEN, EXPIRY_DATE)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class StaffSessionData(

    override val id: Int = 0,
    var staffId : Int = 0,
    var sessionToken : String = "",
    var expiryDate : LocalDate = LocalDate.now().plusWeeks(1L)

) : DataClass<StaffSessionData>(id) {

    override val tableName = "staff_sessions"
    override val tableColumns = StaffSessionColumns.ALL

    override val indexes : List<IndexArgs> = listOf(
        IndexArgs("inx_staff_sessions_staff_id", StaffSessionColumns.STAFF_ID.name),
        IndexArgs("inx_staff_sessions_session_token", StaffSessionColumns.SESSION_TOKEN.name),
        IndexArgs("inx_staff_sessions_expiry_date", StaffSessionColumns.EXPIRY_DATE.name)
    )

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            StaffSessionColumns.STAFF_ID to staffId,
            StaffSessionColumns.SESSION_TOKEN to sessionToken.toString(),
            StaffSessionColumns.EXPIRY_DATE to expiryDate
        )

    override fun mapRowToData(row : Array<Any?>) : StaffSessionData =
        StaffSessionData(
            id = castRowElement(row, StaffSessionColumns.ID),
            staffId = castRowElement(row, StaffSessionColumns.STAFF_ID),
            sessionToken = castRowElement(row, StaffSessionColumns.SESSION_TOKEN),
            expiryDate = castDateRowElement(row, StaffSessionColumns.EXPIRY_DATE)
        )

    override fun debugData() {
        println("Staff session data: (\"$id\", \"$staffId\", \"$sessionToken\", \"$expiryDate\")")
    }

    fun toTokenSession() : StaffSessionToken {
        return StaffSessionToken(sessionToken)
    }

    companion object {
        val EMPTY : StaffSessionData
            get() = StaffSessionData()

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null,
            orderByArgs : OrderByArgs? = null,
            limitArgs : LimitArgs? = null        
        ) : List<QueryResult<StaffSessionData>> {
            return EMPTY.queryDatabase(joinArgs, whereArgs, orderByArgs, limitArgs)
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)

        fun delete(
            id : Int
        ) : Int {
            return StaffSessionData(id = id).delete()
        }

        fun createSession(
            staffId : Int
        ) : StaffSessionData {
            val token : String = EMPTY.generateSecureToken()

            val session = StaffSessionData(
                staffId = staffId,
                sessionToken = token
            )

            val id : Int = session.insertIntoDatabase()
            return StaffSessionData (
                id = id,
                staffId = staffId,
                sessionToken = token
            )
        }

        fun queryDatabase(
            token : String
        ) : List<QueryResult<StaffSessionData>> {
            val joinArgs : JoinArgs = JoinArgs(
                joinType = "INNER",
                joinTable = UserData.EMPTY.tableName,
                joinTable1Column = StaffSessionColumns.STAFF_ID.name,
                joinTable2Column = UserColumns.ID.name,
                joinSelectColumns = UserColumns.ALL.map { it.name }
            )

            val whereArgs : WhereArgs = WhereArgs(
                whereClause = "${EMPTY.tableName}.${StaffSessionColumns.SESSION_TOKEN.name} = ?",
                whereArgs = listOf(token)
            )

            return queryDatabase(
                joinArgs = joinArgs,
                whereArgs = whereArgs
            )
        }

        fun deleteOld() {
            val whereArgs = WhereArgs(
                whereClause = "${StaffSessionColumns.EXPIRY_DATE.name} < ?",
                whereArgs = listOf(LocalDate.now())
            )

            val query : List<QueryResult<StaffSessionData>> = queryDatabase(whereArgs = whereArgs)

            query.forEach { session ->
                session.dataClass.delete()
            }
        }
    }
}
