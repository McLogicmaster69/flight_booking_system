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
import auth.UserSession
import auth.LoggedInState
import org.mindrot.jbcrypt.BCrypt

fun Route.logInRoutes() {
    get("/login") { call.handleLogInLoad() }
    post("/login") { call.handleLogInPost() }
    get("/logout") { call.handleLogOut() }
}

fun ApplicationCall.createLoginStatus(message : String) : String = """<div id="log-in-status" hx-swap-oob="true" role="status" aria-live="polite" aria-atomic="true">$message</div>"""

private suspend fun ApplicationCall.handleLogInLoad() {
    timed("T0_log_in", jsMode()) {
        val pebble = getEngine()
        val logged_state : LoggedInState = loggedIn()

        if (logged_state.logged_in)
            respondRedirect("/")

        val model = mapOf(
            "title" to "Log In / Sign Up"
        )

        val template = pebble.getTemplate("auth/login.peb")
        val writer = StringWriter()
        fullEvaluate(template, writer, model)
        respondText(writer.toString(), ContentType.Text.Html)
    }
}

private suspend fun ApplicationCall.handleLogInPost() {
    timed("T1_log_in_post", jsMode()) {
        val pebble = getEngine()
        val params = receiveParameters()
        val email = params["loginEmail"]
        val password = params["loginPw"]

        if (email == null) {
            respondText(createLoginStatus("Incorrect email or password"), ContentType.Text.Html, status = HttpStatusCode.OK)
            return@timed
        }

        val joinArgs : JoinArgs = JoinArgs(
            joinType = "INNER",
            joinTable = LoginData.EMPTY.tableName,
            joinTable1Column = UserColumns.LOGIN_ID.name,
            joinTable2Column = LoginColumns.ID.name,
            joinSelectColumns = LoginColumns.ALL.map { it.name }
        )

        val whereArgs : WhereArgs = WhereArgs(
            whereClause = "${LoginData.EMPTY.tableName}.${LoginColumns.EMAIL.name} = ?",
            listOf(email)
        )

        val query : List<QueryResult<UserData>> = UserData.queryDatabase(
            joinArgs = joinArgs,
            whereArgs = whereArgs
        )

        if (query.size == 0) {
            respondText(createLoginStatus("Incorrect email or password"), ContentType.Text.Html, status = HttpStatusCode.OK)
            return@timed
        }

        val result : QueryResult<UserData> = query[0]

        val column : ColumnValue? = result.getColumn(LoginData.EMPTY.tableName, LoginColumns.PASSWORD_HASH.name)

        if (column == null) {
            respondText(createLoginStatus("An error occured, please try again later"), ContentType.Text.Html, status = HttpStatusCode.OK)
            return@timed
        }

        val stored_password : String = column.columnVal as String

        if (!BCrypt.checkpw(password, stored_password)){
            respondText(createLoginStatus("Incorrect email or password"), ContentType.Text.Html, status = HttpStatusCode.OK)
            return@timed
        }

        sessions.set(UserSession(
            result.dataClass.id,
            result.dataClass.firstName,
            result.dataClass.lastName,
            result.dataClass.verifiedAccount,
            result.dataClass.loginId
        ))
        
        response.headers.append("HX-Redirect", "/")
        respond(HttpStatusCode.OK)
    }
}

private suspend fun ApplicationCall.handleLogOut() {
    timed("T2_log_out", jsMode()) {
        sessions.clear<UserSession>()
        respondRedirect("/")
    }
}
