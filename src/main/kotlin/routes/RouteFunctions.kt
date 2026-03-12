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
import utils.jsMode
import utils.logValidationError
import utils.timed
import auth.*
import data.*

fun ApplicationCall.isHtmx(): Boolean = request.headers["HX-Request"]?.equals("true", ignoreCase = true) == true

fun ApplicationCall.getEngine() : PebbleEngine = PebbleEngine.Builder().loader(
    io.pebbletemplates.pebble.loader.ClasspathLoader().apply {
        prefix = "templates/"
    }).build()

fun ApplicationCall.loggedIn() : LoggedInState {
    val token : SessionToken? = sessions.get("TOKEN_SESSION") as SessionToken?
    if (token == null)
        return LoggedInState(false, null)
    
    val query : List<QueryResult<SessionData>> = SessionData.queryDatabase(token.token)
    if (query.isEmpty())
        return LoggedInState(false, null)

    return LoggedInState(true, token)
}

fun ApplicationCall.createUserState(logged_state : LoggedInState) : UserSession? {
    if (!logged_state.logged_in)
        return null

    val query : List<QueryResult<UserData>> = UserData.queryByToken(logged_state.session?.token ?: "")
    if (query.isEmpty())
        return UserSession("", "", 0)

    val user : UserData = query.first().dataClass
    return UserSession(
        user.firstName ?: "",
        user.lastName ?: "",
        user.loyalityPoints ?: 0
    )
}

fun ApplicationCall.loggedMap() : Map<String, Any?> {
    val logged_state = loggedIn()
    return mapOf(
        "logged_in" to logged_state.logged_in,
        "user" to createUserState(logged_state)
    )
}

fun ApplicationCall.fullEvaluate(template : PebbleTemplate, writer : StringWriter, model : Map<String, Any?>) {
    template.evaluate(writer, model + loggedMap())
}
