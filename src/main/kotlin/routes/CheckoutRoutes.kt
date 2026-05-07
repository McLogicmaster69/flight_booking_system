package routes

import data.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import java.io.StringWriter
import java.security.SecureRandom
import utils.jsMode
import utils.timed
import auth.*
import utils.EmailService
import com.stripe.model.PaymentIntent
import com.stripe.param.PaymentIntentCreateParams

fun Route.checkoutRoutes() {
    post("/checkout") { call.handleCheckoutPost() }
    post("/payment") { call.handlePaymentPost() }
    post("/create-payment-intent") { call.handleCreatePaymentIntent() }
    post("/confirm-booking") { call.handleConfirmBooking() }
    get("/payment-success") { call.handlePaymentSuccessLoad() }
}

data class PaymentLegSelection(
    val passenger: Int,
    val leg: Int,
    val classId: Int,
    val seatId: Int?,
    val typeId: Int,
)

data class CheckoutFlightView(
    val info: Any,
    val flightId: Int,
    val leg: Int,
)

private suspend fun ApplicationCall.handleCheckoutPost() {
    timed("T0_checkout", jsMode()) {
        val pebble = getEngine()

        val cheakoutparams = receiveParameters()
        val tickets = (cheakoutparams["tickets"]?.toIntOrNull() ?: 1).coerceAtLeast(1)
        val token = cheakoutparams["token"]
        val search = token?.let { FlightSearchData.queryByToken(it) }

        if (search == null) {
            respondText("Invalid flight selection", status = HttpStatusCode.BadRequest)
            return@timed
        }

        val flightInfo = search.getFlightInfo()
        val ticketTypes = TicketTypeData.queryDatabase().map { it.dataClass }

        val classes = ClassData.queryDatabase().map { it.dataClass }

        val flightIds: List<Int> = search.flights.map { it.flightId }

        for (flightId in flightIds) {
            SeatData.generateSeatsForFlight(flightId)
        }

        val availableSeatsByFlight =
            flightIds.associateWith { flightId ->
                SeatData
                    .queryDatabase(
                        whereArgs = WhereArgs("${SeatColumns.FLIGHT_ID.name} = ?", listOf(flightId)),
                    ).map { it.dataClass }
                    .filter { seat ->
                        BookedSeatData
                            .queryDatabase(
                                whereArgs = WhereArgs("${BookedSeatColumns.SEAT_ID.name} = ?", listOf(seat.id)),
                            ).isEmpty()
                    }
            }

        val checkoutFlights =
            flightInfo.zip(flightIds).mapIndexed { index, pair ->
                CheckoutFlightView(
                    info = pair.first,
                    flightId = pair.second,
                    leg = index,
                )
            }

        val selectedRewards =
            loggedIn()
                .session
                ?.token
                ?.let { SELECTED_REWARDS_BY_USER[it] }
                ?: emptyList()

        val upgradeCount =
            selectedRewards
                .filter { it.key == "upgrade" }
                .sumOf { it.quantity }

        val model =
            mapOf(
                "title" to "Checkout",
                "inNav" to true,
                "tickets" to tickets,
                "classes" to classes,
                "availableSeatsByFlight" to availableSeatsByFlight,
                "ticketTypes" to ticketTypes,
                "flightInfo" to flightInfo,
                "checkoutFlights" to checkoutFlights,
                "selectedRewards" to selectedRewards,
                "upgradeCount" to upgradeCount,
                "token" to token,
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

        val token = paymentparams["token"]
        val search = token?.let { FlightSearchData.queryByToken(it) }

        if (search == null) {
            respondText(
                "<p>Missing or invalid flight selection</p>",
                ContentType.Text.Html,
                status = HttpStatusCode.BadRequest,
            )
            return@timed
        }

        val flightInfo = search.getFlightInfo()
        val flightIds: List<Int> = search.flights.map { it.flightId }
        val legCount = flightInfo.size
        val selections = mutableListOf<PaymentLegSelection>()

        val tickets = (paymentparams["tickets"]?.toIntOrNull() ?: 1).coerceAtLeast(1)

        for (i in 1..tickets) {
            val lastName = paymentparams["lastName$i"]
            val passport = paymentparams["passport$i"]

            if (lastName.isNullOrBlank() || passport.isNullOrBlank()) {
                respondText("<p>Missing passenger info</p>", ContentType.Text.Html, status = HttpStatusCode.BadRequest)
                return@timed
            }
        }

        val selectedRewards =
            loggedIn()
                .session
                ?.token
                ?.let { SELECTED_REWARDS_BY_USER[it] }
                ?: emptyList()

        val upgradeCount =
            selectedRewards
                .filter { it.key == "upgrade" }
                .sumOf { it.quantity }

        val upgradedPassengerSet =
            (1..upgradeCount)
                .mapNotNull { u ->
                    paymentparams["upgradePassenger$u"]?.toIntOrNull()
                }.toSet()

        val classIds = mutableListOf<Int>()
        val seatIds = mutableListOf<Int?>()
        var totalAmount = 0L

        for (i in 1..tickets) {
            for ((legIndex, flightId) in flightIds.withIndex()) {
                val leg = legIndex

                val classId = paymentparams["classId${i}_$leg"]?.toIntOrNull()
                val seatRaw = paymentparams["seatId${i}_$leg"]
                val seatId =
                    if (seatRaw.isNullOrBlank() ||
                        upgradedPassengerSet.contains(i)
                    ) {
                        null
                    } else {
                        seatRaw.toIntOrNull()
                    }
                val typeId = paymentparams["typeId${i}_$leg"]?.toIntOrNull()

                if (classId == null) {
                    respondText(
                        "<p>Missing class selection</p>",
                        ContentType.Text.Html,
                        status = HttpStatusCode.BadRequest,
                    )
                    return@timed
                }

                if (typeId == null) {
                    respondText("<p>Missing ticket type</p>", ContentType.Text.Html, status = HttpStatusCode.BadRequest)
                    return@timed
                }

                if (seatId != null && !SeatData.isSeatAvailable(seatId)) {
                    respondText(
                        "<p>Selected seat is no longer available</p>",
                        ContentType.Text.Html,
                        status = HttpStatusCode.BadRequest,
                    )
                    return@timed
                }

                val flight =
                    FlightData
                        .queryDatabase(
                            whereArgs = WhereArgs("id = ?", listOf(flightId)),
                        ).firstOrNull()
                        ?.dataClass

                if (flight == null) {
                    respondText("<p>Flight not found</p>", ContentType.Text.Html, status = HttpStatusCode.BadRequest)
                    return@timed
                }

                val durationMinutes = RouteData.getDurationMinutes(flight.routeId)

                val classData =
                    ClassData
                        .queryDatabase(
                            whereArgs = WhereArgs("id = ?", listOf(classId)),
                        ).firstOrNull()
                        ?.dataClass

                val className = classData?.name ?: "Economy"

                classIds.add(classId)
                seatIds.add(seatId)
                typeIds.add(typeId)

                totalAmount +=
                    calculateTicketPrice(
                        className = className,
                        durationMinutes = durationMinutes,
                        choseSeat = seatId != null,
                    )

                selections.add(
                    PaymentLegSelection(i, leg, classId, seatId, typeId),
                )
            }
        }

        val hasDiscount = selectedRewards.any { it.key == "discount15" }

        if (hasDiscount) {
            totalAmount = (totalAmount * 85L) / 100L
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
                val login =
                    LoginData
                        .queryDatabase(
                            whereArgs = WhereArgs("id = ?", listOf(user.loginId)),
                        ).firstOrNull()
                        ?.dataClass

                userEmail = login?.email
            }
        }

        val upgradePassengers =
            (1..upgradeCount).map { u ->
                paymentparams["upgradePassenger$u"].orEmpty()
            }

        val model =
            mapOf(
                "title" to "Payment",
                "inNav" to true,
                "flightInfo" to flightInfo,
                "token" to token,
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
                "legCount" to legCount,
                "selections" to selections,
                "typeIds" to typeIds,
                "selectedRewards" to selectedRewards,
                "hasDiscount" to hasDiscount,
                "upgradeCount" to upgradeCount,
                "upgradePassengers" to upgradePassengers,
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

        val amount = body["amount"]?.toLongOrNull()

        if (amount == null || amount <= 0) {
            respondText("Invalid payment amount", status = HttpStatusCode.BadRequest)
            return@timed
        }

        val intentparams =
            PaymentIntentCreateParams
                .builder()
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

        val token = confirmparams["token"]
        val search = token?.let { FlightSearchData.queryByToken(it) }

        if (search == null) {
            respondText("Missing or invalid flight selection", status = HttpStatusCode.BadRequest)
            return@timed
        }

        val flightInfo = search.getFlightInfo()
        val flightIds: List<Int> = search.flights.map { it.flightId }
        val email = confirmparams["email"]
        val tickets = (confirmparams["tickets"]?.toIntOrNull() ?: 1).coerceAtLeast(1)
        val totalAmount = confirmparams["totalAmount"]?.toLongOrNull()
        val paymentIntentId = confirmparams["paymentIntentId"]
        var signedInUser: UserData? = null

        if (flightIds.isEmpty() ||
            email.isNullOrBlank() ||
            totalAmount == null ||
            totalAmount <= 0 ||
            paymentIntentId.isNullOrBlank()
        ) {
            respondText("Invalid booking data", status = HttpStatusCode.BadRequest)
            return@timed
        }

        val loggedState = loggedIn()

        val selectedRewards =
            loggedState.session
                ?.token
                ?.let { SELECTED_REWARDS_BY_USER[it] }
                ?: emptyList()

        val booker =
            if (loggedState.logged_in && loggedState.session != null) {
                val token = loggedState.session.token
                val user = UserData.queryByToken(token).firstOrNull()?.dataClass

                if (user == null) {
                    respondText("Could not identify logged in user", status = HttpStatusCode.BadRequest)
                    return@timed
                }

                signedInUser = user

                BookerData
                    .queryDatabase(
                        whereArgs = WhereArgs("${BookerColumns.USER_ID.name} = ?", listOf(user.id)),
                    ).firstOrNull()
                    ?.dataClass ?: run {
                    val newBooker = BookerData(userId = user.id, guestId = null)
                    val newBookerId = newBooker.insertIntoDatabase()
                    BookerData(id = newBookerId, userId = user.id, guestId = null)
                }
            } else {
                val guest =
                    GuestData
                        .queryDatabase(
                            whereArgs = WhereArgs("${GuestColumns.EMAIL.name} = ?", listOf(email)),
                        ).firstOrNull()
                        ?.dataClass ?: run {
                        val newGuest = GuestData(email = email)
                        val newGuestId = newGuest.insertIntoDatabase()
                        GuestData(id = newGuestId, email = email)
                    }

                BookerData
                    .queryDatabase(
                        whereArgs = WhereArgs("${BookerColumns.GUEST_ID.name} = ?", listOf(guest.id)),
                    ).firstOrNull()
                    ?.dataClass ?: run {
                    val newBooker = BookerData(userId = null, guestId = guest.id)
                    val newBookerId = newBooker.insertIntoDatabase()
                    BookerData(id = newBookerId, userId = null, guestId = guest.id)
                }
            }

        val bookingRef = generateSecureBookingReference()
        val passengerNames = mutableListOf<String>()

        val upgradePassengers =
            selectedRewards
                .filter { it.key == "upgrade" }
                .sumOf { it.quantity }
                .let { count ->
                    (1..count).mapNotNull { u ->
                        confirmparams["upgradePassenger$u"]?.toIntOrNull()
                    }
                }

        val upgradedPassengerSet = upgradePassengers.toSet()
        val bookingCount = tickets * flightIds.size
        val bookingAmount = (totalAmount / bookingCount).toInt()

        for (i in 1..tickets) {
            val lastName = confirmparams["lastName$i"]
            val passport = confirmparams["passport$i"]

            if (lastName.isNullOrBlank() || passport.isNullOrBlank()) {
                respondText("Missing passenger info", status = HttpStatusCode.BadRequest)
                return@timed
            }

            passengerNames.add(lastName)

            for ((legIndex, flightId) in flightIds.withIndex()) {
                val leg = legIndex

                val classId = confirmparams["classId${i}_$leg"]?.toIntOrNull()
                val chosenSeatRaw = confirmparams["seatId${i}_$leg"]
                val chosenSeatId =
                    if (chosenSeatRaw.isNullOrBlank() ||
                        upgradedPassengerSet.contains(i)
                    ) {
                        null
                    } else {
                        chosenSeatRaw.toIntOrNull()
                    }
                val typeId = confirmparams["typeId${i}_$leg"]?.toIntOrNull()

                if (classId == null) {
                    respondText("Missing class selection", status = HttpStatusCode.BadRequest)
                    return@timed
                }

                if (typeId == null) {
                    respondText("Missing ticket type", status = HttpStatusCode.BadRequest)
                    return@timed
                }

                val finalClassId =
                    if (upgradedPassengerSet.contains(i)) {
                        when (classId) {
                            3 -> 2 // Economy to Business
                            2 -> 1 // Business to First Class
                            else -> {
                                respondText("Cannot upgrade First Class", status = HttpStatusCode.BadRequest)
                                return@timed
                            }
                        }
                    } else {
                        classId
                    }

                val finalSeat =
                    if (chosenSeatId != null) {
                        if (!SeatData.isSeatAvailable(chosenSeatId)) {
                            respondText("Selected seat is no longer available", status = HttpStatusCode.BadRequest)
                            return@timed
                        }

                        SeatData
                            .queryDatabase(
                                whereArgs = WhereArgs("id = ?", listOf(chosenSeatId)),
                            ).firstOrNull()
                            ?.dataClass
                    } else {
                        SeatData.getRandomAvailableSeat(flightId, finalClassId)
                    }

                if (finalSeat == null) {
                    respondText("No available seats", status = HttpStatusCode.BadRequest)
                    return@timed
                }

                if (finalSeat.flightId != flightId || finalSeat.classId != finalClassId) {
                    respondText(
                        "Selected seat does not match selected class or flight",
                        status = HttpStatusCode.BadRequest,
                    )
                    return@timed
                }

                val booking =
                    BookingData(
                        bookerId = booker.id,
                        passportNumber = passport,
                        lastname = lastName,
                        bookingReference = bookingRef,
                        paymentIntentId = paymentIntentId,
                        amountPaid = bookingAmount,
                    )

                val bookingId = booking.insertIntoDatabase()

                BookedSeatData(
                    seatId = finalSeat.id,
                    bookingId = bookingId,
                ).insertIntoDatabase()
            }
        }

        signedInUser?.let { user ->
            val rewardCost = selectedRewards.sumOf { it.cost * it.quantity }

            if (rewardCost > user.loyaltyPoints) {
                respondText("You no longer have enough points for these rewards.", status = HttpStatusCode.BadRequest)
                return@timed
            }

            user.usePoints(rewardCost)

            val pointsEarned = (totalAmount / 100L * 10L).toInt()
            user.awardPoints(pointsEarned)
        }

        val rewardEmailLines =
            selectedRewards.flatMap { reward ->
                when (reward.key) {
                    "lounge" -> listOf("Lounge access included at your departure airport(s).")
                    "priority" ->
                        listOf(
                            "Priority boarding included. You also have lounge access in the departure airport(s).",
                        )
                    "bag" -> listOf("Free checked bag included × ${reward.quantity}.")
                    "upgrade" -> {
                        if (upgradePassengers.isEmpty()) {
                            listOf("Free flight upgrade included × ${reward.quantity}.")
                        } else {
                            upgradePassengers.map { passengerNumber ->
                                "Free flight upgrade assigned to Passenger $passengerNumber."
                            }
                        }
                    }
                    "discount15" -> listOf("15% loyalty discount applied to this booking.")
                    else -> listOf("${reward.name} included.")
                }
            }

        EmailService.sendBookingConfirmation(
            to = email,
            reference = bookingRef,
            startLocation = search.getStartDestinationName(),
            destination = search.getEndDestinationName(),
            dateTime = search.getDate(),
            passengers = passengerNames,
            rewards = rewardEmailLines,
        )

        loggedState.session?.token?.let {
            SELECTED_REWARDS_BY_USER.remove(it)
        }

        respond(
            mapOf(
                "status" to "success",
                "bookingReference" to bookingRef,
            ),
        )
    }
}

fun generateSecureBookingReference(): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    val secureRandom = SecureRandom()

    return (1..6)
        .map { chars[secureRandom.nextInt(chars.length)] }
        .joinToString("")
}

private suspend fun ApplicationCall.handlePaymentSuccessLoad() {
    timed("T4_payment_success", jsMode()) {
        val pebble = getEngine()

        val model =
            mapOf(
                "title" to "Payment Success",
                "inNav" to true,
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
    choseSeat: Boolean,
): Long {
    val classMultiplier =
        when (className) {
            "First Class" -> 3.0
            "Business" -> 2.0
            else -> 1.0
        }

    val basePerMinute = 50L // £0.50 per minute
    val seatFee = if (choseSeat) 500L else 0L // £5

    val basePrice = durationMinutes * basePerMinute

    return (basePrice * classMultiplier).toLong() + seatFee
}
