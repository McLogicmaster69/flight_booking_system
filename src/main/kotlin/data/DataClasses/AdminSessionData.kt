package data

import auth.*
import java.time.LocalDate

object AdminSessionColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val ADMIN_ID = Column<Int>("admin_id", "INTEGER NOT NULL REFERENCES ${AdminData.EMPTY.tableName}(id)")
    val SESSION_TOKEN = Column<String>("session_token", "STRING NOT NULL")
    val EXPIRY_DATE = Column<String>("expiry_date", "STRING NOT NULL")

    val ALL = listOf(ID, ADMIN_ID, SESSION_TOKEN, EXPIRY_DATE)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class AdminSessionData(
    override val id: Int = 0,
    var adminId: Int = 0,
    var sessionToken: String = "",
    var expiryDate: LocalDate = LocalDate.now().plusWeeks(1L),
) : DataClass<AdminSessionData>(id) {
    override val tableName = "admin_sessions"
    override val tableColumns = AdminSessionColumns.ALL

    override val indexes: List<IndexArgs> =
        listOf(
            IndexArgs("inx_admin_sessions_admin_id", AdminSessionColumns.ADMIN_ID.name),
            IndexArgs("inx_admin_session_token", AdminSessionColumns.SESSION_TOKEN.name),
            IndexArgs("inx_admin_expiry_date", AdminSessionColumns.EXPIRY_DATE.name),
        )

    override fun mapDataToColumns(): Map<Column<*>, Any?> =
        mapOf(
            AdminSessionColumns.ADMIN_ID to adminId,
            AdminSessionColumns.SESSION_TOKEN to sessionToken.toString(),
            AdminSessionColumns.EXPIRY_DATE to expiryDate,
        )

    override fun mapRowToData(row: Array<Any?>): AdminSessionData =
        AdminSessionData(
            id = castRowElement(row, AdminSessionColumns.ID),
            adminId = castRowElement(row, AdminSessionColumns.ADMIN_ID),
            sessionToken = castRowElement(row, AdminSessionColumns.SESSION_TOKEN),
            expiryDate = castDateRowElement(row, AdminSessionColumns.EXPIRY_DATE),
        )

    override fun debugData() {
        println("Admin session data: (\"$id\", \"$adminId\", \"$sessionToken\", \"$expiryDate\")")
    }

    fun toTokenSession(): AdminSessionToken = AdminSessionToken(sessionToken)

    companion object {
        val EMPTY: AdminSessionData
            get() = AdminSessionData()

        fun queryDatabase(
            multipleJoinArgs: MultipleJoinArgs? = null,
            whereArgs: WhereArgs? = null,
            orderByArgs: OrderByArgs? = null,
            limitArgs: LimitArgs? = null,
            groupByArgs: GroupByArgs? = null,
        ): List<QueryResult<AdminSessionData>> =
            EMPTY.queryDatabase(multipleJoinArgs, whereArgs, orderByArgs, limitArgs, groupByArgs)

        fun updateTable(
            values: Map<Column<*>, Any?>,
            whereArgs: WhereArgs,
        ): Int = EMPTY.updateTable(values, whereArgs)

        fun delete(id: Int): Int = AdminSessionData(id = id).delete()

        fun createSession(adminId: Int): AdminSessionData {
            val token: String = EMPTY.generateSecureToken()

            val session =
                AdminSessionData(
                    adminId = adminId,
                    sessionToken = token,
                )

            val id: Int = session.insertIntoDatabase()
            return AdminSessionData(
                id = id,
                adminId = adminId,
                sessionToken = token,
            )
        }

        fun queryDatabase(token: String): List<QueryResult<AdminSessionData>> {
            val joinArgs: MultipleJoinArgs =
                MultipleJoinArgs(
                    listOf(
                        JoinArgs(
                            joinType = "INNER",
                            rightTableJoin = UserData.EMPTY.tableName,
                            leftTableJoinColumn = AdminSessionColumns.ADMIN_ID.name,
                            rightTableJoinColumn = UserColumns.ID.name,
                            joinSelectColumns = UserColumns.ALL.map { it.name },
                        ),
                    ),
                )

            val whereArgs: WhereArgs =
                WhereArgs(
                    whereClause = "${EMPTY.tableName}.${AdminSessionColumns.SESSION_TOKEN.name} = ?",
                    whereArgs = listOf(token),
                )

            return queryDatabase(
                multipleJoinArgs = joinArgs,
                whereArgs = whereArgs,
            )
        }

        fun deleteOld() {
            println("Cleaning ${EMPTY.tableName}")
            val whereArgs =
                WhereArgs(
                    whereClause = "${AdminSessionColumns.EXPIRY_DATE.name} < ?",
                    whereArgs = listOf(LocalDate.now()),
                )

            val query: List<QueryResult<AdminSessionData>> = queryDatabase(whereArgs = whereArgs)

            query.forEach { session ->
                session.dataClass.delete()
            }
        }
    }
}
