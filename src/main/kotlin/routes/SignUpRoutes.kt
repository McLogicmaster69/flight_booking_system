package routes

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
import data.*
import auth.UserSession
import auth.LoggedInState

fun Route.signUpRoutes() {
    post("/signup") { call.handleSignUpPost() }
}

fun ApplicationCall.createSignUpStatus(message : String) : String = """<div id="sign-up-status" hx-swap-oob="true" role="status" aria-live="polite" aria-atomic="true">$message</div>"""

private suspend fun ApplicationCall.handleSignUpPost() {
    timed("T1_sign_up_post", jsMode()) {
        val pebble = getEngine()
        val params = receiveParameters()

        val firstname = params["signupFirstName"]
        val lastname = params["signupLastName"]
        val email = params["signupEmail"]
        val password = params["signupPassword"]

        //
        // TODO Hash password
        //

        val passwordHash = password

        if (firstname.isNullOrBlank()) {
            respondText(createSignUpStatus("Please fill in a first name"), ContentType.Text.Html, status = HttpStatusCode.OK)
            return@timed
        }

        if (lastname.isNullOrBlank()) {
            respondText(createSignUpStatus("Please fill in a last name"), ContentType.Text.Html, status = HttpStatusCode.OK)
            return@timed
        }

        if (email.isNullOrBlank()) {
            respondText(createSignUpStatus("Please fill in an email"), ContentType.Text.Html, status = HttpStatusCode.OK)
            return@timed
        }

        if (password.isNullOrBlank()) {
            respondText(createSignUpStatus("Please fill in a password"), ContentType.Text.Html, status = HttpStatusCode.OK)
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

        if (query.size > 0) {
            respondText(createSignUpStatus("User already exists with that email"), ContentType.Text.Html, status = HttpStatusCode.OK)
            return@timed
        }
        
        val login_id : Int = LoginData(email = email, password_hash = passwordHash).insertIntoDatabase()
        val userData : UserData = UserData(firstName = firstname, lastName = lastname, verifiedAccount = false, loginId = login_id)
        val user_id : Int = userData.insertIntoDatabase()

        sessions.set(UserSession(
            user_id,
            userData.firstName,
            userData.lastName,
            userData.verifiedAccount,
            userData.loginId
        ))

        response.headers.append("HX-Redirect", "/")
        respond(HttpStatusCode.OK)
    }
}
