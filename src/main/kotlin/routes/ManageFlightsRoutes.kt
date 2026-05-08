package routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.StringWriter
import utils.timed
import utils.jsMode
import auth.*
import data.*
import io.ktor.server.request.*
import java.time.LocalDate
import java.time.LocalTime
import com.stripe.model.Refund
import com.stripe.param.RefundCreateParams
import utils.EmailService

/**
 * Registers routing for administrators to handle CRUD operations and view details for all flights.
 */
fun Route.manageFlightsRoutes() {
    get("/manageFlights") { call.handleManageFlightsLoad() }
    get("/manageFlights/edit/{id}") { call.handleEditFlightLoad() }
    get("/manageFlights/details/{id}") { call.handleFlightDetailsLoad() }
    post("/manageFlights/create") { call.handleCreateFlightPost() }
    post("/manageFlights/update") { call.handleUpdateFlightPost() }
    post("/manageFlights/delete") { call.handleDeleteFlightPost() }
}

data class FlightView(
    val id: Int,
    val routeId: Int,
    val routeName: String?,
    val planeId: Int,
    val planeRegistrationCode: String?,
    val date: LocalDate,
    val time: LocalTime,
)

data class FlightReservationView(
    val bookingId: Int,
    val bookingReference: String,
    val bookerEmail: String?,
    val seatId: Int,
    val amountPaid: Long?,
    val amountPaidDisplay: String,
    val refundStatus: String?,
)

/**
 * Handles rendering the main dashboard to view and search existing flights using pagination logic.
 */
private suspend fun ApplicationCall.handleManageFlightsLoad() {
    timed("T0_manage_flights", jsMode()) {
        if (!requireAdmin()) return@timed

        val pebble = getEngine()

        val search = request.queryParameters["search"]?.trim().orEmpty()
        val page = request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val pageSize = 5

        val routes = RouteData.queryDatabase().map { it.dataClass }
        val planes = PlaneData.queryDatabase().map { it.dataClass }
        val destinations = DestinationData.queryDatabase().map { it.dataClass }

        fun routeName(route: RouteData): String {
            val start =
                DestinationData
                    .queryDatabase(route.startDestination)
                    .firstOrNull()
                    ?.dataClass
                    ?.cityName ?: "Unknown"
            val end =
                DestinationData
                    .queryDatabase(route.endDestination)
                    .firstOrNull()
                    ?.dataClass
                    ?.cityName ?: "Unknown"
            return "$start to $end"
        }

        val routeNames = routes.associate { it.id to routeName(it) }

        val allFlights =
            FlightData.queryDatabase().map { result ->
                val flight = result.dataClass
                val plane = PlaneData.queryDatabase(flight.planeId).firstOrNull()?.dataClass

                FlightView(
                    id = flight.id,
                    routeId = flight.routeId,
                    routeName = routeNames[flight.routeId],
                    planeId = flight.planeId,
                    planeRegistrationCode = plane?.registrationCode,
                    date = flight.date,
                    time = flight.time,
                )
            }

        val filteredFlights =
            if (search.isBlank()) {
                allFlights
            } else {
                allFlights.filter { flight ->
                    flight.routeName?.contains(search, ignoreCase = true) == true ||
                        flight.planeRegistrationCode?.contains(search, ignoreCase = true) == true ||
                        flight.date.toString().contains(search, ignoreCase = true) ||
                        flight.time.toString().contains(search, ignoreCase = true)
                }
            }

        val totalPages = ((filteredFlights.size + pageSize - 1) / pageSize).coerceAtLeast(1)
        val safePage = page.coerceAtMost(totalPages)

        val flights =
            filteredFlights
                .drop((safePage - 1) * pageSize)
                .take(pageSize)

        val model =
            mapOf(
                "title" to "Manage Flights",
                "layout" to "admin",
                "activePage" to "manageFlights",
                "inNav" to true,
                "isAdmin" to true,
                "flights" to flights,
                "routes" to routes,
                "routeNames" to routeNames,
                "planes" to planes,
                "destinations" to destinations,
                "search" to search,
                "page" to safePage,
                "totalPages" to totalPages,
            )

        val template = pebble.getTemplate("admin/manageFlights.peb")
        val writer = StringWriter()
        fullEvaluate(template, writer, model)
        respondText(writer.toString(), ContentType.Text.Html)
    }
}

/**
 * Creates a new scheduled flight ensuring no exact duplicates exist in the database.
 */
private suspend fun ApplicationCall.handleCreateFlightPost() {
    timed("T1_create_flight", jsMode()) {
        if (!requireAdmin()) return@timed

        val params = receiveParameters()

        val routeId = params["routeId"]?.toIntOrNull()
        val planeId = params["planeId"]?.toIntOrNull()
        val date = params["date"]?.let { LocalDate.parse(it) }
        val time = params["time"]?.let { LocalTime.parse(it) }

        if (routeId == null || planeId == null || date == null || time == null) {
            respondText("Please fill in all flight fields", status = HttpStatusCode.BadRequest)
            return@timed
        }

        val existing =
            FlightData.queryDatabase(
                whereArgs =
                    WhereArgs(
                        "${FlightColumns.ROUTE_ID.name} = ? AND ${FlightColumns.PLANE_ID.name} = ? AND ${FlightColumns.DATE.name} = ? AND ${FlightColumns.TIME.name} = ?",
                        listOf(routeId, planeId, date.toString(), time.toString()),
                    ),
            )

        if (existing.isNotEmpty()) {
            respondText("That flight already exists", status = HttpStatusCode.BadRequest)
            return@timed
        }

        FlightData(
            routeId = routeId,
            planeId = planeId,
            date = date,
            time = time,
        ).insertIntoDatabase()

        response.headers.append("HX-Redirect", "/manageFlights")
        respond(HttpStatusCode.OK)
    }
}

/**
 * Updates properties of an existing scheduled flight via ID lookup.
 */
private suspend fun ApplicationCall.handleUpdateFlightPost() {
    timed("T2_update_flight", jsMode()) {
        if (!requireAdmin()) return@timed

        val params = receiveParameters()

        val flightId = params["flightId"]?.toIntOrNull()
        val routeId = params["routeId"]?.toIntOrNull()
        val planeId = params["planeId"]?.toIntOrNull()
        val date = params["date"]?.let { LocalDate.parse(it) }
        val time = params["time"]?.let { LocalTime.parse(it) }

        if (flightId == null || routeId == null || planeId == null || date == null || time == null) {
            respondText("Invalid flight details", status = HttpStatusCode.BadRequest)
            return@timed
        }

        val duplicate =
            FlightData.queryDatabase(
                whereArgs =
                    WhereArgs(
                        "${FlightColumns.ROUTE_ID.name} = ? AND ${FlightColumns.PLANE_ID.name} = ? AND ${FlightColumns.DATE.name} = ? AND ${FlightColumns.TIME.name} = ? AND ${FlightColumns.ID.name} != ?",
                        listOf(routeId, planeId, date.toString(), time.toString(), flightId),
                    ),
            )

        if (duplicate.isNotEmpty()) {
            respondText("That flight already exists", status = HttpStatusCode.BadRequest)
            return@timed
        }

        FlightData.updateTable(
            values =
                mapOf(
                    FlightColumns.ROUTE_ID to routeId,
                    FlightColumns.PLANE_ID to planeId,
                    FlightColumns.DATE to date.toString(),
                    FlightColumns.TIME to time.toString(),
                ),
            whereArgs =
                WhereArgs(
                    "${FlightColumns.ID.name} = ?",
                    listOf(flightId),
                ),
        )

        response.headers.append("HX-Redirect", "/manageFlights")
        respond(HttpStatusCode.OK)
    }
}

/**
 * Completes a flight deletion, safely removing associated seats, updating refunds via Stripe API, and notifying customers.
 */
private suspend fun ApplicationCall.handleDeleteFlightPost() {
    timed("T3_delete_flight", jsMode()) {
        if (!requireAdmin()) return@timed

        val flightId = receiveParameters()["flightId"]?.toIntOrNull()

        if (flightId == null) {
            respondText("Invalid flight", status = HttpStatusCode.BadRequest)
            return@timed
        }

        val flight = FlightData.queryDatabase(flightId).firstOrNull()?.dataClass

        if (flight == null) {
            respondText("Flight not found", status = HttpStatusCode.BadRequest)
            return@timed
        }

        val seats =
            SeatData
                .queryDatabase(
                    whereArgs =
                        WhereArgs(
                            "${SeatColumns.FLIGHT_ID.name} = ?",
                            listOf(flightId),
                        ),
                ).map { it.dataClass }

        val bookedSeats =
            seats.flatMap { seat ->
                BookedSeatData
                    .queryDatabase(
                        whereArgs =
                            WhereArgs(
                                "${BookedSeatColumns.SEAT_ID.name} = ?",
                                listOf(seat.id),
                            ),
                    ).map { it.dataClass }
            }

        val bookingRows =
            bookedSeats.mapNotNull { bookedSeat ->
                DatabaseManager
                    .queryTable(
                        table = BookingData.EMPTY.tableName,
                        columns = BookingColumns.COLUMN_NAMES,
                        whereArgs =
                            WhereArgs(
                                "${BookingColumns.ID.name} = ?",
                                listOf(bookedSeat.bookingId),
                            ),
                    ).firstOrNull()
            }

        // Group rows to aggregate refundable portions correctly
        val refundableGroups =
            bookingRows
                .filter { row ->
                    val paymentIntentId = row[5]?.toString()
                    val amountPaid = (row[6] as? Number)?.toLong()
                    val refundStatus = row[7]?.toString()

                    !paymentIntentId.isNullOrBlank() &&
                        amountPaid != null &&
                        amountPaid > 0L &&
                        refundStatus == null
                }.groupBy { row ->
                    "${row[5]}|${row[4]}"
                }

        for ((_, groupRows) in refundableGroups) {
            val firstRow = groupRows.first()

            val bookerId = firstRow[1] as Int
            val bookingReference = firstRow[4].toString()
            val paymentIntentId = firstRow[5]?.toString()
            val refundAmount =
                groupRows.sumOf { row ->
                    (row[6] as? Number)?.toLong() ?: 0L
                }

            if (paymentIntentId.isNullOrBlank() || refundAmount <= 0L) {
                continue
            }

            // Issue Stripe Refund Request
            val refundParams =
                RefundCreateParams
                    .builder()
                    .setPaymentIntent(paymentIntentId)
                    .setAmount(refundAmount)
                    .setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
                    .putMetadata("booking_reference", bookingReference)
                    .putMetadata("admin_deleted_flight_id", flightId.toString())
                    .build()

            val refund = Refund.create(refundParams)

            for (row in groupRows) {
                val bookingId = row[0] as Int

                BookingData.updateTable(
                    values =
                        mapOf(
                            BookingColumns.REFUND_STATUS to "REFUNDED_FULL",
                            BookingColumns.STRIPE_REFUND_ID to refund.id,
                            BookingColumns.REFUND_AMOUNT to refundAmount.toInt(),
                        ),
                    whereArgs =
                        WhereArgs(
                            "${BookingColumns.ID.name} = ?",
                            listOf(bookingId),
                        ),
                )
            }

            val email = getBookerEmail(bookerId)

            if (!email.isNullOrBlank()) {
                EmailService.sendRefundConfirmation(
                    to = email,
                    reference = bookingReference,
                    refundAmount = refundAmount,
                    refundId = refund.id,
                )
            }
        }

        // Clean up database dependencies before deleting the flight parent table record
        for (bookedSeat in bookedSeats) {
            BookedSeatData.delete(bookedSeat.id)
        }

        for (seat in seats) {
            SeatData.delete(seat.id)
        }

        val staffAssignments = AssignedFlightStaffData.queryByFlightID(flightId)

        for (assignment in staffAssignments) {
            AssignedFlightStaffData.delete(assignment.dataClass.id)
        }

        val flightSearchLinks =
            FlightSearchFlightData.queryDatabase(
                whereArgs =
                    WhereArgs(
                        "${FlightSearchFlightColumns.FLIGHT_ID.name} = ?",
                        listOf(flightId),
                    ),
            )

        for (link in flightSearchLinks) {
            FlightSearchFlightData.delete(link.dataClass.id)
        }

        FlightData.delete(flightId)

        response.headers.append("HX-Redirect", "/manageFlights")
        respond(HttpStatusCode.OK)
    }
}

/**
 * Prepares the edit flight view template by resolving the specific flight details needed for form rendering.
 */
private suspend fun ApplicationCall.handleEditFlightLoad() {
    timed("T4_edit_flight_load", jsMode()) {
        if (!requireAdmin()) return@timed

        val flightId = parameters["id"]?.toIntOrNull()
        if (flightId == null) {
            respondRedirect("/manageFlights")
            return@timed
        }

        val flight = FlightData.queryDatabase(flightId).firstOrNull()?.dataClass
        if (flight == null) {
            respondRedirect("/manageFlights")
            return@timed
        }

        val routes = RouteData.queryDatabase().map { it.dataClass }
        val planes = PlaneData.queryDatabase().map { it.dataClass }

        fun routeName(route: RouteData): String {
            val start =
                DestinationData
                    .queryDatabase(route.startDestination)
                    .firstOrNull()
                    ?.dataClass
                    ?.cityName ?: "Unknown"

            val end =
                DestinationData
                    .queryDatabase(route.endDestination)
                    .firstOrNull()
                    ?.dataClass
                    ?.cityName ?: "Unknown"

            return "$start to $end"
        }

        val routeNames = routes.associate { it.id to routeName(it) }

        val model =
            mapOf(
                "title" to "Edit Flight",
                "layout" to "admin",
                "activePage" to "manageFlights",
                "inNav" to true,
                "isAdmin" to true,
                "flight" to flight,
                "routes" to routes,
                "routeNames" to routeNames,
                "planes" to planes,
            )

        val template = getEngine().getTemplate("admin/editFlight.peb")
        val writer = StringWriter()
        fullEvaluate(template, writer, model)
        respondText(writer.toString(), ContentType.Text.Html)
    }
}

/**
 * Loads the deep-dive administration view for an individual flight, including an attached list of current reservations.
 */
private suspend fun ApplicationCall.handleFlightDetailsLoad() {
    timed("T5_flight_details_load", jsMode()) {
        if (!requireAdmin()) return@timed

        val flightId = parameters["id"]?.toIntOrNull()
        if (flightId == null) {
            respondRedirect("/manageFlights")
            return@timed
        }

        val flight = FlightData.queryDatabase(flightId).firstOrNull()?.dataClass
        if (flight == null) {
            respondRedirect("/manageFlights")
            return@timed
        }

        val route = RouteData.queryDatabase(flight.routeId).firstOrNull()?.dataClass
        val plane = PlaneData.queryDatabase(flight.planeId).firstOrNull()?.dataClass

        val routeName =
            if (route == null) {
                "Unknown"
            } else {
                val start = DestinationData.getDestinationName(route.startDestination)
                val end = DestinationData.getDestinationName(route.endDestination)
                "$start to $end"
            }

        val search = request.queryParameters["search"]?.trim().orEmpty()
        val page = request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val pageSize = 5

        val seats =
            SeatData
                .queryDatabase(
                    whereArgs =
                        WhereArgs(
                            "${SeatColumns.FLIGHT_ID.name} = ?",
                            listOf(flightId),
                        ),
                ).map { it.dataClass }

        val reservations =
            seats.flatMap { seat ->
                BookedSeatData
                    .queryDatabase(
                        whereArgs =
                            WhereArgs(
                                "${BookedSeatColumns.SEAT_ID.name} = ?",
                                listOf(seat.id),
                            ),
                    ).mapNotNull { bookedSeatResult ->
                        val bookedSeat = bookedSeatResult.dataClass

                        val bookingRow =
                            DatabaseManager
                                .queryTable(
                                    table = BookingData.EMPTY.tableName,
                                    columns = BookingColumns.COLUMN_NAMES,
                                    whereArgs =
                                        WhereArgs(
                                            "${BookingColumns.ID.name} = ?",
                                            listOf(bookedSeat.bookingId),
                                        ),
                                ).firstOrNull() ?: return@mapNotNull null

                        val bookingId = bookingRow[0] as Int
                        val bookerId = bookingRow[1] as Int
                        val bookingReference = bookingRow[4]?.toString() ?: "Unknown"
                        val amountPaid = (bookingRow[6] as? Number)?.toLong()
                        val amountPaidDisplay =
                            if (amountPaid == null) "£0.00" else "£%.2f".format(amountPaid / 100.0)
                        val refundStatus = bookingRow[7]?.toString()

                        FlightReservationView(
                            bookingId = bookingId,
                            bookingReference = bookingReference,
                            bookerEmail = getBookerEmail(bookerId),
                            seatId = seat.id,
                            amountPaid = amountPaid,
                            amountPaidDisplay = amountPaidDisplay,
                            refundStatus = refundStatus,
                        )
                    }
            }

        val filteredReservations =
            if (search.isBlank()) {
                reservations
            } else {
                reservations.filter { reservation ->
                    reservation.bookingReference.contains(search, ignoreCase = true) ||
                        reservation.bookerEmail?.contains(search, ignoreCase = true) == true ||
                        reservation.seatId.toString().contains(search)
                }
            }

        val totalPages = ((filteredReservations.size + pageSize - 1) / pageSize).coerceAtLeast(1)
        val safePage = page.coerceAtMost(totalPages)

        val pagedReservations =
            filteredReservations
                .drop((safePage - 1) * pageSize)
                .take(pageSize)

        val model =
            mapOf(
                "title" to "Flight Details",
                "layout" to "admin",
                "activePage" to "manageFlights",
                "inNav" to true,
                "isAdmin" to true,
                "flight" to flight,
                "routeName" to routeName,
                "planeRegistrationCode" to plane?.registrationCode,
                "reservations" to pagedReservations,
                "search" to search,
                "page" to safePage,
                "totalPages" to totalPages,
            )

        val template = getEngine().getTemplate("admin/flightDetails.peb")
        val writer = StringWriter()
        fullEvaluate(template, writer, model)
        respondText(writer.toString(), ContentType.Text.Html)
    }
}

/**
 * Extracts the associated email string for a given booker profile ID across User and Guest references.
 */
private fun getBookerEmail(bookerId: Int): String? {
    val booker =
        BookerData
            .queryDatabase(
                whereArgs = WhereArgs("${BookerColumns.ID.name} = ?", listOf(bookerId)),
            ).firstOrNull()
            ?.dataClass ?: return null

    booker.userId?.let { userId ->
        val user =
            UserData
                .queryDatabase(
                    whereArgs = WhereArgs("${UserColumns.ID.name} = ?", listOf(userId)),
                ).firstOrNull()
                ?.dataClass ?: return null

        val login =
            LoginData
                .queryDatabase(
                    whereArgs = WhereArgs("${LoginColumns.ID.name} = ?", listOf(user.loginId)),
                ).firstOrNull()
                ?.dataClass

        return login?.email
    }

    booker.guestId?.let { guestId ->
        val guest =
            GuestData
                .queryDatabase(
                    whereArgs = WhereArgs("${GuestColumns.ID.name} = ?", listOf(guestId)),
                ).firstOrNull()
                ?.dataClass

        return guest?.email
    }

    return null
}
