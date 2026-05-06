package data

object BookingColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val BOOKER_ID = Column<Int>("booker_id", "INTEGER NOT NULL REFERENCES ${BookerData.EMPTY.tableName}(id)")
    val PASSPORT_NUMBER = Column<String?>("passport_number", "STRING")
    val LASTNAME = Column<String?>("lastname", "STRING")
    val BOOKING_REFERENCE = Column<String>("booking_reference", "STRING NOT NULL")
    val PAYMENT_INTENT_ID = Column<String?>("payment_intent_id", "STRING")
    val AMOUNT_PAID = Column<Int?>("amount_paid", "INTEGER")
    val REFUND_STATUS = Column<String?>("refund_status", "STRING")
    val STRIPE_REFUND_ID = Column<String?>("stripe_refund_id", "STRING")
    val REFUND_AMOUNT = Column<Int?>("refund_amount", "INTEGER")

    val ALL = listOf(ID, BOOKER_ID, PASSPORT_NUMBER, LASTNAME, BOOKING_REFERENCE, PAYMENT_INTENT_ID, AMOUNT_PAID, REFUND_STATUS, STRIPE_REFUND_ID, REFUND_AMOUNT)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class BookingData(

    override val id : Int = 0,
    var bookerId : Int = 0,
    var passportNumber : String? = null,
    var lastname : String? = null,
    var bookingReference : String = "",
    var paymentIntentId: String? = null,
    var amountPaid: Int? = null,
    var refundStatus: String? = null,
    var stripeRefundId: String? = null,
    var refundAmount: Int? = null

) : DataClass<BookingData>(id) {

    override val tableName = "bookings"
    override val tableColumns = BookingColumns.ALL

    override val indexes : List<IndexArgs> = listOf(
        IndexArgs("inx_bookings_booker_id", BookingColumns.BOOKER_ID.name)
    )

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            BookingColumns.BOOKER_ID to bookerId,
            BookingColumns.PASSPORT_NUMBER to passportNumber,
            BookingColumns.LASTNAME to lastname,
            BookingColumns.BOOKING_REFERENCE to bookingReference,
            BookingColumns.PAYMENT_INTENT_ID to paymentIntentId,
            BookingColumns.AMOUNT_PAID to amountPaid,
            BookingColumns.REFUND_STATUS to refundStatus,
            BookingColumns.STRIPE_REFUND_ID to stripeRefundId,
            BookingColumns.REFUND_AMOUNT to refundAmount
        )

    override fun mapRowToData(row: Array<Any?>): BookingData =
        BookingData(
            id = castRowElement(row, BookingColumns.ID),
            bookerId = castRowElement(row, BookingColumns.BOOKER_ID),
            passportNumber = castRowElement(row, BookingColumns.PASSPORT_NUMBER),
            lastname = castRowElement(row, BookingColumns.LASTNAME),
            bookingReference = castRowElement(row, BookingColumns.BOOKING_REFERENCE),
            paymentIntentId = castRowElement(row, BookingColumns.PAYMENT_INTENT_ID),
            amountPaid = castRowElement(row, BookingColumns.AMOUNT_PAID),
            refundStatus = castRowElement(row, BookingColumns.REFUND_STATUS),
            stripeRefundId = castRowElement(row, BookingColumns.STRIPE_REFUND_ID),
            refundAmount = castRowElement(row, BookingColumns.REFUND_AMOUNT)
        )

    override fun debugData() {
        println("Booking data: (\"$id\", \"$bookerId\", \"$passportNumber\", \"$lastname\", \"$bookingReference\", \"$paymentIntentId\", \"$amountPaid\", \"$refundStatus\", \"$stripeRefundId\", \"$refundAmount\")")
    }

    companion object {
        val EMPTY : BookingData
            get() = BookingData()

        fun queryDatabase (
            multipleJoinArgs : MultipleJoinArgs? = null,
            whereArgs : WhereArgs? = null,
            orderByArgs : OrderByArgs? = null,
            limitArgs : LimitArgs? = null,
            groupByArgs : GroupByArgs? = null   
        ) : List<QueryResult<BookingData>> {
            return EMPTY.queryDatabase(multipleJoinArgs, whereArgs, orderByArgs, limitArgs, groupByArgs)
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)

        fun delete(id : Int) : Int {
            return BookingData(id = id).delete()
        }
    }
}
