package data

import data.DataClass

data class UserData(

    val id: Int = 0,
    val firstName: String? = null,
    val lastName: String? = null,
    val verifiedAccount: Boolean? = null,
    val loginId: Int = 0

) : DataClass() {

    override val tableName = "users"
    override val tableColumns = "id INTEGER PRIMARY KEY, firstname VARCHAR, lastname VARCHAR, verified_account BOOL, login_id INTEGER NOT NULL"

    companion object {
        val EMPTY = UserData()
    }
}
