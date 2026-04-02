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
            columns = listOf("id", "booker_id", "flight_id", "passport_number", "lastname", "booking_reference"),
            whereArgs = WhereArgs(
                "LOWER(booking_reference) = LOWER(?) AND LOWER(lastname) = LOWER(?)",
                listOf(ref, last)
            )
        )

        val results = mutableListOf<Array<Any?>>()

        for (row in bookingRows) {
            val flightId = row[2] as Int
            val lastname = row[4] as? String ?: ""
            val bookingReference = row[5] as? String ?: ""

            val flight = FlightData.queryDatabase(flightId).firstOrNull()?.dataClass

            if (flight == null) {
                results.add(
                    arrayOf(
                        bookingReference,
                        lastname,
                        "Not assigned",
                        "Not assigned",
                        "Not assigned"
                    )
                )
            } else {
                results.add(
                    arrayOf(
                        bookingReference,
                        lastname,
                        flight.date.toString(),
                        flight.time.toString(),
                        flight.id
                    )
                )
            }
        }

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
        columns = listOf("id", "booker_id", "flight_id", "passport_number", "lastname", "booking_reference"),
        whereArgs = WhereArgs("booker_id = ?", listOf(bookerId))
    )

    val enrichedBookings = mutableListOf<Array<Any?>>()

    for (row in bookingRows) {
        val flightId = row[2] as Int
        val lastname = row[4] as? String ?: ""
        val bookingReference = row[5] as? String ?: ""

        val flight = FlightData.queryDatabase(flightId).firstOrNull()?.dataClass

        if (flight == null) {
            enrichedBookings.add(
                arrayOf(
                    bookingReference,
                    lastname,
                    "Not assigned",
                    "Not assigned",
                    "Not assigned"
                )
            )
            continue
        }

        enrichedBookings.add(
            arrayOf(
                bookingReference,
                lastname,
                flight.date.toString(),
                flight.time.toString(),
                flight.id
            )
        )
    }

    return enrichedBookings
}
