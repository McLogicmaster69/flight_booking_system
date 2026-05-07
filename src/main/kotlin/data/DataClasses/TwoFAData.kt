package data

import java.sql.Timestamp

object TwoFAColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY")
    val USER_ID = Column<Int>("user_id", "INTEGER REFERENCES ${UserData.EMPTY.tableName}(id)")
    val TTL = Column<Timestamp>("ttl", "TIMESTAMP NOT NULL")
    val CODE_HASH = Column<String>("code_hash", "VARCHAR NOT NULL")
    val ATTEMPTS = Column<Int>("attempts", "INTEGER NOT NULL DEFAULT 0")
    val SESSION_TOKEN = Column<String>("session_token", "STRING NOT NULL")

    val ALL = listOf(ID, USER_ID, TTL, CODE_HASH, ATTEMPTS, SESSION_TOKEN)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class TwoFAData(
    override val id: Int = 0,
    var userId: Int = 0,
    var ttl: Timestamp = Timestamp(System.currentTimeMillis()),
    var code_hash: String = "",
    var attempts: Int = 0,
    var sessionToken: String = "",
) : DataClass<TwoFAData>(id) {
    override val tableName = "two_fa_data"
    override val tableColumns = TwoFAColumns.ALL

    override val indexes: List<IndexArgs> =
        listOf(
            IndexArgs("inx_two_fa_data_sessions_staff_id", TwoFAColumns.USER_ID.name),
        )

    override fun mapDataToColumns(): Map<Column<*>, Any?> =
        mapOf(
            TwoFAColumns.ID to id,
            TwoFAColumns.USER_ID to userId,
            TwoFAColumns.TTL to ttl,
            TwoFAColumns.CODE_HASH to code_hash,
            TwoFAColumns.ATTEMPTS to attempts,
            TwoFAColumns.SESSION_TOKEN to sessionToken,
        )

    override fun mapRowToData(row: Array<Any?>): TwoFAData {
        val rawTtl = row[tableColumns.indexOf(TwoFAColumns.TTL)]

        val ttlValue =
            when (rawTtl) {
                is Timestamp -> rawTtl
                is Long -> Timestamp(rawTtl)
                else -> throw IllegalStateException("Unexpected ttl type: ${rawTtl?.javaClass}")
            }

        return TwoFAData(
            id = castRowElement(row, TwoFAColumns.ID),
            userId = castRowElement(row, TwoFAColumns.USER_ID),
            ttl = ttlValue,
            code_hash = castRowElement(row, TwoFAColumns.CODE_HASH),
            attempts = castRowElement(row, TwoFAColumns.ATTEMPTS),
            sessionToken = castRowElement(row, TwoFAColumns.SESSION_TOKEN),
        )
    }

    override fun debugData() {
        println("TwoFA data: (\"$id\", \"$userId\", \"$ttl\", \"$code_hash\", \"$attempts\", \"$sessionToken\")")
    }

    companion object {
        val EMPTY: TwoFAData
            get() = TwoFAData()

        fun queryDatabase(
            multipleJoinArgs: MultipleJoinArgs? = null,
            whereArgs: WhereArgs? = null,
            orderByArgs: OrderByArgs? = null,
            limitArgs: LimitArgs? = null,
            groupByArgs: GroupByArgs? = null,
        ): List<QueryResult<TwoFAData>> =
            EMPTY.queryDatabase(multipleJoinArgs, whereArgs, orderByArgs, limitArgs, groupByArgs)

        fun updateTable(
            values: Map<Column<*>, Any?>,
            whereArgs: WhereArgs,
        ): Int = EMPTY.updateTable(values, whereArgs)

        fun delete(id: Int): Int = TwoFAData(id = id).delete()

        fun deleteByUserId(userId: Int): Int =
            DatabaseManager.deleteFromTable(
                EMPTY.tableName,
                WhereArgs("${TwoFAColumns.USER_ID.name} = ?", listOf(userId)),
            )

        fun deleteByToken(token: String): Int =
            DatabaseManager.deleteFromTable(
                EMPTY.tableName,
                WhereArgs("${TwoFAColumns.SESSION_TOKEN.name} = ?", listOf(token)),
            )
    }
}
