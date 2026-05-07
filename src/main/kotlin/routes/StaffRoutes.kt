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
import org.mindrot.jbcrypt.BCrypt
import io.ktor.server.sessions.*

fun Route.staffRoutes() {
    get("/stafflogin") { call.handleStaffLoginLoad() }
    post("/stafflogin") { call.handleStaffLoginPost() }
    get("/staffdashboard") { call.handleStaffDashboardLoad() }
    get("/staff-assignment/{token}") { call.handleGetAssignment() }
}

fun ApplicationCall.createStaffLoginStatus(message: String): String =
    """<div id="staff-log-in-status" hx-swap-oob="true" role="status" aria-live="polite" aria-atomic="true">$message</div>"""

private suspend fun ApplicationCall.handleStaffLoginLoad() {
    timed("T0_staff_login", jsMode()) {

        val pebble = getEngine()

        val model = mapOf(
            "title" to "Staff Login",
            "activePage" to "staff",
            "inNav" to true
        )

        val template = pebble.getTemplate("staff/stafflogin.peb")
        val writer = StringWriter()
        fullEvaluate(template, writer, model)
        respondText(writer.toString(), ContentType.Text.Html)
    }
}

private suspend fun ApplicationCall.handleStaffLoginPost() {
    timed("T1_staff_login_post", jsMode()) {
        val params = receiveParameters()

        val email = params["staffEmail"]
        val password = params["staffPw"]

        if (email.isNullOrBlank() || password.isNullOrBlank()) {
            respondText(
                createStaffLoginStatus("Incorrect email or password"),
                ContentType.Text.Html,
                status = HttpStatusCode.OK
            )
            return@timed
        }

        val query = StaffData.queryByLogIn(email)

        if (query.isEmpty()) {
            respondText(
                createStaffLoginStatus("Incorrect email or password"),
                ContentType.Text.Html,
                status = HttpStatusCode.OK
            )
            return@timed
        }

        val result = query.first()

        val passwordColumn = result.getColumn(
            LoginData.EMPTY.tableName,
            LoginColumns.PASSWORD_HASH.name
        )

        if (passwordColumn == null) {
            respondText(
                createStaffLoginStatus("An error occurred, please try again later"),
                ContentType.Text.Html,
                status = HttpStatusCode.OK
            )
            return@timed
        }

        val storedPassword = passwordColumn.columnVal as String

        if (!BCrypt.checkpw(password, storedPassword)) {
            respondText(
                createStaffLoginStatus("Incorrect email or password"),
                ContentType.Text.Html,
                status = HttpStatusCode.OK
            )
            return@timed
        }

        sessions.set(StaffSessionData.createSession(result.dataClass.id).toTokenSession())
        response.headers.append("HX-Redirect", "/staffdashboard")
        respond(HttpStatusCode.OK)
    }
}

private suspend fun ApplicationCall.handleStaffDashboardLoad() {
    timed("T2_staff_dashboard", jsMode()) {
        requireStaff() ?: return@timed
        val logged_state : StaffLoggedInState = staffLoggedIn()
        if (!logged_state.logged_in || logged_state.session == null) {
            respondRedirect("/login")
            return@timed
        }

        val staffAssignments = AssignedFlightStaffData.getFlightsAssignedToStaff(logged_state.staffId).mapNotNull { flight ->
            val route = RouteData.queryDatabase(flight.dataClass.routeId).firstOrNull()?.dataClass ?: return@mapNotNull null

            StaffFlightInfo(
                DestinationData.getDestinationName(route.startDestination),
                DestinationData.getDestinationName(route.endDestination),
                "${flight.dataClass.date} ${flight.dataClass.time}",
                route.duration.toString(),
                flight.getColumn(AssignedFlightStaffData.EMPTY.tableName, AssignedFlightStaffColumns.SEARCH_TOKEN.name)?.columnVal as String
            )
        }

        val pebble = getEngine()
        val model = mapOf(
            "title" to "Staff Dashboard",
            "layout" to "staff",
            "activePage" to "staffDashboard",
            "inNav" to true,
            "staffFlights" to staffAssignments
        )

        val template = pebble.getTemplate("staff/index.peb")
        val writer = StringWriter()
        fullEvaluate(template, writer, model)
        respondText(writer.toString(), ContentType.Text.Html)
    }
}

private suspend fun ApplicationCall.handleGetAssignment() {
    timed("T3_staff_assignment", jsMode()) {
        val token = parameters["token"]        
        if (token == null) return@timed pageNotFoundResponse()

        val assignment : AssignedFlightStaffData? = AssignedFlightStaffData.queryDatabase(token).firstOrNull()?.dataClass
        if (assignment == null) return@timed pageNotFoundResponse()

        val flight : FlightData? = FlightData.queryDatabase(assignment.flightId).firstOrNull()?.dataClass
        if (flight == null) return@timed pageNotFoundResponse()

        val route : RouteData? = RouteData.queryDatabase(flight.routeId).firstOrNull()?.dataClass
        if (route == null) return@timed pageNotFoundResponse()

        val model = mapOf(
            "title" to "Result",
            "inNav" to true,
            "start" to DestinationData.getDestinationName(route.startDestination),
            "end" to DestinationData.getDestinationName(route.endDestination),
            "date" to "${flight.date} ${flight.time}",
            "duration" to route.duration.toString()
        )
        
        val writer = StringWriter()
        val pebble = getEngine()
        val template = pebble.getTemplate("staff/staffAssignment.peb")
        fullEvaluate(template, writer, model)
        respondText(writer.toString(), ContentType.Text.Html)
    }
}

private suspend fun ApplicationCall.pageNotFoundResponse() {
    val writer = StringWriter()
    val pebble = getEngine()
    val template = pebble.getTemplate("staff/pageNotFound.peb")

    val errorModel = mapOf(
        "title" to "Error",
        "inNav" to true
    )

    fullEvaluate(template, writer, errorModel)
    return respondText(writer.toString(), ContentType.Text.Html)
}

private suspend fun ApplicationCall.requireStaff(): StaffSessionToken? {
    val staffSession = sessions.get<StaffSessionToken>()

    if (staffSession == null) {
        respondRedirect("/stafflogin")
        return null
    }

    val results : List<QueryResult<StaffSessionData>> = StaffSessionData.queryDatabase(staffSession.token)
    
    if (results.isEmpty()) {
        sessions.clear<StaffSessionToken>()
        respondRedirect("/stafflogin")
        return null
    }

    return staffSession
}