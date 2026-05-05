package routes

import data.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.pebbletemplates.pebble.PebbleEngine
import java.io.StringWriter
import utils.jsMode
import utils.timed
import auth.*
import com.stripe.Stripe
import com.stripe.model.Refund
import com.stripe.param.RefundCreateParams
import utils.EmailService

fun Route.manageRoutes() {
    get("/manage") { call.handleManageLoad() }
    post("/handleBookingSearch") { call.handleBookingSearch() }
    post("/manage/refund") { call.handleRefundBooking() }
}

private suspend fun ApplicationCall.handleManageLoad() {
    timed("T0_manage", jsMode()) {
        val logged_state = loggedIn()

        var userBookings: List<Array<Any?>> = emptyList()

        if (logged_state.logged_in && logged_state.session != null) {
            val token = logged_state.session.token
            val users = UserData.queryByToken(token)
            val user = users.firstOrNull()?.dataClass

            if (user != null) {
                val bookerQuery = BookerData.queryDatabase(
                    whereArgs = WhereArgs("${BookerColumns.USER_ID.name} = ?", listOf(user.id))
                )

                if (bookerQuery.isNotEmpty()) {
                    val booker = bookerQuery.first().dataClass
                    userBookings = getEnrichedBookingsForBooker(booker.id)
                }
            }
        }

        val model = mapOf(
            "title" to "Manage Bookings",
            "inNav" to true,
            "logged_in" to logged_state.logged_in,
            "userBookings" to userBookings,
            "searchResults" to emptyList<Array<Any?>>()
        )

        val pebble = getEngine()
        val template = pebble.getTemplate("manage/index.peb")
        val writer = StringWriter()
        fullEvaluate(template, writer, model)
        respondText(writer.toString(), ContentType.Text.Html)
    }
}

private suspend fun ApplicationCall.handleBookingSearch() {
    timed("T1_manageSearch", jsMode()) {
        val params = receiveParameters()
        val ref = params["ref"]?.trim()
        val last = params["last"]?.trim()

        if (ref.isNullOrBlank() || last.isNullOrBlank()) {
            respondRedirect("/manage")
            return@timed
        }

        val loggedState = loggedIn()
        var userBookings: List<Array<Any?>> = emptyList()

        if (loggedState.logged_in && loggedState.session != null) {
            val token = loggedState.session.token
            val users = UserData.queryByToken(token)
            val user = users.firstOrNull()?.dataClass

            if (user != null) {
                val bookerQuery = BookerData.queryDatabase(
                    whereArgs = WhereArgs("${BookerColumns.USER_ID.name} = ?", listOf(user.id))
                )

                if (bookerQuery.isNotEmpty()) {
                    val booker = bookerQuery.first().dataClass
                    userBookings = getEnrichedBookingsForBooker(booker.id)
                }
            }
        }

        val bookingRows = DatabaseManager.queryTable(
            table = BookingData.EMPTY.tableName,
            columns = BookingColumns.COLUMN_NAMES,
            whereArgs = WhereArgs(
                "LOWER(${BookingColumns.BOOKING_REFERENCE.name}) = LOWER(?) AND LOWER(${BookingColumns.LASTNAME.name}) = LOWER(?)",
                listOf(ref, last)
            )
        )

        val results = enrichBookingRowsFromSeats(bookingRows)

        val model = mapOf(
            "title" to "Manage Bookings",
            "inNav" to true,
            "searchResults" to results,
            "logged_in" to loggedState.logged_in,
            "userBookings" to userBookings
        )

        val pebble = getEngine()
        val template = pebble.getTemplate("manage/index.peb")
        val writer = StringWriter()
        fullEvaluate(template, writer, model)
        respondText(writer.toString(), ContentType.Text.Html)
    }
}

private suspend fun ApplicationCall.handleRefundBooking() {
    timed("T2_manageRefund", jsMode()) {
        val params = receiveParameters()
        val ref = params["bookingRef"]?.trim()
        val last = params["last"]?.trim()

        if (ref.isNullOrBlank() || last.isNullOrBlank()) {
            respondRedirect("/manage")
            return@timed
        }

        val bookingRows = DatabaseManager.queryTable(
            table = BookingData.EMPTY.tableName,
            columns = BookingColumns.COLUMN_NAMES,
            whereArgs = WhereArgs(
                "LOWER(${BookingColumns.BOOKING_REFERENCE.name}) = LOWER(?) AND LOWER(${BookingColumns.LASTNAME.name}) = LOWER(?)",
                listOf(ref, last)
            )
        )

        if (bookingRows.isEmpty()) {
            respondText("Booking not found", status = HttpStatusCode.NotFound)
            return@timed
        }

        val bookingIds = bookingRows.map { it[0] as Int }
        val bookerId = bookingRows.first()[1] as Int
        val paymentIntentId = bookingRows.first()[5]?.toString()
        val amountPaid = bookingRows.sumOf { (it[6] as? Number)?.toLong() ?: 0L }
        val alreadyRefunded = bookingRows.any { it[7] != null }

        if (alreadyRefunded) {
            respondText("This booking has already been refunded", status = HttpStatusCode.BadRequest)
            return@timed
        }

        if (paymentIntentId.isNullOrBlank() || amountPaid <= 0L) {
            respondText("Missing payment details for this booking", status = HttpStatusCode.BadRequest)
            return@timed
        }

        val refundAmount = amountPaid

        Stripe.apiKey = "sk_test_51TCfFDDPNfjFe9Utry1rCfJEQJ0YIASnPd7O0SkI3Ewo7COIifnBpfEsP7xPhx5c1WJ8ndpJKxi1IrWqJEJfoyEL00engmejFe"

        val refundParams = RefundCreateParams.builder()
            .setPaymentIntent(paymentIntentId)
            .setAmount(refundAmount)
            .setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
            .putMetadata("booking_reference", ref)
            .build()

        val refund = Refund.create(refundParams)

        releaseBookedSeatsForBookingIds(bookingIds)

        markBookingsRefunded(
            bookingIds = bookingIds,
            refundId = refund.id,
            refundAmount = refundAmount
        )

        val email = getBookerEmail(bookerId)

        if (!email.isNullOrBlank()) {
            EmailService.sendRefundConfirmation(
                to = email,
                reference = ref,
                refundAmount = refundAmount,
                refundId = refund.id
            )
        }

        respondRedirect("/manage")
    }
}

private fun getEnrichedBookingsForBooker(bookerId: Int): List<Array<Any?>> {
    val bookingRows = DatabaseManager.queryTable(
        table = BookingData.EMPTY.tableName,
        columns = BookingColumns.COLUMN_NAMES,
        whereArgs = WhereArgs("${BookingColumns.BOOKER_ID.name} = ?", listOf(bookerId))
    )

    return enrichBookingRowsFromSeats(bookingRows)
}

private fun enrichBookingRowsFromSeats(bookingRows: List<Array<Any?>>): List<Array<Any?>> {
    val results = mutableListOf<Array<Any?>>()

    for (row in bookingRows) {
        val bookingId = row[0] as Int
        val lastname = row[3] as? String ?: ""
        val bookingReference = row[4] as? String ?: ""

        val bookedSeat = BookedSeatData.queryDatabase(
            whereArgs = WhereArgs("${BookedSeatColumns.BOOKING_ID.name} = ?", listOf(bookingId))
        ).firstOrNull()?.dataClass

        val seat = bookedSeat?.seatId?.let { seatId ->
            SeatData.queryDatabase(
                whereArgs = WhereArgs("id = ?", listOf(seatId))
            ).firstOrNull()?.dataClass
        }

        val flight = seat?.flightId?.let { seatFlightId ->
            FlightData.queryDatabase(
                whereArgs = WhereArgs("id = ?", listOf(seatFlightId))
            ).firstOrNull()?.dataClass
        }

        val seatClass = seat?.classId?.let { classId ->
            ClassData.queryDatabase(
                whereArgs = WhereArgs("id = ?", listOf(classId))
            ).firstOrNull()?.dataClass
        }

        val ticketType = seat?.typeId?.let { typeId ->
            TicketTypeData.queryDatabase(
                whereArgs = WhereArgs("id = ?", listOf(typeId))
            ).firstOrNull()?.dataClass
        }

        val route = flight?.routeId?.let { routeId ->
            RouteData.queryDatabase(
                whereArgs = WhereArgs("id = ?", listOf(routeId))
            ).firstOrNull()?.dataClass
        }

        val startDestination = route?.startDestination?.let { destinationId ->
            DestinationData.queryDatabase(
                whereArgs = WhereArgs("id = ?", listOf(destinationId))
            ).firstOrNull()?.dataClass
        }

        val endDestination = route?.endDestination?.let { destinationId ->
            DestinationData.queryDatabase(
                whereArgs = WhereArgs("id = ?", listOf(destinationId))
            ).firstOrNull()?.dataClass
        }

        results.add(
            arrayOf(
                bookingReference,
                lastname,
                flight?.date?.toString() ?: "Not assigned",
                flight?.time?.toString() ?: "Not assigned",
                seat?.number ?: "Not assigned",
                seatClass?.name ?: "Not assigned",
                ticketType?.name ?: "Not assigned",
                startDestination?.cityName ?: "Not assigned",
                endDestination?.cityName ?: "Not assigned"
            )
        )
    }

    return results
}

private fun releaseBookedSeatsForBookingIds(bookingIds: List<Int>) {
    for (bookingId in bookingIds) {
        val bookedSeats = BookedSeatData.queryDatabase(
            whereArgs = WhereArgs(
                "${BookedSeatColumns.BOOKING_ID.name} = ?",
                listOf(bookingId)
            )
        )

        for (bookedSeat in bookedSeats) {
            BookedSeatData.delete(bookedSeat.dataClass.id)
        }
    }
}

private fun markBookingsRefunded(
    bookingIds: List<Int>,
    refundId: String,
    refundAmount: Long
) {
    for (bookingId in bookingIds) {
        BookingData.updateTable(
            values = mapOf(
                BookingColumns.REFUND_STATUS to "REFUNDED_FULL",
                BookingColumns.STRIPE_REFUND_ID to refundId,
                BookingColumns.REFUND_AMOUNT to refundAmount.toInt()
            ),
            whereArgs = WhereArgs(
                "${BookingColumns.ID.name} = ?",
                listOf(bookingId)
            )
        )
    }
}

private fun getBookerEmail(bookerId: Int): String? {
    val booker = BookerData.queryDatabase(
        whereArgs = WhereArgs("${BookerColumns.ID.name} = ?", listOf(bookerId))
    ).firstOrNull()?.dataClass ?: return null

    booker.userId?.let { userId ->
        val user = UserData.queryDatabase(
            whereArgs = WhereArgs("${UserColumns.ID.name} = ?", listOf(userId))
        ).firstOrNull()?.dataClass ?: return null

        val login = LoginData.queryDatabase(
            whereArgs = WhereArgs("${LoginColumns.ID.name} = ?", listOf(user.loginId))
        ).firstOrNull()?.dataClass

        return login?.email
    }

    booker.guestId?.let { guestId ->
        val guest = GuestData.queryDatabase(
            whereArgs = WhereArgs("${GuestColumns.ID.name} = ?", listOf(guestId))
        ).firstOrNull()?.dataClass

        return guest?.email
    }

    return null
}