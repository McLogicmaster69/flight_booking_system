package data

object PaymentMethodColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val USER_ID = Column<Int>("user_id", "INTEGER NOT NULL REFERENCES ${UserData.EMPTY.tableName}(id)")
    val PAYMENT_TOKEN = Column<String?>("payment_token", "VARCHAR")
    val LAST_FOUR = Column<String?>("last_ four", "VARCHAR")
    val BRAND = Column<String?>("brand", "VARCHAR")
    val EXP_MONTH = Column<Int?>("exp_month", "INTEGER")
    val EXP_YEAR = Column<Int?>("exp_year", "INTEGER")

    val ALL = listOf(ID, USER_ID, PAYMENT_TOKEN, LAST_FOUR, BRAND, EXP_MONTH, EXP_YEAR)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class PaymentMethodData(

    override val id: Int = 0,
    var userId : Int = 0,
    var paymentToken : String? = null,
    var lastFour : String? = null,
    var brand : String? = null,
    var expMonth : Int? = null,
    var expYear : Int? = null

) : DataClass<PaymentMethodData>(id) {

    override val tableName = "payment_methods"
    override val tableColumns = PaymentMethodColumns.ALL

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            PaymentMethodColumns.USER_ID to userId,
            PaymentMethodColumns.PAYMENT_TOKEN to paymentToken,
            PaymentMethodColumns.LAST_FOUR to lastFour,
            PaymentMethodColumns.BRAND to brand,
            PaymentMethodColumns.EXP_MONTH to expMonth,
            PaymentMethodColumns.EXP_YEAR to expYear
        )

    override fun mapRowToData(row : Array<Any?>) : PaymentMethodData =
        PaymentMethodData(
            id = castRowElement(row, PaymentMethodColumns.ID),
            userId = castRowElement(row, PaymentMethodColumns.USER_ID),
            paymentToken = castRowElement(row, PaymentMethodColumns.PAYMENT_TOKEN),
            lastFour = castRowElement(row, PaymentMethodColumns.LAST_FOUR),
            brand = castRowElement(row, PaymentMethodColumns.BRAND),
            expMonth = castRowElement(row, PaymentMethodColumns.EXP_MONTH),
            expYear = castRowElement(row, PaymentMethodColumns.EXP_YEAR)
        )

    override fun debugData() {
        println("Payment method data: (\"$id\", \"$userId\", \"$paymentToken\", \"$lastFour\", \"$brand\", \"$expMonth\", \"$expYear\")")
    }

    companion object {
        val EMPTY : PaymentMethodData
            get() = PaymentMethodData()

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null,
            orderByArgs : OrderByArgs? = null,
            limitArgs : LimitArgs? = null
        ) : List<QueryResult<PaymentMethodData>> {
            return EMPTY.queryDatabase(joinArgs, whereArgs, orderByArgs, limitArgs)
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)

        fun delete(id : Int) : Int {
            return PaymentMethodData(id = id).delete()
        }
    }
}
