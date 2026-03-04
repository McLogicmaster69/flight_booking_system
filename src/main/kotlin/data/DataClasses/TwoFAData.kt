package data

import java.sql.Timestamp

object TwoFAColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY REFERENCES users(id)")
    val TTL = Column<Timestamp>("ttl", "TIMESTAMP NOT NULL")
    val CODE_HASH = Column<String>("code_hash", "VARCHAR NOT NULL")
    val ATTEMPTS = Column<Int>("attempts", "INTEGER NOT NULL DEFAULT 0")

    val ALL = listOf(ID, TTL, CODE_HASH, ATTEMPTS)
}

data class TwoFAData(

    val id: Int = 0,
    var ttl: Timestamp = Timestamp(System.currentTimeMillis()),
    var code_hash: String = "",
    var attempts: Int = 0

) : DataClass<TwoFAData>() {

    override val tableName = "TwoFAData"
    override val tableColumns = TwoFAColumns.ALL

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            TwoFAColumns.ID to id,
            TwoFAColumns.TTL to ttl,
            TwoFAColumns.CODE_HASH to code_hash,
            TwoFAColumns.ATTEMPTS to attempts
        )

    override fun mapRowToData(row : Array<Any?>) : TwoFAData {
        val rawTtl = row[tableColumns.indexOf(TwoFAColumns.TTL)]

        val ttlValue = when (rawTtl) {
            is Timestamp -> rawTtl
            is Long -> Timestamp(rawTtl)
            else -> throw IllegalStateException("Unexpected ttl type: ${rawTtl?.javaClass}")
        }

        return TwoFAData(
            id = castRowElement(row, TwoFAColumns.ID),
            ttl = ttlValue,
            code_hash = castRowElement(row, TwoFAColumns.CODE_HASH),
            attempts = castRowElement(row, TwoFAColumns.ATTEMPTS)
        )
    }

    override fun debugData() {
        println("TwoFA data: (\"$id\", \"$ttl\", \"$code_hash\", \"$attempts\")")
    }

    fun update() {
        DatabaseManager.updateInDatabase(
            tableName,
            id,
            mapDataToColumns()
        )
    }

    companion object {
        val EMPTY : TwoFAData
            get() = TwoFAData()

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null) : List<QueryResult<TwoFAData>> {
            return EMPTY.queryDatabase(joinArgs, whereArgs)
        }

        fun deleteByUserId(userId: Int) {
            DatabaseManager.executeSQL(
                "DELETE FROM TwoFAData WHERE id = $userId"
            )
        }

    }
}