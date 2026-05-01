package data

object CartItemColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val USER_ID = Column<Int>("user_id", "INTEGER NOT NULL REFERENCES ${UserData.EMPTY.tableName}(id)")
    val SEAT_ID = Column<Int>("seat_id", "INTEGER NOT NULL REFERENCES ${SeatData.EMPTY.tableName}(id)")

    val ALL = listOf(ID, USER_ID, SEAT_ID)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class CartItemData(

    override val id: Int = 0,
    var userId : Int = 0,
    var seatId : Int = 0

) : DataClass<CartItemData>(id) {

    override val tableName = "cart_items"
    override val tableColumns = CartItemColumns.ALL

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            CartItemColumns.USER_ID to userId,
            CartItemColumns.SEAT_ID to seatId
        )

    override fun mapRowToData(row : Array<Any?>) : CartItemData =
        CartItemData(
            id = castRowElement(row, CartItemColumns.ID),
            userId = castRowElement(row, CartItemColumns.USER_ID),
            seatId = castRowElement(row, CartItemColumns.SEAT_ID)
        )

    override fun debugData() {
        println("Cart item data: (\"$id\", \"$userId\", \"$seatId\")")
    }

    companion object {
        val EMPTY : CartItemData
            get() = CartItemData()

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null
        ) : List<QueryResult<CartItemData>> {
            return EMPTY.queryDatabase(joinArgs, whereArgs)
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)

        fun delete(id : Int) : Int {
            return CartItemData(id = id).delete()
        }
    }
}
