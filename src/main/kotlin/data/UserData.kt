package data

import data.DatabaseManager
import data.DataClass
import data.anyToBool

data class UserData(

    val id: Int = 0,
    var firstName: String? = null,
    var lastName: String? = null,
    var verifiedAccount: Boolean? = null,
    var loginId: Int = 0

) : DataClass() {

    override val tableName : String = "users"

    override val tableColumns : Map<String, String> = mapOf(
        "id" to "INTEGER PRIMARY KEY",
        "firstname" to "VARCHAR",
        "lastname" to "VARCHAR",
        "verified_account" to "BOOL",
        "login_id" to "INTEGER NOT NULL"
    )

    override fun insertIntoDatabase() {
        UserData.insertIntoDatabase(this)
    }

    override fun mapDataToKeys () : Map<String, Any?> {
        val keys = tableColumns.keys.toList()
        return mapOf(
            keys[1] to firstName,
            keys[2] to lastName,
            keys[3] to verifiedAccount,
            keys[4] to loginId
        )
    }


    companion object {
        val EMPTY : UserData
            get() = UserData()

        fun insertIntoDatabase (data : UserData) {
            val values = data.mapDataToKeys()
            DatabaseManager.insertIntoTable(data.tableName, values)
        }

        fun queryDatabase (whereClause : String? = null, whereArgs : List<Any?> = emptyList()) : List<UserData> {
            val empty = EMPTY
            val rows = DatabaseManager.queryTable(empty.tableName, empty.tableColumns.keys.toList(), whereClause, whereArgs)
            val output : MutableList<UserData> = mutableListOf<UserData>()

            for (row in rows) {
                val data = UserData(
                    id = row[0] as Int,
                    firstName = row[1] as? String,
                    lastName = row[2] as? String,
                    verifiedAccount =  anyToBool(row[3]),
                    loginId = row[4] as Int
                )

                output.add(data)
            }

            return output.toList()
        }
    }
}
