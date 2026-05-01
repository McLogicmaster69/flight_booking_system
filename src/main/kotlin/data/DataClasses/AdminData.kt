package data

object AdminColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val LOGIN_ID = Column<Int>("login_id", "INTEGER NOT NULL REFERENCES ${LoginData.EMPTY.tableName}(id)")

    val ALL = listOf(ID, LOGIN_ID)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class AdminData(

    override val id: Int = 0,
    var loginId : Int = 0,

) : DataClass<AdminData>(id) {

    override val tableName = "admins"
    override val tableColumns = AdminColumns.ALL

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            AdminColumns.LOGIN_ID to loginId
        )

    override fun mapRowToData(row : Array<Any?>) : AdminData =
        AdminData(
            id = castRowElement(row, AdminColumns.ID),
            loginId = castRowElement(row, AdminColumns.LOGIN_ID)
        )

    override fun debugData() {
        println("Admin data: (\"$id\", \"$loginId\")")
    }

    companion object {
        val EMPTY : AdminData
            get() = AdminData()

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null) : List<QueryResult<AdminData>> {
            return EMPTY.queryDatabase(joinArgs, whereArgs)
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)

        fun delete(id : Int) : Int {
            return AdminData(id = id).delete()
        }

        fun queryByLogIn(
            email : String
        ) : List<QueryResult<AdminData>> {
            val joinArgs : JoinArgs = JoinArgs(
                joinType = "INNER",
                joinTable = LoginData.EMPTY.tableName,
                joinTable1Column = AdminColumns.LOGIN_ID.name,
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
