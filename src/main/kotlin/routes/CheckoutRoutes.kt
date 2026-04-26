package routes

import data.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.pebbletemplates.pebble.PebbleEngine
import java.io.StringWriter
import utils.jsMode
import utils.logValidationError
import utils.timed
import auth.*
import org.mindrot.jbcrypt.BCrypt
import utils.EmailService
import java.sql.Timestamp
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.time.LocalDate
import java.time.LocalTime
import com.stripe.Stripe
import com.stripe.model.PaymentIntent
import com.stripe.param.PaymentIntentCreateParams

fun Route.checkoutRoutes() {
    post("/checkout") { call.handleCheckoutPost() }
    post("/payment") { call.handlePaymentPost()}
    post("/create-payment-intent") { call.handleCreatePaymentIntent() }
    post("/confirm-booking") { call.handleConfirmBooking() }
    get("/payment-success") { call.handlePaymentSuccessLoad() }
}

private suspend fun ApplicationCall.handleCheckoutPost() {
    timed("T0_checkout", jsMode()) { 
        val pebble = getEngine()

        val cheakoutparams = receiveParameters()
        val tickets = (cheakoutparams["tickets"]?.toIntOrNull() ?: 1).coerceAtLeast(1)
        val start = cheakoutparams["start"]
        val end = cheakoutparams["end"]
        val dateTimeStr = cheakoutparams["dateTime"]
        val ticketTypes = TicketTypeData.queryDatabase().map { it.dataClass }

        val (datePart, timePart) = dateTimeStr?.split("T")?.let {
            if (it.size == 2) it[0] to it[1] else it[0] to "00:00"
        } ?: ("1970-01-01" to "00:00")

        val startPair = DestinationData.parseDestination(start)
        val endPair = DestinationData.parseDestination(end)

        val startId = startPair?.let { (city, country) ->
            DestinationData.queryDatabase(city, country).firstOrNull()?.dataClass?.id ?: -1
        } ?: -1

        val endId = endPair?.let { (city, country) ->
            DestinationData.queryDatabase(city, country).firstOrNull()?.dataClass?.id ?: -1
        } ?: -1

        val routeId = RouteData.getRouteId(DestinationArgs(startId, endId))
        val flightId = FlightData.queryDatabase(listOf(routeId), LocalDate.parse(datePart)).firstOrNull()?.dataClass?.id

        if (flightId != null) {
            SeatData.generateSeatsForFlight(flightId)
        }

        val classes = ClassData.queryDatabase().map { it.dataClass }

        val availableSeats = if (flightId != null) {
            SeatData.queryDatabase(
                whereArgs = WhereArgs("${SeatColumns.FLIGHT_ID.name} = ?", listOf(flightId))
            ).map { it.dataClass }.filter { seat ->
                BookedSeatData.queryDatabase(
                    whereArgs = WhereArgs("${BookedSeatColumns.SEAT_ID.name} = ?", listOf(seat.id))
                ).isEmpty()
            }
        } else {
            emptyList()
        }

        val model = mapOf(
            "title" to "Checkout",
            "isNav" to true,
            "tickets" to tickets,
            "start" to start,
            "end" to end,
            "date" to dateTimeStr,
            "flightId" to flightId,
            "classes" to classes,
            "availableSeats" to availableSeats,
            "ticketTypes" to ticketTypes
        )

        val template = pebble.getTemplate("checkout/index.peb")
        val writer = StringWriter()
        fullEvaluate(template, writer, model)
        respondText(writer.toString(), ContentType.Text.Html)
    }
}

private suspend fun ApplicationCall.handlePaymentPost() {
    timed("T1_payment", jsMode()) {
        val pebble = getEngine()
        val paymentparams = receiveParameters()

        val typeIds = mutableListOf<Int>()

        val flightId = paymentparams["flightId"]
        if (flightId.isNullOrBlank()) {
            respondText("Missing flight ID", status = HttpStatusCode.BadRequest)
            return@timed
        }

        val tickets = (paymentparams["tickets"]?.toIntOrNull() ?: 1).coerceAtLeast(1)

        for (i in 1..tickets) {
            val lastName = paymentparams["lastName$i"]
            val passport = paymentparams["passport$i"]

            if (lastName.isNullOrBlank() || passport.isNullOrBlank()) {
                respondText("Missing passenger info", status = HttpStatusCode.BadRequest)
                return@timed
            }
        }

        val classIds = mutableListOf<Int>()
        val seatIds = mutableListOf<Int?>()
        var totalAmount = 0L

        val flight = FlightData.queryDatabase(
            whereArgs = WhereArgs("id = ?", listOf(flightId.toInt()))
        ).firstOrNull()?.dataClass

        if (flight == null) {
            respondText("Flight not found", status = HttpStatusCode.BadRequest)
            return@timed
        }

        val durationMinutes = RouteData.getDurationMinutes(flight.routeId)

        for (i in 1..tickets) {
            val classId = paymentparams["classId$i"]?.toIntOrNull()
            val seatId = paymentparams["seatId$i"]?.toIntOrNull()
            val typeId = paymentparams["typeId$i"]?.toIntOrNull()

            if (classId == null) {
                respondText("Missing class selection", status = HttpStatusCode.BadRequest)
                return@timed
            }

            if (seatId != null && !SeatData.isSeatAvailable(seatId)) {
                respondText("Selected seat is no longer available", status = HttpStatusCode.BadRequest)
                return@timed
            }

            if (typeId == null) {
                respondText("Missing ticket type", status = HttpStatusCode.BadRequest)
                return@timed
            }

            val classData = ClassData.queryDatabase(
                whereArgs = WhereArgs("id = ?", listOf(classId))
            ).firstOrNull()?.dataClass

            val className = classData?.name ?: "Economy"

            classIds.add(classId)
            seatIds.add(seatId)
            typeIds.add(typeId)

            totalAmount += calculateTicketPrice(
                className = className,
                durationMinutes = durationMinutes,
                choseSeat = seatId != null
            )
        }

        val lastNames = mutableListOf<String>()
        val passports = mutableListOf<String>()

        for (i in 1..tickets) {
            lastNames.add(paymentparams["lastName$i"] ?: "")
            passports.add(paymentparams["passport$i"] ?: "")
        }

        val loggedState = loggedIn()
        var user: UserData? = null
        var userEmail: String? = null

        if (loggedState.logged_in && loggedState.session != null) {
            val token = loggedState.session.token
            user = UserData.queryByToken(token).firstOrNull()?.dataClass

            if (user != null) {
                val login = LoginData.queryDatabase(
                    whereArgs = WhereArgs("id = ?", listOf(user.loginId))
                ).firstOrNull()?.dataClass

                userEmail = login?.email
            }
        }

        val model = mapOf(
            "title" to "Payment",
            "isNav" to true,
            "flightId" to flightId,
            "tickets" to tickets,
            "lastNames" to lastNames,
            "passportNumbers" to passports,
            "logged_in" to loggedState.logged_in,
            "user" to user,
            "userEmail" to userEmail,
            "classIds" to classIds,
            "seatIds" to seatIds,
            "totalAmount" to totalAmount,
            "displayTotal" to "%.2f".format(totalAmount / 100.0),
            "typeIds" to typeIds
        )

        val template = pebble.getTemplate("checkout/payment.peb")
        val writer = StringWriter()
        fullEvaluate(template, writer, model)
        respondText(writer.toString(), ContentType.Text.Html)
    }
}

private suspend fun ApplicationCall.handleCreatePaymentIntent() {
    timed("T2_payment_intent", jsMode()) {
        val body = receive<Map<String, String>>()
        val email = body["email"] ?: ""
        Stripe.apiKey = "sk_test_51TCfFDDPNfjFe9Utry1rCfJEQJ0YIASnPd7O0SkI3Ewo7COIifnBpfEsP7xPhx5c1WJ8ndpJKxi1IrWqJEJfoyEL00engmejFe"

        val amount = body["amount"]?.toLongOrNull()

        if (amount == null || amount <= 0) {
            respondText("Invalid payment amount", status = HttpStatusCode.BadRequest)
            return@timed
        }

        val intentparams = PaymentIntentCreateParams.builder()
            .setAmount(amount)
            .setCurrency("gbp")
            .setReceiptEmail(email)
            .build()

        val intent = PaymentIntent.create(intentparams)


        respond(mapOf("clientSecret" to intent.clientSecret))
    }
}

private suspend fun ApplicationCall.handleConfirmBooking() {
    timed("T3_confirm_booking", jsMode()) {
        val confirmparams = receiveParameters()

        val flightId = confirmparams["flightId"]?.toIntOrNull()
        val email = confirmparams["email"]
        val tickets = (confirmparams["tickets"]?.toIntOrNull() ?: 1).coerceAtLeast(1)

        if (flightId == null || email.isNullOrBlank()) {
            respondText("Invalid booking data", status = HttpStatusCode.BadRequest)
            return@timed
        }

        val loggedState = loggedIn()

        val booker = if (loggedState.logged_in && loggedState.session != null) {
            val token = loggedState.session.token
            val user = UserData.queryByToken(token).firstOrNull()?.dataClass

            if (user == null) {
                respondText("Could not identify logged in user", status = HttpStatusCode.BadRequest)
                return@timed
            }

            BookerData.queryDatabase(
                whereArgs = WhereArgs("${BookerColumns.USER_ID.name} = ?", listOf(user.id))
            ).firstOrNull()?.dataClass ?: run {
                val newBooker = BookerData(userId = user.id, guestId = null)
                val newBookerId = newBooker.insertIntoDatabase()
                BookerData(id = newBookerId, userId = user.id, guestId = null)
            }
        } else {
            val guest = GuestData.queryDatabase(
                whereArgs = WhereArgs("${GuestColumns.EMAIL.name} = ?", listOf(email))
            ).firstOrNull()?.dataClass ?: run {
                val newGuest = GuestData(email = email)
                val newGuestId = newGuest.insertIntoDatabase()
                GuestData(id = newGuestId, email = email)
            }

            BookerData.queryDatabase(
                whereArgs = WhereArgs("${BookerColumns.GUEST_ID.name} = ?", listOf(guest.id))
            ).firstOrNull()?.dataClass ?: run {
                val newBooker = BookerData(userId = null, guestId = guest.id)
                val newBookerId = newBooker.insertIntoDatabase()
                BookerData(id = newBookerId, userId = null, guestId = guest.id)
            }
        }

        val bookingRef = generateBookingReference()
        val passengerNames = mutableListOf<String>()

        for (i in 1..tickets) {
            val lastName = confirmparams["lastName$i"]
            val passport = confirmparams["passport$i"]

            if (lastName.isNullOrBlank() || passport.isNullOrBlank()) {
                respondText("Missing passenger info", status = HttpStatusCode.BadRequest)
                return@timed
            }

            passengerNames.add(lastName)

            val classId = confirmparams["classId$i"]?.toIntOrNull()
            val chosenSeatId = confirmparams["seatId$i"]?.toIntOrNull()
            val typeId = confirmparams["typeId$i"]?.toIntOrNull()

            if (classId == null) {
                respondText("Missing class selection", status = HttpStatusCode.BadRequest)
                return@timed
            }

            if (typeId == null) {
                respondText("Missing ticket type", status = HttpStatusCode.BadRequest)
                return@timed
            }

            val finalSeat = if (chosenSeatId != null) {
                if (!SeatData.isSeatAvailable(chosenSeatId)) {
                    respondText("Selected seat is no longer available", status = HttpStatusCode.BadRequest)
                    return@timed
                }

                SeatData.queryDatabase(
                    whereArgs = WhereArgs("id = ?", listOf(chosenSeatId))
                ).firstOrNull()?.dataClass

            } else {
                SeatData.getRandomAvailableSeat(flightId, classId)
            }

            if (finalSeat == null) {
                respondText("No available seats", status = HttpStatusCode.BadRequest)
                return@timed
            }

            if (finalSeat.flightId != flightId || finalSeat.classId != classId) {
                respondText("Selected seat does not match selected class or flight", status = HttpStatusCode.BadRequest)
                return@timed
            }

            val booking = BookingData(
                bookerId = booker.id,
                passportNumber = passport,
                lastname = lastName,
                bookingReference = bookingRef
            )

            val bookingId = booking.insertIntoDatabase()

            BookedSeatData(
                seatId = finalSeat.id,
                bookingId = bookingId
            ).insertIntoDatabase()
        }

        val flight = FlightData.queryDatabase(
            whereArgs = WhereArgs("id = ?", listOf(flightId))
        ).firstOrNull()?.dataClass

        val route = flight?.routeId?.let {
            RouteData.queryDatabase(it).firstOrNull()?.dataClass
        }

        val startLocation = route?.startDestination?.let {
            DestinationData.queryDatabase(
                whereArgs = WhereArgs("id = ?", listOf(it))
            ).firstOrNull()?.dataClass?.cityName
        } ?: "Unknown"

        val endLocation = route?.endDestination?.let {
            DestinationData.queryDatabase(
                whereArgs = WhereArgs("id = ?", listOf(it))
            ).firstOrNull()?.dataClass?.cityName
        } ?: "Unknown"

        val dateTime = "${flight?.date} ${flight?.time}"

        EmailService.sendBookingConfirmation(
            to = email,
            reference = bookingRef,
            startLocation = startLocation,
            destination = endLocation,
            dateTime = dateTime,
            passengers = passengerNames
        )

        respond(
            mapOf(
                "status" to "success",
                "bookingReference" to bookingRef
            )
        )
    }
}

fun generateBookingReference(): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    return (1..6)
        .map { chars.random() }
        .joinToString("")
}

private suspend fun ApplicationCall.handlePaymentSuccessLoad() {
    timed("T4_payment_success", jsMode()) { 
        val pebble = getEngine()

        val model = mapOf(
            "title" to "Payment Success",
            "isNav" to true
        )

        val template = pebble.getTemplate("checkout/payment-success.peb")
        val writer = StringWriter()
        fullEvaluate(template, writer, model)
        respondText(writer.toString(), ContentType.Text.Html)
    }
}

private fun calculateTicketPrice(
    className: String,
    durationMinutes: Long,
    choseSeat: Boolean
): Long {

    val classMultiplier = when (className) {
        "First Class" -> 3.0
        "Business" -> 2.0
        else -> 1.0
    }

    val basePerMinute = 50L // £0.50 per minute
    val seatFee = if (choseSeat) 500L else 0L // £5

    val basePrice = durationMinutes * basePerMinute

    return (basePrice * classMultiplier).toLong() + seatFee
}