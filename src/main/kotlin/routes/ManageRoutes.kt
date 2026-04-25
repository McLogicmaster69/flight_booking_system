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

fun Route.manageRoutes() {
    get("/manage") { call.handleManageLoad() }
    post("/handleBookingSearch") { call.handleBookingSearch() }
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
                    whereArgs = WhereArgs("user_id = ?", listOf(user.id))
                )

                if (bookerQuery.isNotEmpty()) {
                    val booker = bookerQuery.first().dataClass
                    userBookings = getEnrichedBookingsForBooker(booker.id)
                }
            }
        }

        val model = mapOf(
            "title" to "Manage Bookings",
            "isNav" to true,
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
                    whereArgs = WhereArgs("user_id = ?", listOf(user.id))
                )

                if (bookerQuery.isNotEmpty()) {
                    val booker = bookerQuery.first().dataClass
                    userBookings = getEnrichedBookingsForBooker(booker.id)
                }
            }
        }

        val bookingRows = DatabaseManager.queryTable(
            table = "bookings",
            columns = listOf("id", "booker_id", "passport_number", "lastname", "booking_reference"),
            whereArgs = WhereArgs(
                "LOWER(booking_reference) = LOWER(?) AND LOWER(lastname) = LOWER(?)",
                listOf(ref, last)
            )
        )

        val results = enrichBookingRowsFromSeats(bookingRows)

        val model = mapOf(
            "title" to "Manage Bookings",
            "isNav" to true,
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

private fun getEnrichedBookingsForBooker(bookerId: Int): List<Array<Any?>> {
    val bookingRows = DatabaseManager.queryTable(
        table = "bookings",
        columns = listOf("id", "booker_id", "passport_number", "lastname", "booking_reference"),
        whereArgs = WhereArgs("booker_id = ?", listOf(bookerId))
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
            whereArgs = WhereArgs("booking_id = ?", listOf(bookingId))
        ).firstOrNull()?.dataClass

        val seat = bookedSeat?.seatId?.let { seatId ->
            SeatData.queryDatabase(
                whereArgs = WhereArgs("id = ?", listOf(seatId))
            ).firstOrNull()?.dataClass
        }

        val flight = seat?.flightId?.let { seatFlightId ->
            FlightData.queryDatabase(seatFlightId).firstOrNull()?.dataClass
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

        results.add(
            arrayOf(
                bookingReference,
                lastname,
                flight?.date?.toString() ?: "Not assigned",
                flight?.time?.toString() ?: "Not assigned",
                seat?.number ?: "Not assigned",
                seatClass?.name ?: "Not assigned",
                ticketType?.name ?: "Not assigned"
            )
        )
    }

    return results
}