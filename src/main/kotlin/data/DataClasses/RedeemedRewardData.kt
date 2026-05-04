package data

import java.sql.Timestamp

object RedeemedRewardColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val USER_ID = Column<Int>("user_id", "INTEGER NOT NULL REFERENCES users(id)")
    val REWARD_NAME = Column<String>("reward_name", "STRING NOT NULL")
    val REDEEMED_AT = Column<Timestamp>("redeemed_at", "TIMESTAMP NOT NULL")

    val ALL = listOf(ID, USER_ID, REWARD_NAME, REDEEMED_AT)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class RedeemedRewardData(
    override val id: Int = 0,
    var userId: Int = 0,
    var rewardName: String = "",
    var redeemedAt: Timestamp = Timestamp(System.currentTimeMillis())
) : DataClass<RedeemedRewardData>(id) {

    override val tableName = "redeemed_rewards"
    override val tableColumns = RedeemedRewardColumns.ALL

    override fun mapDataToColumns(): Map<Column<*>, Any?> =
        mapOf(
            RedeemedRewardColumns.USER_ID to userId,
            RedeemedRewardColumns.REWARD_NAME to rewardName,
            RedeemedRewardColumns.REDEEMED_AT to redeemedAt
        )

    override fun mapRowToData(row: Array<Any?>): RedeemedRewardData {
        val rawRedeemedAt = row[tableColumns.indexOf(RedeemedRewardColumns.REDEEMED_AT)]
        val redeemedAtValue = when (rawRedeemedAt) {
            is Timestamp -> rawRedeemedAt
            is Long -> Timestamp(rawRedeemedAt)
            else -> throw IllegalStateException("Unexpected timestamp type: ${rawRedeemedAt?.javaClass}")
        }

        return RedeemedRewardData(
            id = castRowElement(row, RedeemedRewardColumns.ID),
            userId = castRowElement(row, RedeemedRewardColumns.USER_ID),
            rewardName = castRowElement(row, RedeemedRewardColumns.REWARD_NAME),
            redeemedAt = redeemedAtValue
        )
    }

    override fun debugData() {
        println("Redeemed Reward: (\"$id\", \"$userId\", \"$rewardName\", \"$redeemedAt\")")
    }

    companion object {
        val EMPTY: RedeemedRewardData
            get() = RedeemedRewardData()
    }
}
