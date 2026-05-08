package routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.pebbletemplates.pebble.PebbleEngine
import io.pebbletemplates.pebble.template.PebbleTemplate
import java.io.StringWriter
import auth.*
import data.*

/**
 * Checks whether the current request was sent by HTMX.
 */
fun ApplicationCall.isHtmx(): Boolean = request.headers["HX-Request"]?.equals("true", ignoreCase = true) == true

/**
 * Creates and returns a Pebble template engine configured to load templates from the classpath.
 */
fun ApplicationCall.getEngine(): PebbleEngine =
    PebbleEngine
        .Builder()
        .loader(
            io.pebbletemplates.pebble.loader.ClasspathLoader().apply {
                prefix = "templates/"
            },
        ).build()

/**
 * Checks whether a regular user is currently logged in.
 */
fun ApplicationCall.loggedIn(): LoggedInState {
    val token: SessionToken? = sessions.get("TOKEN_SESSION") as SessionToken?

    if (token == null) {
        return LoggedInState(false, null)
    }

    val query: List<QueryResult<SessionData>> = SessionData.queryDatabase(token.token)

    if (query.isEmpty()) {
        return LoggedInState(false, null)
    }

    return LoggedInState(true, token)
}

/**
 * Checks whether a staff user is currently logged in.
 */
fun ApplicationCall.staffLoggedIn(): StaffLoggedInState {
    val token: StaffSessionToken? = sessions.get("STAFF_TOKEN_SESSION") as StaffSessionToken?

    if (token == null) {
        return StaffLoggedInState(false, null, -1)
    }

    val query: List<QueryResult<StaffSessionData>> = StaffSessionData.queryDatabase(token.token)

    if (query.isEmpty()) {
        return StaffLoggedInState(false, null, -1)
    }

    return StaffLoggedInState(true, token, query.first().dataClass.staffId)
}

/**
 * Builds the user session object that is passed into templates.
 */
fun ApplicationCall.createUserState(logged_state: LoggedInState): UserSession? {
    if (!logged_state.logged_in) {
        return null
    }

    val query: List<QueryResult<UserData>> = UserData.queryByToken(logged_state.session?.token ?: "")

    if (query.isEmpty()) {
        return UserSession("", "", 0)
    }

    val user: UserData = query.first().dataClass

    return UserSession(
        user.firstName ?: "",
        user.lastName ?: "",
        user.loyaltyPoints ?: 0,
    )
}

/**
 * Creates a shared login-state map for template rendering.
 */
fun ApplicationCall.loggedMap(): Map<String, Any?> {
    val loggedState = loggedIn()

    return mapOf(
        "logged_in" to loggedState.logged_in,
        "user" to createUserState(loggedState),
    )
}

/**
 * Evaluates a Pebble template with the provided model and shared authentication state.
 */
fun ApplicationCall.fullEvaluate(
    template: PebbleTemplate,
    writer: StringWriter,
    model: Map<String, Any?>,
) {
    val loggedState = loggedIn()

    var isAdmin = false
    var user: UserData? = null

    // Look up the current user to determine whether they have admin access.
    if (loggedState.logged_in && loggedState.session != null) {
        user =
            UserData
                .queryByToken(loggedState.session.token)
                .firstOrNull()
                ?.dataClass

        if (user != null) {
            isAdmin =
                AdminData
                    .queryDatabase(
                        whereArgs =
                            WhereArgs(
                                "${AdminColumns.LOGIN_ID.name} = ?",
                                listOf(user.loginId),
                            ),
                    ).isNotEmpty()
        }
    }

    // Staff status is based on whether a staff session token exists.
    val isStaff = sessions.get<StaffSessionToken>() != null

    template.evaluate(
        writer,
        model + loggedMap() +
            mapOf(
                "isAdmin" to isAdmin,
                "isStaff" to isStaff,
            ),
    )
}
